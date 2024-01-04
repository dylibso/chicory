#!/bin/bash

while IFS= read -r line || [ -n "$line" ]
do
  echo "$line" >> tmp.rs
done < "/dev/stdin"

rustc tmp.rs --target=wasm32-unknown-unknown --crate-type=cdylib -C opt-level=0 -C debuginfo=0 -o tmp.wasm > /dev/null 2> /dev/null

cat tmp.wasm
