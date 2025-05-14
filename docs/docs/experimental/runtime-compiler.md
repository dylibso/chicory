---
sidebar_position: 3
sidebar_label: Runtime Compilation
title: Runtime Compilation
---
## Overview

The runtime compiler backend is a drop-in replacement for the interpreter, and it passes 100% of the same
spec tests that the interpreter already supports.

This runtime compiler translates the WASM instructions to Java bytecode on-the-fly in-memory.
The resulting code is usually expected to evaluate (much) faster and consume less memory than if it 
was interpreted.

At the current time, the compiler will eagerly compile all WASM instructions to Java bytecode.  You end up
paying a small performance penalty at Instance initialization, but the execution speedup
is usually worth it.  Use [AoT Compilation ](./aot-compiler.md) if you want to avoid the penalty.

## Using

### Required Maven Changes

Add the following dependency:

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>aot-experimental</artifactId>
</dependency>
```

### Code Changes

You enable the JIT by configuring the instance to use `CompilerMachine::new` as the machine factory instead 
of the default `InterpreterMachine`.

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:aot-experimental:999-SNAPSHOT

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "your.wasm");
```
-->

```java
import com.dylibso.chicory.experimental.aot.AotMachine;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;

var module = Parser.parse(new File("your.wasm"));
var instance = Instance.builder(module).
        withMachineFactory(AotMachine::new).
        build();
```

### Caveats 

Please note that compiling and executing AoT modules at runtime requires:
- an external dependency on [ASM](https://asm.ow2.io/)
- the usage of runtime reflection

This is usually fine when running on a standard JVM, but it involves some additional configuration when using tools like `native-image`.

<!--
```java
docs.FileOps.writeResult("docs/experimental", "runtime-compiler.md.result", "empty");
```
-->
