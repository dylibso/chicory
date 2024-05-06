#!/bin/bash

# run with `rebuild` to build the image
# run with no args to compile everything

if [[ "$1" = "rebuild" ]]; then
  docker build . --platform linux/amd64 -t docker.io/chicory/wasm-corpus
else
  # Optionally takes the args `lang` (ex: wat, rust) and `file` (ex: br.wat)
  # both default to all
  docker run --platform linux/amd64 -v $(pwd)/src/test/resources:/usr/code --security-opt label=disable --rm docker.io/chicory/wasm-corpus $1 $2
fi
