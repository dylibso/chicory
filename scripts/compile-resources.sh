#! /bin/bash
set -euxo pipefail

CONTAINER_IMAGE="docker.io/andreatp/chicory-compilation-support"
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

declare -a arr=("wasm" "runtime")

for i in "${arr[@]}"
do
  rm -rf "${SCRIPT_DIR}/../${i}/src/test/resources/compiled"
  mkdir -p "${SCRIPT_DIR}/../${i}/src/test/resources/compiled"

  WATS="${SCRIPT_DIR}/../${i}/src/test/resources/sources/*.wat"
  for w in $WATS
  do
    if test -f "$w"; then
      out=$(echo $w | sed 's|resources/sources|resources/compiled|')
      cat $w | docker run --rm -i --entrypoint "./compile-wat.sh" ${CONTAINER_IMAGE} > $out.wasm
    fi
  done

  RUSTS="${SCRIPT_DIR}/../${i}/src/test/resources/sources/*.rs"
  for w in $RUSTS
  do
    if test -f "$w"; then
      out=$(echo $w | sed 's|resources/sources|resources/compiled|')
      cat $w | docker run --rm -i --entrypoint "./compile-rust.sh" ${CONTAINER_IMAGE} > $out.wasm
    fi
  done

  CS="${SCRIPT_DIR}/../${i}/src/test/resources/sources/*.c"
  for w in $CS
  do
    if test -f "$w"; then
      out=$(echo $w | sed 's|resources/sources|resources/compiled|')
      cat $w | docker run --rm -i --entrypoint "./compile-c.sh" ${CONTAINER_IMAGE} > $out.wasm
    fi
  done
done
