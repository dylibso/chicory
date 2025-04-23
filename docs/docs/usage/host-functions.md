---
sidebar_position: 20
sidebar_label: Host Functions
title: Host Functions
---
# Host and guests

In Wasm, the instance of a module is generally regarded as the **guest**,
and the surrounding runtime environment is called the **host**.

For example, an **application** using Chicory as a **library** would be the **host**
to a Wasm module **guest** you have instantiated.

Wasm modules may **export** functions, so that they can be externally invoked.
But Wasm modules may also **import** functions. 
These imports have to be resolved at the time when a module is instantiated;
in other words, when a module is instantiated, the runtime has to provide
references for all the imports that a module declares.

The import/export mechanism is the way through which Wasm interacts 
with the outside world: without imports, a Wasm module is "pure compute",
that is, it cannot perform any kind of I/O, nor can it interact with other
modules.

## Host Functions

One way to fulfill imports is providing a **host function** written in Java. 
No matter what source language that your Wasm module was written in, 
it will be able to call this Java function when needed.

It is called a **host** function because it is executed in the environment of the
**host** (in this case, a JVM). As opposed to any other Wasm function, 
a **host function** is _unrestricted_ and it may interact with the surrounding
environment in any arbitrary way. This lets you effectively escape the sandbox.
As a consequence, host functions are a security boundary and
need to be implemented carefully if the Wasm code is not trusted.

You can think of host functions as similar to OS system calls or the standard library in your favorite programming language.
The key difference is that, instead of relying on a default implementation, you use Java to define their behavior 
and determine what they do.

Let's see it with another example. Download the following Wasm binary:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/host-function.wat.wasm > logger.wasm
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT

docs.FileOps.copyFromWasmCorpus("host-function.wat.wasm", "logger.wasm");
```
-->

This module expects us to fulfil an import with the name `console.log`. 
As the name implies, this function allows a caller to log a message to standard output.
We could write it as the host function:

<!--
```java
System.setOut(new PrintStream(
  new BufferedOutputStream(
    new FileOutputStream("docs/usage/host-functions.md.result"))));
```
-->


```java
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.wasm.types.ValType;

var func = new HostFunction(
    "console",
    "log",
    List.of(ValueType.I32, ValueType.I32),
    List.of(),
    (Instance instance, long... args) -> {
        var len = (int) args[0];
        var offset = (int) args[1];
        var message = instance.memory().readString(offset, len);
        System.out.println(message);
        return null;
    });
```

The module calls `console.log` with the length of the string and an index (offset) in its memory. 
This is essentially a pointer into the Wasm's linear memory.
The `Instance` class provides a reference to a `Memory` object instance, that allows 
to pull a value out of memory. You can use the `readString()` convenience method
to read a buffer into a `String`; we can then print that to stdout on behalf of our Wasm program.

Note that the `HostFunction` needs 3 things:

1. The namespace and function name of the import (in our case it's `console` and `log` respectively)
2. The Wasm type signature (this function takes two `i32`s as arguments and returns nothing)
3. A lambda to call when the Wasm module invokes the import


We now need to pass this host function when we instantiate the module.
We can do so by using a `Store`:

```java
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Store;

// instantiate the store
var store = new Store();
// registers `console.log` in the store
store.addFunction(func);
var instance = store.instantiate("logger", Parser.parse(new File("./logger.wasm")));
var logIt = instance.export("logIt");
logIt.apply();
// should print "Hello, World!" 10 times
```

> **_NOTE:_** For an easier way to write host function, see [Host Modules Annotations](../experimental/host-modules.md).
