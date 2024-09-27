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

compileJavyDynamic() {
  filename=$(basename "$1")
  (set -x; javy emit-provider -o "./compiled/quickjs-provider.javy-dynamic.wasm" && \
  javy compile -d $1 -o "./compiled/$filename.javy-dynamic.wasm")
}

compileTinyGo() {
  filename=$(basename "$1")
  (set -x; tinygo build -o "./compiled/$filename.tiny.wasm" -target=wasi $1)
}

compileGo() {
  filename=$(basename "$1")
  (set -x; GOOS=wasip1 GOARCH=wasm go build -o "./compiled/$filename.wasm" $1)
}

compileDotnet() {
  filename=$(basename "$1")
  (set -x; cd $1 && dotnet publish -c Release && cp ./bin/Release/net8.0/wasi-wasm/AppBundle/*.wasm "../../compiled/$filename.dotnet.wasm")
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
    javy-dynamic)
      compileJavyDynamic $2
      ;;
    tinygo)
      compileTinyGo $2
      ;;
    go)
      compileGo $2
      ;;
    dotnet)
      compileDotnet $2
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
  langs=("wat" "rust" "c" "javy" "javy-dynamic" "tinygo", "go", "dotnet")
else
  langs=("$lang")
fi

for lang in "${langs[@]}"; do
  echo "Compiling all modules in ./$lang/*"
  for file in ./$lang/*; do
    compile $lang $file
  done
done
