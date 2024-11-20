---
sidebar_position: 5
sidebar_label: WABT
title: WABT
---
# WebAssembly Binary Toolkit

Since we use them in the build of the project, we publish a few [wabt](https://github.com/WebAssembly/wabt) tools AOT compiled with Chicory.
Adding support for more tools is easy and we welcome contributions in this direction.
The relevant module can be added to the build with:

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>wabt</artifactId>
  <version>latest-release</version>
</dependency>
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:wabt:999-SNAPSHOT

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;

System.setOut(new PrintStream(
  new BufferedOutputStream(
    new FileOutputStream("docs/usage/wabt.md.result"))));
```
-->

## wat2wasm

In Chicory, we don't have a Wasm text format parser just yet.
To overcome this limitation you can use `wat2wasm`, for example:

```java
import com.dylibso.chicory.wabt.Wat2Wasm;

var wasm = Wat2Wasm.parse(
    "(module (func (export \"add\") (param $x i32) (param $y i32) (result i32)"
    + " (i32.add (local.get $x) (local.get $y))))");

var moduleInstance = Instance.builder(Parser.parse(wasm)).build();

var addFunction = moduleInstance.export("add");
var result = addFunction.apply(1, 41)[0];

System.out.println(result);
```
