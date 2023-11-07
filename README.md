# Chicory Runtime

[![Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/8a410d60b8840e7df415f81a3490acf0aac79a93/badge.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/8a410d60b8840e7df415f81a3490acf0aac79a93/badge.svg)

<img align="right" width="200" src="chicory1.png">

Chicory is a JVM native WebAssembly runtime. It allows you to run WebAssembly programs with
zero native dependencies or JNI. Chicory can run Wasm anywhere that the JVM can go. It is designed with
simplicity and safety in mind. See the [development section](#development) for a better idea of what we are trying to achieve and why.

## Getting Started

### Install Dependency

To use the runtime, you need to add the `com.dylibso.chicory:runtime` dependency
to your dependency management system.

#### Maven

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>chicory</artifactId>
  <version>0.0.1</version>
</dependency>
```

#### Gradle

```groovy
implementation 'com.dylibso:runtime:0.0.1'
```

### Loading and Instantiating Code

First your Wasm module must be loaded from disk and then "instantiated". Let's [download a test module](https://github.com/dylibso/chicory/raw/main/runtime/src/test/resources/wasm/iterfact.wat.wasm).
This module contains some code to compute factorial:

Download from the link or with curl:

```bash
curl https://github.com/dylibso/chicory/raw/main/runtime/src/test/resources/wasm/iterfact.wat.wasm > factorial.wasm
```

Now let's load this module and instantiate it:

```java
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.runtime.*;

// point this to your path on disk
Module module = Module.build("./factorial.wasm");
Instance instance = module.instantiate();
```

You can think of the `module` as the inert code and the `instance` as a virtual machine
loaded with the code and ready to execute.

### Invoking an Export Function

Wasm modules, like all code modules, can export functions to the outside
world. This module exports a function called `"iterFact"`. We can get a handle to this function using `Instance#getExport(String)`:

```java
ExportFunction iterFact = instance.getExport("iterFact");
```

iterFact can be invoked with the `apply()` method. We must map any java types to a wasm type and do the reverse
when we want to go back to Java. This export function takes an `i32` argument. We can use a method like `Value#asInt()`
on the return value to get back the Java integer:

```java
Value result = iterFact.apply(Value.i32(5))[0];
assertEquals(120, result.asInt());
```

> *Note*: Functions in Wasm can have multiple returns but here we're just taking the first returned value.

### Memory and Complex Types

Wasm only understands basic integer and float primitives. So passing more complex types across the boundary involves
passing pointers. To read, write, or allocate memory in a module, Chicory gives you the `Memory` class. Let's look at an
example where we have a module `count_vowels.wasm`, written in rust, that takes a string input and counts the number of vowels
in the string:

```bash
curl https://github.com/dylibso/chicory/raw/main/runtime/src/test/resources/wasm/count_vowels.rs.wasm > count_vowels.wasm
```

Build and instantiate this module:

```java
Instance instance = Module.build("./count_vowels.wasm").instantiate();
ExportFunction countVowels = instance.getExport("count_vowels");
```

To pass it a string, we first need to put the string in the module's memory. To make this easier and safe,
the module gives us some extra exports to allow us to allocate and deallocate memory:

```java
ExportFunction alloc = instance.getExport("alloc");
ExportFunction dealloc = instance.getExport("dealloc");
```

Let's allocate Wasm memory for a string and put in the instance's memory. We can do this with `Memory#put`:

```java
Memory memory = instance.getMemory();
String message = "Hello, World!";
int len = message.getBytes().length;
// allocate {len} bytes of memory, this returns a pointer to that memory
int ptr = alloc.apply(Value.i32(len))[0].asInt();
// We can now write the message to the module's memory:
memory.put(ptr, message);
```

Now we can call `countVowels` with this pointer to the string. It will do it's job and return the count. We will
call `dealloc` to free that memory in the module. Though the module could do this itself if you want:

```java
Value result = countVowels.apply(Value.i32(ptr), Value.i32(len))[0];
dealloc.apply(Value.i32(ptr), Value.i32(len));
assertEquals(3, result.asInt()); // 3 vowels in Hello, World!
```

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
curl https://github.com/dylibso/chicory/raw/main/runtime/src/test/resources/wasm/host-function.wat.wasm > logger.wasm
```

This module expects us to fulfil an import with the name `console.log` which will allow the module to log to the stdout.
Let's write that host function:

```java
var func = new HostFunction(
    (Memory memory, Value... args) -> { // decompiled is: console_log(13, 0);
        var len = args[0].asInt();
        var offset = args[1].asInt();
        var message = memory.getString(offset, len);
        System.out.println(message);
        return null;
    },
    "console",
    "log",
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
var funcs = new HostFunction[] {func};
var instance = Module.build("./logger.wasm").instantiate(funcs);
var logIt = instance.getExport("logIt");
logIt.apply();
// should print "Hello, World!" 10 times
```

## Development

### Why is this needed?

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
JVM program.

### Goals

* Be as safe as possible
  * In that we are willing to sacrifice things like performance for safety and simplicity
* Make it easy to run Wasm in any JVM environment without native code, including very restrictive environments.
* Fully support the core Wasm 1.0 and 2.0 spec specs
* Make integration with Java (and other host languages) easy and idiomatic.

### Non-Goals:

* Be a standalone runtime
* Be the fastest runtime
* Be the right choice for every JVM project

### Roadmap

* [x] Wasm binary parser [link](wasm/)
* [x] Simple bytecode interpreter
* [x] Generate JUnit tests from wasm test suite [link](test-gen-plugin/)
* [ ] Make all tests green with the interpreter
* [ ] Implement validation logic
* [ ] Performance improvements
* [ ] AOT compiler (generate JVM bytecode from Wasm module)

Some nice to have but probably separate items:

* [ ] Off-heap linear memory
* [ ] WASI Support
* [ ] GC Support
* [ ] Threads Support
* [ ] Component Model Support

### Prior Art

* [asmble](https://github.com/cretz/asmble)
* [kwasm](https://github.com/jasonwyatt/KWasm)

### Building Locally

* `mvn clean install` to run all of the project's tests and install the library in your local repo
* `mvn spotless:apply` to autoformat the code

### Modules

There are three independent modules at the moment:

* wasm
* test-gen-plugin
* runtime

#### wasm package

The [wasm](wasm/) package contains a lot of the core Wasm types and the binary parser.
It can be useful as an independent library for using wasm in Java.

There are a few scripts that we use.

```bash
cd wasm

# Recompiles all the wasm modules we use in the tests
# Only needs to be run if the code for the test wasm modules are changes
sh scripts/compile-tests.sh

# Parses the instructions.tsv file and generates the OpCode.java file.
# Ony needs to be run if the instructions.tsv file changes
ruby scripts/gen-instr.rb
```

#### test-gen-plugin

The [test-gen-plugin](test-gen-plugin/) package is a maven plugin that handles the test generation that exercises both the wasm package and the runtime package. Tests are parsed from the [Wasm testsuite](https://github.com/WebAssembly/testsuite) and generate Java Junit tests.

#### runtime package

The [runtime](runtime/) packages contains the actual Chicory runtime. There are a few scripts we use here too:

```bash
cd runtime

# Recompiles all the wasm modules we use in the tests
# Only needs to be run if the code for the test wasm modules are changes
sh scripts/compile-tests.sh
```

#### logging

For maximum compatibility and to avoid external dependencies we use the JDK Platform Logging (JEP 264).
You can configure it by providing a `logging.properties` using the `java.util.logging.config.file` property and [here](https://docs.oracle.com/cd/E57471_01/bigData.100/data_processing_bdd/src/rdp_logging_config.html) you can find the possible configurations.

For more advanced configuration scenarios we encourage you to provide an alternative, compatible, adapter:

- [slf4j](https://www.slf4j.org/manual.html#jep264)
- [log4j2](https://logging.apache.org/log4j/2.x/log4j-jpl.html)
