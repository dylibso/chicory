---
sidebar_position: 3
sidebar_label: AOT compilation
title: AOT compilation
---
## Runtime AOT

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:aot-experimental:999-SNAPSHOT

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;

docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "your.wasm");
```
-->

The Ahead-of-Time compiler backend is a drop-in replacement for the interpreter, and it passes 100% of the same
spec tests that the interpreter already supports.

You can instantiate a module using the AoT by explicitly providing a `MachineFactory`.
The default `Machine` implementation is the `InterpreterMachine`.

You can opt in to the AoT mode by writing:

```java
import com.dylibso.chicory.experimental.aot.AotMachine;

var module = Parser.parse(new File("your.wasm"));
var instance = Instance.builder(module).withMachineFactory(AotMachine::new).build();
```

after you add the dependency:

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>aot-experimental</artifactId>
</dependency>
```

This will translate every module you instantiate into Java bytecode on-the-fly and in-memory.
The resulting code is usually expected to evaluate (much)faster and consume less memory.

Please note that compiling and executing AoT modules at runtime requires:
- an external dependency on [ASM](https://asm.ow2.io/)
- the usage of runtime reflection

This is usually fine when running on a standard JVM, but it involve some additional configuration when using tools like `native-image`.

## Pre-compiled AOT

You can use the AOT compiler at build-time, by leveraging a [Maven plug-in][aot-maven-plugin] to overcome the usage of reflection and external dependencies of the "Runtime AOT".

This mode of execution reduces startup time and will remove the need for distributing
the original Wasm binary.

Key advantages are:

- improved startup time because the translation occurs only once, when you are packaging your application
- distribute Wasm modules as self-contained jars, making it a convenient way to distribute software that was not originally meant to run on the Java platform
- same performance properties as the in-memory compiler (in fact, the compilation backend is the same)

Example configuration of the Maven plug-in:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.dylibso.chicory</groupId>
      <artifactId>aot-maven-plugin-experimental</artifactId>
      <executions>
        <execution>
          <id>aot-gen</id>
          <goals>
            <goal>wasm-aot-gen</goal>
          </goals>
          <configuration>
            <!-- Translate the Wasm binary `wat2wasm` into bytecode -->
            <wasmFile>src/main/resources/add.wasm</wasmFile>
            <!-- Generate the following class file as a result -->
            <name>org.acme.wasm.AddModule</name>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

In the codebase you can use the generated module by configuring appropriately the `MachineFactory`:

<!--
```java
class AddModule {
    public static Machine create(Instance instance) {
        return null;
    }
}
```
-->

```java
var module = Parser.parse(new File("your.wasm"));
var instance = Instance.builder(module).withMachineFactory(AddModule::create).build();
```

#### IDE shortcomings

In some IDEs the sources generated under the standard folder `target/generated-sources` are not automatically recognized.
To overcome this limitation you can use an additional Maven Plugin for a smoother IDE experience:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <executions>
        <execution>
        <id>addSource</id>
        <phase>generate-sources</phase>
        <goals>
            <goal>add-source</goal>
        </goals>
        <configuration>
            <sources>
            <source>${project.basedir}/target/generated-sources/chicory-aot</source>
            </sources>
        </configuration>
        </execution>
    </executions>
</plugin>
```

<!--
```java
docs.FileOps.writeResult("docs/experimental", "3-aot.md.result", "empty");
```
-->
