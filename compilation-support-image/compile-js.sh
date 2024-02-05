#!/bin/bash

while IFS= read -r line || [ -n "$line" ]
do
  echo "$line" >> tmp.js
done < "/dev/stdin"

/opt/javy/javy compile tmp.js -o tmp.wasm > /dev/null 2> /dev/null

cat tmp.wasm
