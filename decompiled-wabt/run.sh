#!/bin/bash
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/target/classes"

java -cp "$OUT" com.dylibso.chicory.wabt.Main "$@"
