#!/bin/bash

set -e

compileWat() {
  filename=$(basename "$1")
  (set -x; wat2wasm $1 -o "./compiled/$filename.wasm")
}

compileRust() {
  filename=$(basename "$1")
  # we can use the wasi targets for any file that end in -wasi.rs
  if [[ "$filename" =~ wasi.rs$ ]]; then
    target="wasm32-wasi"
    crate_type="bin"
  else
    target="wasm32-unknown-unknown"
    crate_type="cdylib"
  fi
  (set -x; rustc $1 --target=$target --crate-type=$crate_type -o "./compiled/$filename.wasm")
}

ENV_WASI_SDK_PATH="${WASK_SDK_PATH:-/opt/wasi-sdk}"
compileC() {
  filename=$(basename "$1")
  (set -x; ${ENV_WASI_SDK_PATH}/bin/clang -g -o "./compiled/$filename.wasm" $1 -nostartfiles -Wl,--no-entry -Wl,--export=run)
}

compileJavy() {
  filename=$(basename "$1")
  (set -x; javy compile $1 -o "./compiled/$filename.javy.wasm")
}

compile() {
  lang=$1
  case "$lang" in
    wat)
      compileWat $2
      ;;
    rust)
      compileRust $2
      ;;
    c)
      compileC $2
      ;;
    javy)
      compileJavy $2
      ;;
    *)
      echo "Don't know how to compile language $lang"
      exit 1
      ;;
  esac
}

lang="${1:-all}"
path="${2:-all}"

if [[ "$lang" == "all" ]]; then
  langs=("wat" "rust" "c" "javy")
else
  langs=("$lang")
fi

for lang in "${langs[@]}"; do
  echo "Compiling all modules in ./$lang/*"
  for file in ./$lang/*; do
    compile $lang $file
  done
done
