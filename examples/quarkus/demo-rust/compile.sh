#! /bin/bash
set -euxo pipefail

# rustup target add wasm32-unknown-unknown

rustc main.rs --target=wasm32-unknown-unknown --crate-type=cdylib -C opt-level=0 -C debuginfo=0 -o main.wasm
