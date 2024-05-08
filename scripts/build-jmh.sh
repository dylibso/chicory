#!/usr/bin/env bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

(
  cd "${SCRIPT_DIR}/.." || exit 1
  mvn -Pbenchmarks spotless:apply clean package -DskipTests || exit 1
)
