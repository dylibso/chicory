#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

(
  cd "${SCRIPT_DIR}/../main"
  java -jar "${SCRIPT_DIR}/../jmh/target/benchmarks.jar" -rf json
)
