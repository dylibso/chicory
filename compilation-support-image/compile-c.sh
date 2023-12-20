#!/bin/bash

while IFS= read -r line || [ -n "$line" ]
do
  echo "$line" >> tmp.c
done < "/dev/stdin"

WASI_SDK_PATH=/opt/wasi-sdk

/opt/wasi-sdk/bin/clang -O0 -g -o tmp.wasm tmp.c -nostartfiles -Wl,--no-entry -Wl,--export=run > /dev/null 2> /dev/null

cat tmp.wasm
