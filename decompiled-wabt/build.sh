#!/bin/bash
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/target/classes"

rm -rf "$OUT"
mkdir -p "$OUT"

javac \
  -d "$OUT" \
  $(find "$DIR/src/main/java" -name '*.java')

cp -r "$DIR/src/main/resources/"* "$OUT/"

echo "Build complete: $OUT"
