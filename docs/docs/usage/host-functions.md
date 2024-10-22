---
id: Host functions
sidebar_position: 1
sidebar_label: Host functions
---

### Host Functions

In Wasm, the instance of a module is generally regarded as the _guest_,
and the surrounding runtime environment is usually called the _host_.

For example, an application using Chicory as a library would be the host
to a Wasm module you have instantiated.

In previous section, we saw that Wasm modules may _export_ functions, so that
they can be externally invoked. But Wasm modules may also _import_ functions. 
These imports have to be resolved at the time when a module is instantiated;
in other words, when a module is instantiated, the runtime has to provide
references for all the imports that a module declares. 

The import/export mechanism is the way through which Wasm interacts 
with the outside world: without imports, a Wasm module is "pure compute",
in other words, it cannot perform any kind of I/O. This puts you in the seat of the operating system. 

One way to fulfill imports is with a host function written in Java. 
Regardless of the source language that originated your Wasm module, it will be able to call this Java function when needed.

If it helps, you can think of host functions like of syscalls or a the standard library of your favorite language.
However, instead of having a default implementation, you use Java to decide what they do and how they behave.

Let's download another example module to demonstrate this:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/host-function.wat.wasm > logger.wasm
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT
```
-->
<!--
```java
docs.FileOps.copyFromWasmCorpus("host-function.wat.wasm", "logger.wasm");
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
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.wasm.types.ValueType;

var func = new HostFunction(
    "console",
    "log",
    List.of(ValueType.I32, ValueType.I32),
    List.of(),
    (Instance instance, long... args) -> { // decompiled is: console_log(13, 0);
        var len = (int) args[0];
        var offset = (int) args[1];
        var message = instance.memory().readString(offset, len);
        println(message);
        return null;
    });
```

The module calls `console.log` with the length of the string and an index (offset) in its memory. 
This is essentially a pointer into the Wasm's "linear memory".
The `Instance` class provides a reference to a `Memory` object instance, that allows 
to pull a value out of memory. You can use the `readString()` convenience method
to read a buffer into a `String`; we can then print that to stdout on behalf of our Wasm program.

Note that the `HostFunction` needs 3 things:

1. A lambda to call when the Wasm module invokes the import
2. The namespace and function name of the import (in our case it's `console` and `log` respectively)
3. The Wasm type signature (this function takes 2 i32s as arguments and returns nothing)


Now we just need to pass this host function when we instantiate the module:

```java
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.ImportValues;
var hostFunctions = new ImportValues(new HostFunction[] {func});
var instance = Instance.builder(Parser.parse(new File("./logger.wasm"))).withImportValues(hostFunctions).build();
var logIt = instance.export("logIt");
logIt.apply();
// should print "Hello, World!" 10 times
```

<!--
```java
docs.FileOps.writeResult("docs/usage", "host-functions.md.result", hostFunctionResult);
```
-->
