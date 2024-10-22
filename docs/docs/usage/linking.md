---
id: Linking
sidebar_position: 1
sidebar_label: Linking
---

### Store and Instantiating Multiple Modules

A [Store][spec] is an intermediate-level abstraction that collects Wasm function, global, memory, and table instances
as named entities.

It simplifies creating instances when there are a lot of interdependencies, by collecting all the

In the simplest case, it allows to register single host functions, globals, memories and tables:

<!--
TODO: should we make this more explicit?
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
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
var hostFunctions = new ImportValues(new HostFunction[] {func});
var instance = Instance.builder(Parser.parse(new File("./logger.wasm"))).withImportValues(hostFunctions).build();
```
-->

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
var external = store.toImportValues();
var m = Parser.parse(new File("./logger.wasm"));
var instance = Instance.builder(m).withImportValues(external).build();
store.register("logger2", instance);
```

Notice that registering two instances with the same name results in overwriting the
functions, globals, memories, tables with matching names. In this case, the new `logger2.logIt` function
overwrote the old `logger2.logIt` function.

The current `Store` is a mutable object, not meant to be shared (it is not thread-safe).

A `Store` _does not_ resolve interdependencies between modules in itself: if your set of modules
have interdependencies, you will have to instantiate and register them in the right order.

[spec]: https://www.w3.org/TR/2019/REC-wasm-core-1-20191205/#store%E2%91%A0

<!--
```java
docs.FileOps.writeResult("docs/usage", "linking.md.result", "empty");
```
-->
