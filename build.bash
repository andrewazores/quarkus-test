#!/usr/bin/env bash

./mvnw -DskipTests clean package
podman build -t quay.io/andrewazores/quarkus-test:latest -f src/main/docker/Dockerfile.jvm .
podman image prune -f
