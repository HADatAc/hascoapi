# Example user script for /run endpoint.
# Expected env vars:
#   INPUT_CSV, OUTPUT_DIR, FUSEKI_SPARQL_ENDPOINT

input_csv <- Sys.getenv("INPUT_CSV", "")
output_dir <- Sys.getenv("OUTPUT_DIR", "")

if (!nzchar(output_dir)) {
  stop("OUTPUT_DIR is not defined")
}

dir.create(output_dir, recursive = TRUE, showWarnings = FALSE)

if (!nzchar(input_csv) || !file.exists(input_csv)) {
  stop("INPUT_CSV not found")
}

data <- read.csv(input_csv, stringsAsFactors = FALSE)

summary_path <- file.path(output_dir, "summary.csv")
write.csv(data.frame(rows = nrow(data), cols = ncol(data)), summary_path, row.names = FALSE)

png(file.path(output_dir, "column-counts.png"), width = 1000, height = 600)
barplot(colSums(!is.na(data)), las = 2, cex.names = 0.7, col = "steelblue", main = "Non-NA count per column")
dev.off()

cat("Generated:", summary_path, "\n")
