#! /bin/bash
set -euxo pipefail

MAIN=${1}
CURRENT=${2}

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

TEMP_DIR=${SCRIPT_DIR}/jmh-tmp

rm -rf ${TEMP_DIR}
mkdir -p ${TEMP_DIR}

# verify that works with local files too
wget ${MAIN} -O ${TEMP_DIR}/main.zip
wget ${CURRENT} -O ${TEMP_DIR}/current.zip

# UNPACK and serve from local web server