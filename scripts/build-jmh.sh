#!/usr/bin/env bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

(
  cd "${SCRIPT_DIR}/.."
  mvn -Dquickly -Pbenchmarks
)
