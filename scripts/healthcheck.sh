#!/bin/bash

STATUSCODE=$(curl -L --silent --output /dev/null --write-out "%{http_code}" 127.0.0.1:8080/healthcheck -c /dev/null)

if test $STATUSCODE -ne 200; then
    echo "FAILURE"
    exit 1
else
    echo "SUCCESS"
    exit 0
fi
