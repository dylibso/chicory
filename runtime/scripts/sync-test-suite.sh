#!/bin/bash

REPO_URL="https://github.com/WebAssembly/testsuite"
DIR_NAME="/tmp/wasm-testsuite"

# Check if the directory exists
if [ -d "$DIR_NAME" ]; then
  echo "Directory $DIR_NAME exists. Updating repo..."
  pushd "$DIR_NAME" || exit 1
  git pull
  popd
else
  echo "Directory $DIR_NAME does not exist. Cloning repo..."
  git clone "$REPO_URL" $DIR_NAME
fi

pushd $DIR_NAME
for file in *.wast; do
    if [[ -f $file ]]; then
      echo "Parsing $file"
      wast2json $file
    fi
done

popd
cp $DIR_NAME/*.json src/test/resources/wasm/specv1
cp $DIR_NAME/*.wasm src/test/resources/wasm/specv1

