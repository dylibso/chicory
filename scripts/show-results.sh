#! /bin/bash
# set -euxo pipefail
set -x

# Two options: run locally or generate a script that will download from GH Actions
MAIN=${1}
CURRENT=${2}

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

TEMP_DIR=${SCRIPT_DIR}/jmh-tmp

rm -rf ${TEMP_DIR}
mkdir -p ${TEMP_DIR}

if [ -z "$MAIN" ]
then
  echo "Arguments not provided using local results"
  cp ${SCRIPT_DIR}/../main/jmh-result.json ${TEMP_DIR}/main.json
  cp ${SCRIPT_DIR}/../jmh-result.json ${TEMP_DIR}/current.json
else 
  echo "Arguments provided downloading the results"
  # verify that works with local files too
  wget ${MAIN} -O ${TEMP_DIR}/main.zip
  wget ${CURRENT} -O ${TEMP_DIR}/current.zip

  # UNPACK and serve from local web server
fi

# Serve local assets
echo "To view results got to the following link:"
echo "http://jmh.morethan.io/?sources=http://localhost:3000/main.json,http://localhost:3000/current.json"
(
  cd ${TEMP_DIR}
  # Install with:
  # npm install http-server -g
  http-server -p 3000 --cors
)
