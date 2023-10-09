# Chicory Runtime

Chicory is a JVM native WebAssembly runtime.
It allows you to run WebAssembly programs with no native dependencies or JNI.

### Goals

* Be as safe as possible
  * This line is pretty relative
  * It depends on the resources we have to verify security
* Make it easy to run Wasm in any JVM environment without native code, including very restrictive environments.
* Fully support the core Wasm 1.0 and 2.0 spec specs
* Make integration with Java (and other host languages) easy and idiomatic.

### Non-Goals:

* Be a standalone runtime
* Be the fastest runtime
* Be the right choice for every JVM project

### Nice-to-Haves:

* WASI support
    * We may develop this outside of this library
    * Which version we target may depend on when we achieve the goals
* Support JITing to JVM bytecode
    * Only if it can be done safely
* Demonstrate performance benefits for at least some subset of use cases
* Support some future standards like Threads, GC, and Component Model


## API

Here is an example of the current API. This will certainly change soon.
See the specs in [runtime/src/test/java/chicory/runtime/ModuleTest.java](runtime/src/test/java/chicory/runtime/ModuleTest.java)
for some more details.

```java
var func = new HostFunction(
        (Memory memory, Value... args) -> {
            var offset = args[0].asInt();
            var len = args[1].asInt();
            var message = memory.getString(offset, len);
            System.out.println(message + " --from Java");
            return null;
        },
        "console",
        "log",
        List.of(ValueType.I32, ValueType.I32),
        List.of()
);
var funcs = new HostFunction[]{func};
var instance = Module.build("src/test/resources/wasm/host-function.wat.wasm").instantiate(funcs);
var logIt = instance.getExport("logIt");
logIt.apply();
// prints "Hello, World! --from Java"
```

## Development

* `mvn clean test` to run all of the project's tests.
* `mvn spotless:apply` to autoformat the code

## Modules

There are three independent modules at the moment:

* wasm
* test-gen-plugin
* runtime

### wasm package

The [wasm](wasm/) package contains a lot of the core Wasm types and the binary parser.
It can be useful as an independent library for using wasm in Java.

There are a few scripts that we use.

```bash
cd wasm

# Recompiles all the wasm modules we use in the tests
# Only needs to be run if the code for the test wasm modules are changes
sh scripts/compile-tests.sh

# Parses the instructions.tsv file and generates the OpCode.java file.
# Ony needs to be run if the instructions.tsv file changes
ruby scripts/gen-instr.rb
```

### test-gen-plugin

The [test-gen-plugin](test-gen-plugin/) package is a maven plugin that handles the test generation that exercises both the wasm package and the runtime package. Tests are parsed from the [Wasm testsuite](https://github.com/WebAssembly/testsuite) and generate Java Junit tests.

### runtime package

This contains the actual Chicory runtime. There are a few scripts we use here too:

```bash
cd runtime

# Recompiles all the wasm modules we use in the tests
# Only needs to be run if the code for the test wasm modules are changes
sh scripts/compile-tests.sh
```
