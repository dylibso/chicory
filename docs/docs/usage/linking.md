---
sidebar_position: 40
sidebar_label: Linking
title: Linking
---
# Linking

In the [Host Functions section](host-functions.md) we met the `Store` for the first-time.

A [Store][spec] is an intermediate-level abstraction that collects Wasm function, global, memory, and table instances
as named entities. It simplifies creating instances, especially when there are a lot of interdependencies.

In the simplest case, it allows to register single host functions, globals, memories and tables. For instance, we already saw how to register a `console.log()` host function to the `Store`:

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT
```
-->

```java
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.types.ValType;

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

// instantiate the store
var store = new Store();
// registers `console.log` in the store
store.addFunction(func);
```

However, the store also automatically exposes the exports of a module to the other instances that are registered. In fact, in the [Host Functions section](host-functions.md), when we created our instance from the `logger.wasm` module, we also passed a string `"logger"`. This is the name of the instance:

```java
// create a named `instance` with name `logger`
var instance = store.instantiate("logger", Parser.parse(new File("./logger.wasm")));
```

Because this instance is now named, now any exports in the `logger` module will be automatically qualified.
For instance, the exported function `logIt` will be visible by other modules as `logger.logIt`.

## Notes

- The invocation `store.instantiate("logger", ...)` is in fact equivalent to the lower-level sequence:

    ```java
    var imports = store.toImportValues();
    var m = Parser.parse(new File("./logger.wasm"));
    var instance = Instance.builder(m).withImportValues(imports).build();
    store.register("logger", instance);
    ```

    However, in most cases we recommend to use the shorthand form.

- Also notice that registering two instances with the same name results in overwriting the
functions, globals, memories, tables with matching names. In this case, the new `logger2.logIt` function
overwrote the old `logger2.logIt` function.

- The current `Store` is a mutable object, not meant to be shared (it is not thread-safe).

- A `Store` _does not_ resolve interdependencies between modules in itself: if your set of modules
have interdependencies, you will have to instantiate and register them in the right order.

[spec]: https://www.w3.org/TR/2019/REC-wasm-core-1-20191205/#store%E2%91%A0

<!--
```java
docs.FileOps.writeResult("docs/usage", "linking.md.result", "empty");
```
-->
