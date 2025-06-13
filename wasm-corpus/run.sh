#!/bin/bash
cd $(dirname "$0")

# run with `rebuild` to build the image
# run with `run` to run a command in the container
# run with no args to compile everything

if [[ "$1" = "rebuild" ]]; then
  docker build . --platform linux/amd64 -t chicory/wasm-corpus
elif [[ "$1" = "run" ]]; then
  shift
  docker run -it --platform linux/amd64 -v $(pwd)/src/main/resources:/usr/code --rm chicory/wasm-corpus "$@"
else
  # Optionally takes the args `lang` (ex: wat, rust) and `file` (ex: br.wat)
  # both default to all
  docker run --platform linux/amd64 -v $(pwd)/src/main/resources:/usr/code --rm chicory/wasm-corpus ./compile.sh $1 $2
fi
