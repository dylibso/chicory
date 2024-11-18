---
sidebar_position: 1
sidebar_label: Rust
title: Using Rust with Chicory
---
## Compile Rust to Wasm

Compiling a Rust library to Wasm is easy and can be performed using standard `rustc` options:

```bash
rustc --target=wasm32-unknown-unknown --crate-type=cdylib
```

when you need to add support for wasi preview 1(typically when using CLIs) you can use:

```bash
rustc --target=wasm32-wasi --crate-type=bin
```

## Using in Chicory

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT

docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "count_vowels.rs.wasm");

System.setOut(new PrintStream(
  new BufferedOutputStream(
    new FileOutputStream("docs/examples/1-rust.md.result"))));
```
-->

```java
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;

var instance = Instance.builder(Parser.parse(new File("count_vowels.rs.wasm"))).build();

var alloc = instance.export("alloc");
var dealloc = instance.export("dealloc");
var countVowels = instance.export("count_vowels");

var memory = instance.memory();
var message = "Hello, World!";
var len = message.getBytes().length;
int ptr = (int) alloc.apply(len)[0];

memory.writeString(ptr, message);

var result = countVowels.apply(ptr, len)[0];
System.out.println(result);
```
