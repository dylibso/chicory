#!/bin/bash

set -e

compileWat() {
  filename=$(basename "$1")
  (set -x; wasm-tools parse $1 -o "./compiled/$filename.wasm")
}

compileRust() {
  filename=$(basename "$1")
  # Target is based on the crate-type
  target="wasm32-unknown-unknown"
  if [[ -f "$1/src/main.rs" ]]; then
    target="wasm32-wasip1"
  fi
  (set -x; cd $1 && cargo build --target=$target &&  cp ./target/${target}/debug/*.wasm "../../compiled/$filename.rs.wasm")
}

compileRustAtomics() {
  filename=$(basename "$1")
  target="wasm32-unknown-unknown"
  (set -x; cd $1 && \
    RUSTFLAGS='-C target-feature=+atomics,+bulk-memory -C panic=abort -C link-arg=--export=__stack_pointer' \
    cargo +nightly-2025-09-01 build --target=$target -Z build-std=core,alloc,std,compiler_builtins,panic_abort --release && \
    cp ./target/${target}/release/*.wasm "../../compiled/$filename.rs.wasm")
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

compileSwift() {
  filename=$(basename "$1")
  (set -x; cd $1 && \
    swiftly use 6.2-snapshot -y && \
    swift build -c release --swift-sdk swift-6.2-DEVELOPMENT-SNAPSHOT-2025-07-09-a_wasm-embedded && \
    cp .build/wasm32-unknown-wasip1/release/*.wasm "../../compiled/$filename.swift.wasm" && \
    rm -rf .build)
}

compileKotlin() {
  filename=$(basename "$1")
  (set -x; cd $1 && ./gradlew build && \
    cp ./build/compileSync/wasmWasi/main/productionExecutable/kotlin/*.wasm \
    "../../compiled/$filename.kt.wasm")
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
    rust-atomics)
      compileRustAtomics $2
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
    swift)
      compileSwift $2
      ;;
    kotlin)
      compileKotlin $2
      ;;
    *)
      echo "Don't know how to compile language [$lang]"
      exit 1
      ;;
  esac
}

lang="${1:-all}"
path="${2:-all}"

if [[ "$lang" == "all" ]]; then
  langs=("wat" "rust" "c" "javy" "javy-dynamic" "tinygo" "go" "dotnet" "swift" "kotlin")
else
  langs=("$lang")
fi

for lang in "${langs[@]}"; do
  echo "Compiling all modules in ./$lang/*"
  for file in ./$lang/*; do
    compile $lang $file
  done
done
