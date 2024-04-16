#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

(
  cd "${SCRIPT_DIR}/.."
  #  TODO: fix
  git clone --depth 1 --branch initial-jmh https://github.com/andreaTP/chicory.git main
  mvn -Pjmh spotless:apply clean package -DskipTests
)
