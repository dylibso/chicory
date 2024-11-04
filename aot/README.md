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

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.experimental.aot.AotMachine;
// ...
var is = ClassLoader.getSystemClassLoader().getResourceAsStream("compiled/basic.c.wasm");
Instance.builder(Parser.parse(is)).withMachineFactory(AotMachine::new).build();
```
