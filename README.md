# SIRAPI - Semantic Instrument Repository API

## What is SIR?

It is a web app for managing an underlying instrument knowledge base. SIR is based on concepts defined in the Human-Aware Science Ontology (HAScO). In HAScO, an instrument can be either a physical instrument like a sensor or thermometer, a questionnaire, or a simulation model.

## What is SIRAPI?

It is the back-end part of SIR. Canonical representation of instruments and instrument elements are stored in RDF inside of the Fuseki triple store embedded in SIRAPI. 

## How to run SIRAPI?

1. SIRAPI's host machine requires the installation of `git` and `docker`
2. Using `git clone`, clone SIRAPI from github
3. Using `docker build .`, build SIRAPI images
4. Using `docker-compose up -d`, run SIRAPI

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

## How to copy a triplestore from one environment to another?

