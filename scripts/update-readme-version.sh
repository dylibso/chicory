#!/usr/bin/env bash
set -euxo pipefail

VERSION=${1}

SED_CMD="sed"
if [[ "$OSTYPE" == "darwin"* ]]; then
  SED_CMD="gsed"
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

$SED_CMD -i "s|^  <version>[^ ]*|  <version>${VERSION}</version>|" ${SCRIPT_DIR}/../README.md
$SED_CMD -i "s|^implementation 'com.dylibso.chicory[^ ]*|implementation 'com.dylibso.chicory:runtime:${VERSION}'|" ${SCRIPT_DIR}/../README.md
