# HASCOAPI - Human-Aware Science Ontology API

## What is HASCO?

HASCO is an ontology describing how instruments are used to acquire and collect data in support of scientific studies. The ontology has essential concepts for describing studies, study objects like subjects, instruments, instrument elements like detectors and items, instrument deployments enabling the use of instruments to acquire data, and a comprehensive semantic description of data in the context of studies and instrument deployments.

## What is HASCOAPI?

It is an API for handling HASCO-based knowledge graphs. The canonical representation of instruments and instrument elements are stored in RDF inside of the Fuseki triple store embedded in HASCOAPI. 

## How to run HASCOAPI?

1. HASCOAPI's host machine requires the installation of `git` and `docker`
2. Using `git clone`, clone HASCOAPI from github
3. Edit `/hascoapi/conf/application.conf` and replace the variable `pac4.jwt.secret` value with another random string of around 40 characters. This is the JWT API token that needs to be shared with the REP application (https://github.com/HADatAc/rep)
4. Using `docker build .`, build HASCOAPI images
5. Using `docker-compose up -d`, run HASCOAPI

## How to upgrade HASCOAPI?

1. log into HASCOAPI hosting machine
2. Go to hascoapi folder: `cd /hascoapi`
3. Bring down hascoapi containers: `docker-compose down`
4. Delete current images and old containers: `docker-compose system prune -a`
5. Update current code: `git pull`
6. Restart hascoapi: `docker-compose up -d`

## How to erase HASCOAPI's triplestore content?

1. log into HASCOAPI hosting machine
2. Go to hascoapi folder: `cd /hascoapi`
3. Bring down hascoapi containers: `docker-compose down`
4. Delete triplestore volume: `docker volume rm hascoapi_hascoapi-fuseki-data`
5. Restart hascoapi: `docker-compose up -d`

## How to create HASCOAPI backup file?

In the example below, we named the backup file with the backup date. This can be any date and it may also include the time of the backup. 

1. log into HASCOAPI hosting machine
2. Go to hascoapi folder: `cd /hascoapi`
3. Bring down hascoapi containers: `docker-compose down`
4. Go to home folder: `cd ~`
5. Generate the backup file: `docker run --rm --volumes-from hascoapi_fuseki -v $PWD:/bkp ubuntu bash -c "tar -zcvf /bkp/fuseki-data_17Aug2023.tar.gz"`
6. Use sftp to copy the backup file `/bkp/fuseki-data_17Aug2023.tar.gz` out of the host machine

## How to restore a HASCOAPI backup file?

1. Use sftp to copy a backup file, e.g., `/bkp/fuseki-data_17Aug2023.tar.gz` into the HASCOAPI host machine
2. log into HASCOAPI hosting machine
3. Go to hascoapi folder: `cd /hascoapi`
4. Bring down hascoapi containers: `docker-compose down`
5. Go to home folder: `cd ~`
6. Restore the backup file: `docker run --rm --volumes-from hascoapi_fuseki -v $PWD:/bkp ubuntu bash -c "tar -zxvf /bkp/fuseki-data_17Aug2023.tar.gz"`

## R Engine (Fuseki + R Execution)

The file `docker-compose-fuseki-r.yml` starts Fuseki, YASGUI, and an `r-engine` service.

1. Start the stack:

```bash
docker compose -f docker-compose-fuseki-r.yml up -d --build
```

2. Check R engine health:

```bash
curl -s http://localhost:8000/health | jq .
```

3. Submit R code with inline CSV input:

```bash
curl -s -X POST http://localhost:8000/run \
	-H 'Content-Type: application/json' \
	-d '{
		"job_id": "demo-job-001",
		"input_csv_text": "student,score\nA,90\nB,75\nC,88",
		"code": "d <- read.csv(Sys.getenv(\"INPUT_CSV\")); out <- Sys.getenv(\"OUTPUT_DIR\"); write.csv(d, file.path(out, \"scores.csv\"), row.names = FALSE); png(file.path(out, \"scores.png\"), 800, 500); barplot(d$score, names.arg=d$student, col=\"steelblue\", main=\"Scores\"); dev.off(); cat(\"done\\n\")"
	}' | jq .
```

4. Retrieve generated artifact paths from response:

```bash
curl -s -X POST http://localhost:8000/run \
	-H 'Content-Type: application/json' \
	-d '{"code":"cat(\"Hello from R\\n\")"}' \
	| jq -r '.artifacts[]'
```

Notes:
- The response includes `job_id`, `output_dir`, and `artifacts`.
- Artifact files are written under the R engine work volume at `/work/output/<job_id>/` inside the container.
- The execution environment exposes `INPUT_CSV`, `OUTPUT_DIR`, and `FUSEKI_SPARQL_ENDPOINT` to your script.

### HASCOAPI Endpoints For R Script Execution

For hascoapi-based applications, call HASCOAPI directly instead of calling the R engine container.

1. Health check:

```bash
curl -s http://localhost:9000/hascoapi/api/r-analysis/engine/health | jq .
```

2. Execute R code (proxied by HASCOAPI to the R engine):

```bash
curl -s -X POST http://localhost:9000/hascoapi/api/r-analysis/engine/run \
	-H 'Content-Type: application/json' \
	-d '{
		"jobId": "demo-job-002",
		"inputCsvText": "student,score\nA,91\nB,72\nC,84",
		"code": "d <- read.csv(Sys.getenv(\"INPUT_CSV\")); out <- Sys.getenv(\"OUTPUT_DIR\"); write.csv(d, file.path(out, \"scores.csv\"), row.names = FALSE); png(file.path(out, \"scores.png\"), 800, 500); barplot(d$score, names.arg=d$student, col=\"darkgreen\", main=\"Scores\"); dev.off(); cat(\"done\\n\")"
	}' | jq .
```

3. Retrieve execution record:

```bash
curl -s http://localhost:9000/hascoapi/api/r-analysis/engine/jobs/demo-job-002 | jq .
```

4. List generated artifacts and download URLs:

```bash
curl -s http://localhost:9000/hascoapi/api/r-analysis/engine/jobs/demo-job-002/artifacts | jq .
```

5. Download a generated artifact (example):

```bash
curl -L "http://localhost:9000/hascoapi/api/r-analysis/engine/jobs/demo-job-002/artifacts/download/scores.png" -o scores.png
```

Configuration:
- `R_ENGINE_URL` (default: `http://r-engine:8000`)
- `R_ENGINE_TIMEOUT_SECONDS` (default: `300`)
- `R_ENGINE_ARTIFACT_ROOT` (optional, needed for artifact download via HASCOAPI)


