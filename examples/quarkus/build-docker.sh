#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

cp $SCRIPT_DIR/../../wasm/target/wasm-1.0-SNAPSHOT.jar lib/wasm.jar
cp $SCRIPT_DIR/../../runtime/target/runtime-1.0-SNAPSHOT.jar lib/runtime.jar

docker build -f $SCRIPT_DIR/src/main/docker/Dockerfile.native-scratch -t andreatp/chicory-demo $SCRIPT_DIR
