#!/usr/bin/env bash

function cleanup() {
  podman stop quarkus-test
}
trap cleanup EXIT

function loadAgent() {
  sleep 3
  podman exec quarkus-test java -jar /deployments/app/cryostat-agent-shaded.jar
}

podman run --name quarkus-test --rm -d quay.io/andrewazores/quarkus-test:latest
loadAgent &
podman logs -f quarkus-test
