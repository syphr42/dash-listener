#!/bin/bash

# Enable custom CA certificates
mkdir -p /usr/local/share/ca-certificates/

for cert in "${APPDIR}"/conf/*.crt; do
    cp "${cert}" /usr/local/share/ca-certificates/
done

update-ca-certificates
