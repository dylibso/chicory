---
sidebar_position: 120
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
is usually worth it.  Use [Build Time Compilation](./build-time-compiler.md) if you want to avoid the penalty.

## Using

### Required Maven Changes

Add the following dependency:

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>compiler</artifactId>
</dependency>
```

### Code Changes

You enable the runtime compiler by configuring the instance to use `CompilerMachine::new` as the machine factory instead 
of the default `InterpreterMachine`.

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:compiler:999-SNAPSHOT

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "your.wasm");
```
-->

```java
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;

var module = Parser.parse(new File("your.wasm"));
var instance = Instance.builder(module).
        withMachineFactory(MachineFactoryCompiler::compile).
        build();
```

### Interpreter Fall Back

The WASM to bytecode compiler translates each WASM function into JVM method.  Occasionally you will find WASM module where functions are bigger than the maximum method size allowed by the JVM.  In these rare cases, we fall back to executing these large functions in the interpreter.  

Since interpreted functions have worse performance, we want to make sure you are aware this is happening so the runtime compiler will log messages to std error like: 

```text
Warning: using interpreted mode for WASM function index: 232
```

By default, the compiler uses `InterpreterFallback.WARN` behavior, which logs warning messages when falling back to the interpreter. If you are happy with these methods being interpreted, you can configure the compiler with `InterpreterFallback.SILENT` to silence those messages:

```java
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.compiler.InterpreterFallback;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;

var module = Parser.parse(new File("your.wasm"));
var instance = Instance.builder(module).
        withMachineFactory(
                MachineFactoryCompiler.builder(module)
                .withInterpreterFallback(InterpreterFallback.SILENT)
                .compile()
        ).
        build();
```

If you want to ensure the functions are never interpreted, you can modify the above to use `InterpreterFallback.FAIL` instead. This will throw an exception if any function is too large to compile.

An even better way to silence the use of interpreted functions (this will speed up your compile times) is to explicitly list the function indexes that should be interpreted:

```java
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.compiler.InterpreterFallback;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.File;
import java.util.Set;

var module = Parser.parse(new File("your.wasm"));
var instance = Instance.builder(module).
        withMachineFactory(
                MachineFactoryCompiler.builder(module)
                .withInterpretedFunctions(Set.of(232, 251))
                .compile()
        ).
        build();
```

### Caveats 

Please note that compiling and executing Wasm modules at runtime requires:
- an external dependency on [ASM](https://asm.ow2.io/)
- the usage of runtime reflection

This is usually fine when running on a standard JVM, but it involves some additional configuration when using tools like `native-image`.

<!--
```java
docs.FileOps.writeResult("docs/usage", "runtime-compiler.md.result", "empty");
```
-->
