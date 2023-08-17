# SIRAPI - Semantic Instrument Repository API

## What is SIR?

It is a web app for managing an underlying instrument knowledge base. SIR is based on concepts defined in the Human-Aware Science Ontology (HAScO). In HAScO, an instrument can be either a physical instrument like a sensor or thermometer, a questionnaire, or a simulation model.

## What is SIRAPI?

It is the back-end part of SIR. Canonical representation of instruments and instrument elements are stored in RDF inside of the Fuseki triple store embedded in SIRAPI. 

## How to run SIRAPI?

1. SIRAPI's host machine requires the installation of `git` and `docker`
2. Using `git clone`, clone SIRAPI from github
3. Edit `/sirapi/app/conf/application.conf' and replace the value of the variable `pac4.jwt.secret` with another random string of around 40 characters. This is the JWT API token that needs to be shared with the SIR application (https://github.com/HADatAc/sir)
4. Using `docker build .`, build SIRAPI images
5. Using `docker-compose up -d`, run SIRAPI
6. record the value of the 

## How to upgrade SIRAPI?

1. log into SIRAPI hosting machine
2. Go to sirapi folder: `cd /sirapi`
3. Bring down sirapi containers: `docker compose down`
4. Delete current images and old containers: `docker compose system prune -a`
5. Update current code: `git pull`
6. Restart sirapi: `docker compose up -d`

## How to erase SIRAPI's triplestore content?

1. log into SIRAPI hosting machine
2. Go to sirapi folder: `cd /sirapi`
3. Bring down sirapi containers: `docker compose down`
4. Delete triplestore volume: `docker volume rm sirapi_sirapi-fuseki-data`
5. Restart sirapi: `docker compose up -d`

## How to create a SIRAPI backup file?

In the example below, we named the backup file with the date of the backup. This can be any date and it may also include the time of the backup. 

1. log into SIRAPI hosting machine
2. Go to sirapi folder: `cd /sirapi`
3. Bring down sirapi containers: `docker compose down`
4. Go to home folder: `cd ~`
5. Generate the backup file: `docker run --rm --volumes-from sirapi_fuseki -v $PWD:/bkp ubuntu bash -c "tar -zcvf /bkp/fuseki-data_17Aug2023.tar.gz"`
6. Use sftp to copy the backup file `/bkp/fuseki-data_17Aug2023.tar.gz` out of the host machine

## How to restore a SIRAPI backup file?

6. Use sftp to copy a backup file, e.g., `/bkp/fuseki-data_17Aug2023.tar.gz` into the SIRAPI host machine
1. log into SIRAPI hosting machine
2. Go to sirapi folder: `cd /sirapi`
3. Bring down sirapi containers: `docker compose down`
4. Go to home folder: `cd ~`
5. Restore the backup file: `docker run --rm --volumes-from sirapi_fuseki -v $PWD:/bkp ubuntu bash -c "tar -zxvf /bkp/fuseki-data_17Aug2023.tar.gz"`


