#!/usr/bin/env bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

(
  cd "${SCRIPT_DIR}/.."
  rm -rf main
  git clone --depth 1 --branch main https://github.com/dylibso/chicory.git main
  mvn -Dquickly -Pbenchmarks
)
