FROM google/cloud-sdk

# TODO allow project to be set by environment variable
ENTRYPOINT gcloud beta emulators datastore start --project=bi-crm-poc --host-port 0.0.0.0:8081 --no-store-on-disk

EXPOSE 8081