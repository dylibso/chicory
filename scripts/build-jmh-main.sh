#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

(
  cd "${SCRIPT_DIR}/.."
  git clone --depth 1 --branch initial-jmh https://github.com/dylibso/chicory.git main
  mvn -Pjmh spotless:apply clean package -DskipTests
)
