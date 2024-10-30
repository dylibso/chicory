# Guests and Hosts

In Wasm, the instance of a module is generally regarded as the **guest**,
and the surrounding runtime environment is usually called the **host**.


For example, an **application** using Chicory as a **library** would be the **host**
to a Wasm module you have instantiated.

In previous section, we saw that Wasm modules may **export** functions, so that
they can be externally invoked. But Wasm modules may also **import** functions. 
These imports have to be resolved at the time when a module is instantiated;
in other words, when a module is instantiated, the runtime has to provide
references for all the imports that a module declares. 

The import/export mechanism is the way through which Wasm interacts 
with the outside world: without imports, a Wasm module is "pure compute",
that is, it cannot perform any kind of I/O, nor can it interact with other
modules. 

## Host Functions

One way to fulfill imports is providing a **host function** written in Java. 
Regardless of the source language that originated your Wasm module, 
it will be able to call this Java function when needed.

It is called a **host** function, because it is written in the language of the
**host** (in this case, a JVM). As opposed to any other Wasm function, 
a **host function** is _unrestricted_ and it may interact with the surrounding
environment in any arbitrary way. This let you "escape the sandbox".

If it helps, you can think of host functions as similar to system calls 
or the standard library in your favorite programming language. The key difference is that, 
instead of relying on a default implementation, you use Java to define their behavior 
and determine what they do.

Let's see it with another example. Download now the following Wasm binary:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/host-function.wat.wasm > logger.wasm
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT
//DEPS com.dylibso.chicory:host-module-annotations:999-SNAPSHOT
//DEPS com.dylibso.chicory:host-module-processor:999-SNAPSHOT
```
-->
<!--
```java
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
        System.out.println(message);
        return null;
    });
```

The module calls `console.log` with the length of the string and an index (offset) in its memory. 
This is essentially a pointer into the Wasm's "linear memory".
The `Instance` class provides a reference to a `Memory` object instance, that allows 
to pull a value out of memory. You can use the `readString()` convenience method
to read a buffer into a `String`; we can then print that to stdout on behalf of our Wasm program.

Note that the `HostFunction` needs 3 things:

1. The namespace and function name of the import (in our case it's `console` and `log` respectively)
2. The Wasm type signature (this function takes 2 `i32`s as arguments and returns nothing)
3. A lambda to call when the Wasm module invokes the import


Now we just need to pass this host function when we instantiate the module. We can do so by using a `Store`. 

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

## Host Function Modules

Instead of writing host functions by hand, you can write a class containing annotated methods
and let the Chicory annotation processor generate the host functions for you. This is especially
useful when you have many host functions.

```java
@HostModule("demo")
public final class Demo {
    
    public Demo() {};
    
    @WasmExport
    public long add(int a, int b) {
        return a + b;
    }

    @WasmExport // the Wasm name is random_get
    public void randomGet(Memory memory, int ptr, int len) {
        byte[] data = new byte[len];
        random.nextBytes(data);
        memory.write(ptr, data);
    }

    public HostFunction[] toHostFunctions() {
        return Demo_ModuleFactory.toHostFunctions(this);
    }
}
```

The `@HostModule` annotation marks the class as a host module and specifies the module name for
all the host functions. The `@WasmExport` annotation marks a method as host function and optionally
specifies the name of the function. If the name is not specified, then the Java method name is
converted from camel case to snake case, as is a common convention in Wasm.

The `Demo_ModuleFactory` class in `toHostFunctions()` is generated by the annotation processor.

Host functions must be instance methods of the class. Static methods are not supported.
This is because host functions will typically interact with instance state in the host class.

To use the host module, you need to instantiate the host module and fetch the host functions:

<!--
```java
// bug in JShell: https://github.com/jbangdev/jbang/issues/1854
public class Demo {
    public Demo() {};

    public HostFunction[] toHostFunctions() {
        return new HostFunction[0];
    }
}
```
-->
```java
var demo = new Demo();
var imports = ImportValues.builder().addFunction(demo.toHostFunctions()).build();
```

### Type conversions

The following conversions are supported:

| Java Type         | Wasm Type  |
|-------------------|------------|
| `int`             | `i32`      |
| `long`            | `i64`      |
| `float`           | `f32`      |
| `double`          | `f64`      |

### Enabling the Annotation Processor

In order to use host modules, you need to configure the Java compiler to include the Chicory
`function-processor` as an annotation processor. Exactly how this is done depends on the build
system you are using. This is how to do it with Maven:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessorPaths>
      <path>
        <groupId>com.dylibso.chicory</groupId>
        <artifactId>function-processor</artifactId>
        <version>DOCS_PLACEHOLDER_VERSION</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```
