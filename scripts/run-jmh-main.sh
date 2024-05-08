#!/usr/bin/env bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

(
  cd "${SCRIPT_DIR}/../main" || exit 1
  java -jar "${SCRIPT_DIR}/../jmh/target/benchmarks.jar" -rf json || exit 1
)
