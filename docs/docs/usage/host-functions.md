---
id: Host functions
sidebar_position: 1
sidebar_label: Host functions
---

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
