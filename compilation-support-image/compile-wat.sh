#!/bin/bash

while IFS= read -r line || [ -n "$line" ]
do
  echo "$line" >> tmp.wat
done < "/dev/stdin"

/opt/wabt/bin/wat2wasm tmp.wat -o tmp.wasm > /dev/null 2> /dev/null

cat tmp.wasm
