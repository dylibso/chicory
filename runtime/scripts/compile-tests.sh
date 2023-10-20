#!/bin/bash

WASI_SDK_PATH=/opt/wasi-sdk

WATS="./src/test/resources/wasm/*.wat"
for w in $WATS
do
  echo "Compiling $w file..."
  wat2wasm $w -o "$w.wasm"
done

RUSTS="./src/test/resources/wasm/*.rs"
for w in $RUSTS
do
  echo "Compiling $w file..."
  file_basename=$(basename "$w")
  if [[ "$file_basename" == "wasi_"* ]]; then
    rustc $w --target=wasm32-wasi -C opt-level=0 -C debuginfo=0 -o $w.wasm
  else
    rustc $w --target=wasm32-unknown-unknown --crate-type=cdylib -C opt-level=0 -C debuginfo=0 -o $w.wasm
  fi
done

CS="./src/test/resources/wasm/*.c"
for w in $CS
do
  echo "Compiling $w file..."
  /opt/wasi-sdk/bin/clang -O0 -g -o $w.wasm $w -nostartfiles -Wl,--no-entry -Wl,--export=run
done
