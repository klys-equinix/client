#!/usr/bin/env bash
docker build -t clientdb:1.0.0 .
docker run -it --rm --expose=7001 -p 7001:7001 clientdb:1.0.0
