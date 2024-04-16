#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

(
  cd "${SCRIPT_DIR}/.."
  #  TODO: fix after merged and add configurability and proper defaults
  git clone --depth 1 --branch initial-jmh https://github.com/andreaTP/chicory.git main
  mvn -Pjmh spotless:apply clean package -DskipTests
)
