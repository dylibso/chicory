#!/usr/bin/env bash

# Usage:
#   ./build.sh [command]
#
# Commands:
#   builder      Build the Docker image used for building the project
#   bash       Start a bash shell in the build container
#   default    Run the build inside the Docker container (default action)
#   container  (internal) Run the build logic inside the container
#   (no arg)   Same as 'default', runs the build in the container
#
# This script builds the Rust WASM project using a Docker container for a reproducible environment.
#
set -e
cd "$(dirname "$0")"

# Use the specified docker command or default to 'docker'
DOCKER=${DOCKER:-docker}
BUILDER_IMAGE=${BUILDER_IMAGE:-chicory/rust-builder}

build_builder() {
  ${DOCKER} build . --load --tag "$BUILDER_IMAGE"
}

run_docker() {
  # Build the builder image if it does not exist
  if ! ${DOCKER} image inspect "$BUILDER_IMAGE" >/dev/null 2>&1; then
    echo "Docker image $BUILDER_IMAGE not found. Building with: ./build.sh builder"
    build_builder
  fi
  ${DOCKER} run ${DOCKER_ARGS} -t -v"$PWD":/work -w /work $BUILDER_IMAGE "$@"
}

case "${1:-}" in
  builder) # Rebuild the builder image...
    build_builder
    exit 0
    ;;
  container) # We are now running in the builder container...
    shift
    ;; # Fallthrough to the main build logic
  bash) # start a bash shell in the container
    shift
    DOCKER_ARGS=-i run_docker bash "$@"
    exit 0
    ;;
  default) # Use docker to run the build....
    shift
    run_docker ./build.sh container "$@"
    exit 0
    ;;
  *)
    run_docker ./build.sh container
    exit 0
    ;;
esac

#
# Main build logic.. This gets run in the docker container..
#
cd ./rust
cargo build --release --target wasm32-wasip1