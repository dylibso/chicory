# Wasm Corpus


This is a central place for all of our guest source code. It can be included in other maven projects as a test
resources bundle.

If you want to add or edit guest modules for tests, you'll need to compile here. Add the program to the subfolder
for the appropriate source language. Example: src/test/resources/rust.

You can use docker to compile everything. The run.sh script can orchestrate this for you:

```bash
cd wasm-corpus

# rebuild (or build image for first time)
./run.sh rebuild

# compile all the submodules
./run.sh

# compile just one folder
./run.sh rust
```


If you want to run on your host machine you can run the `compile.sh` subscript yourself.
It has the same arguments (assumes you have WASI_SDK_PATH set for compiling c:

```bash
cd wasm-corpus/src/test/resources

# example
./compile.sh rust
```
