---
sidebar_position: 1
sidebar_label: Quick Start
title: Quick start
---

### Install the dependency

To use the runtime, you need to add the `com.dylibso.chicory:runtime` dependency
to your dependency management system.

#### Maven

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>runtime</artifactId>
  <version>latest-release</version>
</dependency>
```

#### Gradle

```groovy
implementation 'com.dylibso.chicory:runtime:latest-release'
```


<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT
```
-->

### Loading and Instantiating Wasm Modules

First your Wasm module must be loaded from disk and then instantiated. Let's [download a test module](https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/iterfact.wat.wasm) .
This module contains some code to compute factorial:

Download from the link or with curl:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/iterfact.wat.wasm > factorial.wasm
```

<!--
```java
docs.FileOps.copyFromWasmCorpus("iterfact.wat.wasm", "factorial.wasm");
```
-->

Load this module and instantiate it:

```java
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
import java.nio.file.Path;

// point this to your path on disk
var module = Parser.parse(Path.of("./factorial.wasm"));
Instance instance = Instance.builder(module).build();
```

You can think of the `module` as of inert code, and the `instance`
is the run-time representation of that code: a virtual machine ready to execute.

### Invoking a Wasm Function

Wasm modules, like all code modules, can export functions to the outside
world. This module exports a function called `"iterFact"`. 
We can get a handle to this function using `Instance#export(String)`:

```java
ExportFunction iterFact = instance.export("iterFact");
```

`iterFact` can be invoked with the `apply()` method. We must map any Java types to raw `long`s and do the reverse
when we want to go back to Java.

```java
var result = iterFact.apply(5)[0];
System.out.println("Result: " + result); // should print 120 (5!)
```

<!--
```java
docs.FileOps.writeResult("docs", "index.md.result", "" + result);
```
-->

> *Note*: Functions in Wasm can return multiple values, hence the array. This function only returns one value, so we take the first value.
