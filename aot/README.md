# Chicory AOT

Experimental Wasm -> JVM bytecode compiler.

To enable use the compiler Machine factory when building the module:

<!--
```java
//DEPS com.dylibso.chicory:wasm-corpus:999-SNAPSHOT
//DEPS com.dylibso.chicory:aot:999-SNAPSHOT
```
-->

```java
// ...

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.compiler.CompilerMachine;
// ...
var is = ClassLoader.getSystemClassLoader().getResourceAsStream("compiled/basic.c.wasm");
Instance.builder(Parser.parse(is)).withMachineFactory(CompilerMachine::new).build();
```
