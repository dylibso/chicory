#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

$SCRIPT_DIR/copy-libs.sh

docker build -f $SCRIPT_DIR/src/main/docker/Dockerfile.native-scratch -t andreatp/chicory-demo $SCRIPT_DIR
