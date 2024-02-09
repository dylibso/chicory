# fuzz

Fuzz testing module - kept separate as it needs some additional tools to be installed globally.

## Requirements

`wasmtime` (17.0.0) and, at the moment we need to use `export WASMTIME_NEW_CLI=0` to get the output on stdout, cannot find an equivalent with the new cli ...
`wasm-tools` (1.0.57) to use `wasm-smith`

The Chicory cli should be compiled and available in the `cli/target` folder.

To change the number of iterations of the repeated test you can use java properties, e.g.:

```
mvn clean install -Pfuzz -pl fuzz -Dfuzz.test.numeric=10 -Dfuzz.test.table=5
```

The defaults are defined in the root `pom.xml` under the `fuzz` profile.

## Run a single reproducer

```
export CHICORY_SEED_

```

## Import in IntelliJ

Right click on `/fuzz/pom.xml` -> "Add as a Maven Project"
