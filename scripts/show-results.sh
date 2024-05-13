#!/usr/bin/env bash
set -euxo pipefail

# Two options: run locally or download from GH Actions
TYPE=${1}

if [ "$TYPE" = "local" ]; then
  echo "Display local results"
elif [ "$TYPE" = "ci" ]; then
  echo "Display CI results"
  MAIN=${2}
  CURRENT=${3}
else
  echo "Invalid option, allowed: [ci, local]"
  exit 1
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

TEMP_DIR=${SCRIPT_DIR}/jmh-tmp

rm -rf "${TEMP_DIR}"
mkdir -p "${TEMP_DIR}"

if [ "$TYPE" = "local" ]
then
  echo "Arguments not provided using local results - not supported in the container image."
  cp "${SCRIPT_DIR}"/../main/jmh-result.json "${TEMP_DIR}"/main.json
  cp "${SCRIPT_DIR}"/../jmh-result.json "${TEMP_DIR}"/current.json
else 
  echo "Arguments provided downloading the artifacts."
  wget -q "${MAIN}" -O "${TEMP_DIR}"/main.zip
  wget -q "${CURRENT}" -O "${TEMP_DIR}"/current.zip

  unzip -q -p "${TEMP_DIR}"/main.zip jmh-result.json > "${TEMP_DIR}"/main.json
  unzip -q -p "${TEMP_DIR}"/current.zip jmh-result.json > "${TEMP_DIR}"/current.json
fi

# Serve local assets
echo "To view results go to the following link:"
echo -e "\nhttp://jmh.morethan.io/?sources=http://localhost:3000/main.json,http://localhost:3000/current.json\n"
echo "Hit CTRL-C to stop the server"
(
  cd "${TEMP_DIR}"
  # Install http-server with:
  # npm install http-server -g
  http-server -s -p 3000 --cors
)
