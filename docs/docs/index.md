---
sidebar_position: 1
---

## Getting Started

### Install Dependency

To use the runtime, you need to add the `com.dylibso.chicory:runtime` dependency
to your dependency management system.

#### Maven

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>runtime</artifactId>
  <version>0.0.12</version>
</dependency>
```

#### Gradle

```groovy
implementation 'com.dylibso.chicory:runtime:0.0.12'
```

### Install the CLI (experimental)

The Chicory CLI is available for download on Maven at the link:

```
https://repo1.maven.org/maven2/com/dylibso/chicory/cli/<version>/cli-<version>.sh
```

you can download the latest version and use it locally with few lines:

```bash
export VERSION=$(wget -q -O - https://api.github.com/repos/dylibso/chicory/tags --header "Accept: application/json" | jq -r '.[0].name')
wget -O chicory https://repo1.maven.org/maven2/com/dylibso/chicory/cli/${VERSION}/cli-${VERSION}.sh
chmod a+x chicory
./chicory
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT
```
-->

### Loading and Instantiating Code

First your Wasm module must be loaded from disk and then "instantiated". Let's [download a test module](https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/iterfact.wat.wasm) .
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

Now let's load this module and instantiate it:

```java
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
import java.io.File;

// point this to your path on disk
Module module = Parser.parse(new File("./factorial.wasm"));
Instance instance = Instance.builder(module).build();
```

You can think of the `module` as the inert code and the `instance` as a virtual machine
loaded with the code and ready to execute.

### Invoking an Export Function

Wasm modules, like all code modules, can export functions to the outside
world. This module exports a function called `"iterFact"`. We can get a handle to this function using `Instance#export(String)`:

```java
ExportFunction iterFact = instance.export("iterFact");
```

iterFact can be invoked with the `apply()` method. We must map any java types to a wasm type and do the reverse
when we want to go back to Java. This export function takes an `i32` argument. We can use a method like `Value#asInt()`
on the return value to get back the Java integer:

```java
long result = iterFact.apply(5)[0];
System.out.println("Result: " + result); // should print 120 (5!)
```

<!--
```java
docs.FileOps.writeResult("docs", "index.md.result", "" + result);
```
-->

> *Note*: Functions in Wasm can have multiple returns but here we're just taking the first returned value.
