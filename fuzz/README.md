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

Please note that this doesn't seem to work cross machines! e.g. `wasm-smith` returns different modules despite the seed is the same. 
But is useful for trying things out:

```
export CHICORY_FUZZ_SEED=your-seed-txt.file
export CHICORY_FUZZ_TYPES=numeric,table

mvn install -Pfuzz -pl fuzz -Dtest="com.dylibso.chicory.fuzz.SingleReproTest#singleReproducer"
```

## Import in IntelliJ

Right click on `/fuzz/pom.xml` -> "Add as a Maven Project"
