# Chicory WASM to Bytecode Compiler

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
import com.dylibso.chicory.compiler.MachineFactoryCompiler;

// ...
var is = ClassLoader.getSystemClassLoader().getResourceAsStream("compiled/basic.c.wasm");
        var instance = Instance.builder(Parser.parse(is)).
                withMachineFactory(MachineFactoryCompiler::compile).
                build();
```
