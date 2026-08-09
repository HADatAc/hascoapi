# plumber.R

library(plumber)
library(jsonlite)

work_dir <- Sys.getenv("R_ENGINE_WORKDIR", "/work")
output_root <- Sys.getenv("R_ENGINE_OUTPUT_DIR", file.path(work_dir, "output"))

dir.create(file.path(work_dir, "input"), recursive = TRUE, showWarnings = FALSE)
dir.create(output_root, recursive = TRUE, showWarnings = FALSE)
dir.create(file.path(work_dir, "tmp"), recursive = TRUE, showWarnings = FALSE)

#* Health check
#* @get /health
function() {
  list(
    ok = TRUE,
    service = "hascoapi-r-engine",
    work_dir = work_dir,
    output_dir = output_root,
    fuseki_sparql_endpoint = Sys.getenv("FUSEKI_SPARQL_ENDPOINT", "")
  )
}

write_input_csv <- function(job_input_dir, payload) {
  input_csv_path <- file.path(job_input_dir, "input.csv")

  if (!is.null(payload$input_csv_base64) && nzchar(payload$input_csv_base64)) {
    raw <- jsonlite::base64_dec(payload$input_csv_base64)
    writeBin(raw, input_csv_path)
    return(input_csv_path)
  }

  if (!is.null(payload$input_csv_text) && nzchar(payload$input_csv_text)) {
    writeLines(payload$input_csv_text, con = input_csv_path, useBytes = TRUE)
    return(input_csv_path)
  }

  ""
}

collect_artifacts <- function(job_output_dir) {
  files <- list.files(job_output_dir, recursive = TRUE, full.names = TRUE)
  rel <- gsub(paste0("^", gsub("([\\\\.\\[\\]\\(\\)\\{\\}\\+\\*\\?\\^\\$\\|])", "\\\\\\1", job_output_dir), "/?"), "", files)
  rel[rel != ""]
}

#* Execute R code against optional input data and emit artifacts.
#*
#* Body fields:
#* - code (required): R code to execute
#* - input_csv_text (optional): CSV content as plain text
#* - input_csv_base64 (optional): CSV content as base64
#* - job_id (optional): custom job id
#*
#* Environment exposed to user code:
#* - INPUT_CSV: path to input CSV (if provided)
#* - OUTPUT_DIR: directory where artifacts should be written
#* - FUSEKI_SPARQL_ENDPOINT: Fuseki SPARQL endpoint URL
#*
#* @post /run
function(req, res) {
  payload <- tryCatch(fromJSON(req$postBody, simplifyVector = FALSE), error = function(e) NULL)
  if (is.null(payload) || is.null(payload$code) || !nzchar(payload$code)) {
    res$status <- 400
    return(list(ok = FALSE, error = "Missing required field: code"))
  }

  job_id <- if (!is.null(payload$job_id) && nzchar(payload$job_id)) {
    as.character(payload$job_id)
  }
  else {
    paste0(format(Sys.time(), "%Y%m%d-%H%M%S"), "-", as.integer(runif(1, 1000, 9999)))
  }

  job_dir <- file.path(work_dir, "tmp", job_id)
  job_input_dir <- file.path(job_dir, "input")
  job_output_dir <- file.path(output_root, job_id)

  dir.create(job_input_dir, recursive = TRUE, showWarnings = FALSE)
  dir.create(job_output_dir, recursive = TRUE, showWarnings = FALSE)

  input_csv <- write_input_csv(job_input_dir, payload)
  script_path <- file.path(job_dir, "script.R")
  stdout_path <- file.path(job_dir, "stdout.log")
  stderr_path <- file.path(job_dir, "stderr.log")

  writeLines(as.character(payload$code), con = script_path, useBytes = TRUE)

  env <- c(
    INPUT_CSV = input_csv,
    OUTPUT_DIR = job_output_dir,
    FUSEKI_SPARQL_ENDPOINT = Sys.getenv("FUSEKI_SPARQL_ENDPOINT", "")
  )

  exit_status <- system2(
    command = "Rscript",
    args = c("--vanilla", script_path),
    stdout = stdout_path,
    stderr = stderr_path,
    env = env
  )

  stdout_content <- if (file.exists(stdout_path)) paste(readLines(stdout_path, warn = FALSE), collapse = "\n") else ""
  stderr_content <- if (file.exists(stderr_path)) paste(readLines(stderr_path, warn = FALSE), collapse = "\n") else ""

  artifacts <- collect_artifacts(job_output_dir)

  list(
    ok = identical(exit_status, 0L),
    exit_status = exit_status,
    job_id = job_id,
    input_csv = if (nzchar(input_csv)) input_csv else NULL,
    output_dir = job_output_dir,
    artifacts = artifacts,
    stdout = stdout_content,
    stderr = stderr_content
  )
}
