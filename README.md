# Chicory Runtime

[![Interpreter Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-interpreter.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-interpreter.svg)
[![AOT Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-aot.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-aot.svg)
[![WASI Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-wasi.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-wasi.svg)

[![Zulip](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://chicory.zulipchat.com/join/g4gqsxoys6orfxlrk6hn4cyp/)

<img align="right" width="200" src="chicory1.png">

Chicory is a JVM native WebAssembly runtime. It allows you to run WebAssembly programs with
zero native dependencies or JNI. Chicory can run Wasm anywhere that the JVM can go. It is designed with
simplicity and safety in mind. See the [development section](#development) for a better idea of what we are trying to achieve and why.

> *Reach out to us*: Chicory is very early in development and there will be rough edges. We're hoping to talk to some early adopters and contributors before we formally announce it a beta to the world. Please [join our team Zulip chat with this invite link](https://chicory.zulipchat.com/join/g4gqsxoys6orfxlrk6hn4cyp/) if you're interested in providing feedback or contributing. Or just keeping up with development.

## Getting Started (as a user)

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
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT
```
-->

<!--
```java
public void copyFileFromWasmCorpus(String sourceName, String destName) throws Exception {
  var dest = new File(".").toPath().resolve(destName);
  if (dest.toFile().exists()) {
    dest.toFile().delete();
  }
  Files.copy(new File(".").toPath()
          .resolve("wasm-corpus")
          .resolve("src")
          .resolve("main")
          .resolve("resources")
          .resolve("compiled")
          .resolve(sourceName),
          dest,
          StandardCopyOption.REPLACE_EXISTING);
}

var readmeResults = "readmes/main/current";
new File(readmeResults).mkdirs();

public void writeResultFile(String name, String content) throws Exception {
  FileWriter fileWriter = new FileWriter(new File(".").toPath().resolve(readmeResults).resolve(name).toFile());
  PrintWriter printWriter = new PrintWriter(fileWriter);
  printWriter.print(content);
  printWriter.flush();
  printWriter.close();
}
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
copyFileFromWasmCorpus("iterfact.wat.wasm", "factorial.wasm");
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
writeResultFile("factorial.result", "" + result);
```
-->

> *Note*: Functions in Wasm can have multiple returns but here we're just taking the first returned value.

### Memory and Complex Types

Wasm only understands basic integer and float primitives. So passing more complex types across the boundary involves
passing pointers. To read, write, or allocate memory in a module, Chicory gives you the `Memory` class. Let's look at an
example where we have a module `count_vowels.wasm`, written in rust, that takes a string input and counts the number of vowels
in the string:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/count_vowels.rs.wasm > count_vowels.wasm
```

<!--
```java
copyFileFromWasmCorpus("count_vowels.rs.wasm", "count_vowels.wasm");
```
-->

Build and instantiate this module:

```java
Instance instance = Instance.builder(Parser.parse(new File("./count_vowels.wasm"))).build();
ExportFunction countVowels = instance.export("count_vowels");
```

To pass it a string, we first need to put the string in the module's memory. To make this easier and safe,
the module gives us some extra exports to allow us to allocate and deallocate memory:

```java
ExportFunction alloc = instance.export("alloc");
ExportFunction dealloc = instance.export("dealloc");
```

Let's allocate Wasm memory for a string and put in the instance's memory. We can do this with `Memory#put`:

```java
import com.dylibso.chicory.runtime.Memory;
Memory memory = instance.memory();
String message = "Hello, World!";
int len = message.getBytes().length;
// allocate {len} bytes of memory, this returns a pointer to that memory
int ptr = (int) alloc.apply(len)[0];
// We can now write the message to the module's memory:
memory.writeString(ptr, message);
```

Now we can call `countVowels` with this pointer to the string. It will do it's job and return the count. We will
call `dealloc` to free that memory in the module. Though the module could do this itself if you want:

```java
var result = countVowels.apply(ptr, len)[0];
dealloc.apply(ptr, len);
assert(3L == result); // 3 vowels in Hello, World!
```

<!--
```java
writeResultFile("countVowels.result", "" + result);
```
-->

### Host Functions

On its own, Wasm can't do anything but compute. It cannot affect the outside world. This might seem like a weakness
but it's actually Wasm's greatest strength. By default, programs are sandboxed and have no capabilities. If you want a program
to have capabilities, you must provide them. This puts you in the seat of the operating system. A module can
ask for a capability by listing an "import" function in it's bytecode format. You can fulfill this import with a host
function written in Java. Regardless of the language of the module, it can call this Java function when it needs.
If it helps, you can think of host functions like syscalls or a languages standard library but you decide what they
are and how they behave and it's written in Java.

Let's download another example module to demonstrate this:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/host-function.wat.wasm > logger.wasm
```

<!--
```java
copyFileFromWasmCorpus("host-function.wat.wasm", "logger.wasm");
```
-->

This module expects us to fulfil an import with the name `console.log` which will allow the module to log to the stdout.
Let's write that host function:

<!--
```java
public String hostFunctionResult = "";
public void println(String value) {
  hostFunctionResult += value + "\n";
}
```
-->


```java
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.wasm.types.ValueType;
var func = new HostFunction(
    "console",
    "log",
    (Instance instance, long... args) -> { // decompiled is: console_log(13, 0);
        var len = (int) args[0];
        var offset = (int) args[1];
        var message = instance.memory().readString(offset, len);
        println(message);
        return null;
    },
    List.of(ValueType.I32, ValueType.I32),
    List.of());
```

Again we're dealing with pointers here. The module calls `console.log` with the length of the string
and the pointer (offset) in its memory. We again use the `Memory` class but this time we're pulling a string
*out* of memory. We can then print that to stdout on behalf of our Wasm program.

Note that the HostFunction needs 3 things:

1. A lambda to call when the Wasm module invokes the import
2. The namespace and function name of the import (in our case it's console and log respectively)
3. The Wasm type signature (this function takes 2 i32s as arguments and returns nothing)

Now we just need to pass this host function in during our instantiation phase:

```java
import com.dylibso.chicory.runtime.ExternalValues;
var hostFunctions = new ExternalValues(new HostFunction[] {func});
var instance = Instance.builder(Parser.parse(new File("./logger.wasm"))).withExternalValues(hostFunctions).build();
var logIt = instance.export("logIt");
logIt.apply();
// should print "Hello, World!" 10 times
```

<!--
```java
writeResultFile("hostFunction.result", hostFunctionResult);
```
-->

### Store and Instantiating Multiple Modules

A [Store][spec] is an intermediate-level abstraction that collects Wasm function, global, memory, and table instances
as named entities.

It simplifies creating instances when there are a lot of interdependencies, by collecting all the

In the simplest case, it allows to register single host functions, globals, memories and tables:

```java
import com.dylibso.chicory.runtime.Store;

// instantiate the store
var store = new Store();
// registers `console.log` in the store (see the previous section for the definition of `func`)
store.addFunction(func);
```

However, the store also automatically exposes the exports of a module to the other instances that are registered.

```java
// registers the `instance` created earlier (see the previous section) with the name `logger`
store.register("logger", instance);
// now the exported function `logIt` can be imported by other modules as `logger.logIt`
```

There is also a shorthand method to instantiate a module and register the resulting instance:

```java
var logger2 = store.instantiate("logger2", Parser.parse(new File("./logger.wasm")));
```

This is equivalent to:

```java
var external = store.toExternalValues();
var m = Parser.parse(new File("./logger.wasm"));
var instance = Instance.builder(m).withExternalValues(external).build();
store.register("logger2", instance);
```

Notice that registering two instances with the same name results in overwriting the
functions, globals, memories, tables with matching names. In this case, the new `logger2.logIt` function
overwrote the old `logger2.logIt` function.

The current `Store` is a mutable object, not meant to be shared (it is not thread-safe).

A `Store` _does not_ resolve interdependencies between modules in itself: if your set of modules
have interdependencies, you will have to instantiate and register them in the right order.

[spec]: https://www.w3.org/TR/2019/REC-wasm-core-1-20191205/#store%E2%91%A0

## Development

### Why is this needed?

If you'd prefer to watch a video instead of reading, see our [2024 Wasm I/O](https://2024.wasmio.tech/) on the subject:

[![Wasm I/O Chicory talk](https://img.youtube.com/vi/00LYdZS0YlI/0.jpg)](https://www.youtube.com/watch?v=00LYdZS0YlI)

There are a number of mature Wasm runtimes to choose from to execute a Wasm module.
To name a few [v8](https://v8.dev/), [wasmtime](https://wasmtime.dev/), [wasmer](https://wasmer.io/), [wasmedge](https://wasmedge.org/), etc.

Although these can be great choices for running a Wasm application, embedding them into your existing
Java application has some downsides. Because these runtimes are written in C/C++/Rust/etc, they must be distributed
and run as native code. This causes two main friction points:

#### 1. Distribution

If you're distributing a Java library (jar, war, etc), you must now distribute along with it a native object targeting the correct
architecture and operating system. This matrix can become quite large. This eliminates a lot of the simplicity and original benefit of shipping Java code.

#### 2. Runtime

At runtime, you must use FFI to execute the module. While there might be performance benefits to doing this for some modules,
when you do, you're effectively escaping the safety and observability of the JVM. Having a pure JVM runtime means all your
security and memory guarantees, and your tools, can stay in place.

### Goals

* Be as safe as possible
  * In that we are willing to sacrifice things like performance for safety and simplicity
* Make it easy to run Wasm in any JVM environment without native code, including very restrictive environments.
* Fully support the core Wasm spec
* Make integration with Java (and other host languages) easy and idiomatic.

### Non-Goals:

* Be a standalone runtime
* Be the fastest runtime
* Be the right choice for every JVM project

### Roadmap

Chicory development was started in September, 2023. The following are the milestones we're aiming for. These
are subject to change but represent our best guesses with current information. These are not necessarily sequential
and some may be happening in parallel. Unless specified, any unchecked box is still not planned or started.
If you have an interest in working on any of these please reach out in Zulip!

#### Bootstrap a bytecode interpreter and test suite (EOY 2023)

* [x] Wasm binary parser [link](wasm/)
* [x] Simple bytecode interpreter
* [x] Establish basic coding and testing patterns
* [x] Generate JUnit tests from wasm test suite [link](test-gen-plugin/)

#### Make the interpreter production ready (Summer 2024)

* [x] Make all tests green with the interpreter (important for correctness)
* [ ] Implement validation logic (important for safety)
  * Nearing completion
* [ ] Draft of the v1.0 API (important for stability and dx)

#### Make it fast (EOY 2024)

The primary goal here is to create an AOT compiler that generates JVM bytecode
as interpreting bytecode can only be so fast.

* [x] Decouple interpreter and create separate compiler and interpreter "engines"
* [x] Proof of concept AOT compiler (run some subset of modules)
* [ ] AOT engine passes all the same specs as interpreter (stretch goal)
  * In Progress
* [ ] Off-heap linear memory (stretch goal)

#### Make it compatible (EOY 2024)

* [ ] WASIp1 Support (including test gen)
  * We have [partial support for wasip1](wasi/) and [test generation](wasi-test-gen-plugin/)
* [ ] SIMD Support
  * Started
* [ ] Multi-Memory Support
* [ ] GC Support
* [ ] Threads Support
* [ ] Component Model Support

### Prior Art

* [asmble](https://github.com/cretz/asmble)
* [kwasm](https://github.com/jasonwyatt/KWasm)
* [wazero](https://wazero.io/)

### Building the Runtime

Contributors and other advanced users may want to build the runtime from source. To do so, you'll need to have Maven installed.
`Java version 11+` required for a proper build. You can download and install [Java 11 Temurin](https://adoptium.net/temurin/releases/?version=11)

Basic steps:

* `mvn clean install` to run all of the project's tests and install the library in your local repo
* `mvn -Dquickly` to install the library skipping all tests
* `mvn -Ddev <...goals>` to disable linters and enforcers during development
* `mvn spotless:apply` to autoformat the code
* `./scripts/compile-resources.sh` will recompile and regenerate the `resources/compiled` folders

NOTE: The `install` target relies on the `wabt` library to compile the test suite. This is not currently released for ARM (e.g. new Macs with Apple Silicon). However, `wabt` is available from Homebrew, so `brew install wabt` before running `mvn clean install` should work.

#### logging

For maximum compatibility and to avoid external dependencies we use, by default, the JDK Platform Logging (JEP 264).
You can configure it by providing a `logging.properties` using the `java.util.logging.config.file` property and [here](https://docs.oracle.com/cd/E57471_01/bigData.100/data_processing_bdd/src/rdp_logging_config.html) you can find the possible configurations.

For more advanced configuration scenarios we encourage you to provide an alternative, compatible, adapter:

- [slf4j](https://www.slf4j.org/manual.html#jep264)
- [log4j2](https://logging.apache.org/log4j/2.x/log4j-jpl.html)

It's also possible to provide a custom `com.dylibso.chicory.log.Logger` implementation if JDK Platform Logging is not available or doesn't fit.
