---
sidebar_position: 1
sidebar_label: Memory
title: Advanced Wasm Memory Customization
---
<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT

docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "count_vowels.rs.wasm");

System.setOut(new PrintStream(
  new BufferedOutputStream(
    new FileOutputStream("docs/examples/rust.md.result"))));

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ByteBufferMemory;

var module = Parser.parse(new File("count_vowels.rs.wasm"));
```
-->

Different Wasm workloads will be using the memory in very different ways.

Make sure you don't do changes without proper benchmarking and analysis.

It's possible to provide a custom implementation of the entire Memory used by the Wasm module:

```java
var instance = Instance.builder(module).withMemoryFactory(limits -> {
        return new ByteArrayMemory(limits);
    }).build();
```

> **NOTE:** Since Chicory 1.1.0, an optimized memory implementation called `ByteArrayMemory` is also available. We recommend plugging this  implementation on all recent OpenJDK systems for enhanced performance. On different Java runtimes (in particular, on Android VMs) you should stick to `ByteBufferMemory`.

<!--
```java
docs.FileOps.writeResult("docs/advanced", "memory.md.result", "empty");
```
-->
