# Chicory AOT

Experimental Wasm -> JVM bytecode AOT engine.

To enable use the AotMachine factory when building the module:

<!--
```java
//DEPS com.dylibso.chicory:wasm-corpus:999-SNAPSHOT
//DEPS com.dylibso.chicory:aot:999-SNAPSHOT
```
-->

```java
// ...

import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.aot.AotMachine;
// ...
var is = getClass().getResourceAsStream("compiled/basic.c.wasm");
Instance.

builder(Module.builder(is).

build()).

withMachineFactory(AotMachine::new).

build();
```
