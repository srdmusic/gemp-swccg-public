#!/bin/bash


## Build Gemp
mvn clean install

## Build Gemp Container Image
docker build \
  -t gemp:latest \
  -f Dockerfile .

## Build Database Container Image
docker build --force-rm=true --no-cache \
  -t gempdb:latest \
  -f db.Dockerfile .

