---
sidebar_position: 90
sidebar_label: Simd
title: Simd support
---

> **NOTE:** SIMD support is available only for Java 21+ and interpreter mode

If you are using a version of Java that supports [JEP 448 - Vector API](https://openjdk.org/jeps/448) you can leverage [Vector instructions](https://webassembly.github.io/spec/core/syntax/instructions.html#vector-instructions).

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:simd:999-SNAPSHOT

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;

docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "your.wasm");
```
-->

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT

docs.FileOps.writeResult("docs/usage", "logging.md.result", "empty");
```
-->

After adding the dependency:

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>simd</artifactId>
</dependency>
```

You can instantiate a module with SIMD support by explicitly providing a `MachineFactory`:

```java
import com.dylibso.chicory.simd.SimdInterpreterMachine;

var module = Parser.parse(new File("your.wasm"));
var instance = Instance.builder(module).withMachineFactory(SimdInterpreterMachine::new).build();
```

> **_NOTE:_**  Modules loaded without validation enabled are **NOT** supported (i.e. no:`WasmModule.builder().withValidation(false)`).

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT

docs.FileOps.writeResult("docs/usage", "simd.md.result", "empty");
```
-->

