f# HASCOAPI - Human-Aware Science Ontology API

## What is HASCO?

HASCO is an ontology describing how instruments are used to acquired and collect data in support of scientic studies. The ontology has essential concepts for describing studies, study objects like subjects, instruments, intrument elements like detectors and items, intrument deployments enabling the use of instruments to acquire data, and a comprehensive semantic description of data in the context of studies and instrument deployments.

## What is HASCOAPI?

It an API for handling HASCO-based knowledge graphs. Canonical representation of instruments and instrument elements are stored in RDF inside of the Fuseki triple store embedded in HASCOAPI. 

## How to run HASCOAPI?

1. HASCOAPI's host machine requires the installation of `git` and `docker`
2. Using `git clone`, clone HASCOAPI from github
3. Edit `/hascoapi/conf/application.conf' and replace the value of the variable `pac4.jwt.secret` with another random string of around 40 characters. This is the JWT API token that needs to be shared with the SIR application (https://github.com/HADatAc/sir)
4. Using `docker build .`, build HASCOAPI images
5. Using `docker-compose up -d`, run HASCOAPI
6. record the value of the 

## How to upgrade HASCOAPI?

1. log into HASCOAPI hosting machine
2. Go to hascoapi folder: `cd /hascoapi`
3. Bring down hascoapi containers: `docker compose down`
4. Delete current images and old containers: `docker compose system prune -a`
5. Update current code: `git pull`
6. Restart haacoapi: `docker compose up -d`

## How to erase HASCOAPI's triplestore content?

1. log into HASCOAPI hosting machine
2. Go to hascoapi folder: `cd /hascoapi`
3. Bring down hascoapi containers: `docker compose down`
4. Delete triplestore volume: `docker volume rm hascoapi_hascoapi-fuseki-data`
5. Restart hascoapi: `docker compose up -d`

## How to create HASCORAPI backup file?

In the example below, we named the backup file with the date of the backup. This can be any date and it may also include the time of the backup. 

1. log into HASCOAPI hosting machine
2. Go to hascoapi folder: `cd /hascoapi`
3. Bring down hascoapi containers: `docker compose down`
4. Go to home folder: `cd ~`
5. Generate the backup file: `docker run --rm --volumes-from hascoapi_fuseki -v $PWD:/bkp ubuntu bash -c "tar -zcvf /bkp/fuseki-data_17Aug2023.tar.gz"`
6. Use sftp to copy the backup file `/bkp/fuseki-data_17Aug2023.tar.gz` out of the host machine

## How to restore a HASCOAPI backup file?

6. Use sftp to copy a backup file, e.g., `/bkp/fuseki-data_17Aug2023.tar.gz` into the HASCOAPI host machine
1. log into HASCOAPI hosting machine
2. Go to hascoapi folder: `cd /hascoapi`
3. Bring down hascoapi containers: `docker compose down`
4. Go to home folder: `cd ~`
5. Restore the backup file: `docker run --rm --volumes-from hascoapi_fuseki -v $PWD:/bkp ubuntu bash -c "tar -zxvf /bkp/fuseki-data_17Aug2023.tar.gz"`


