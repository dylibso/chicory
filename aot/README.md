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
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.aot.AotMachine;
// ...
var is = Thread.currentThread().getContextClassLoader().getResourceAsStream("compiled/basic.c.wasm");
Instance.builder(WasmModule.builder(is).build()).withMachineFactory(AotMachine::new).build();
```
