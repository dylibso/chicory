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
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.aot.AotMachine;
// ...

var module = Module.builder("compiled/basic.c.wasm").withMachineFactory(AotMachine::new).build();
```
