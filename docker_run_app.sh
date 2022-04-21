#!/bin/bash

docker run -d \
  -p 8080:8080 \
  --name gemp \
  --link gempdb \
  -e db_hostname=gempdb \
  gemp:latest

