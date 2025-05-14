---
sidebar_position: 4
sidebar_label: AoT Compilation
title: AoT Compilation
---
## Overview

The Ahead-of-Time (AoT) compiler backend is a drop-in replacement for the interpreter, and it passes 100% of the same 
spec tests that the interpreter already supports.

This AoT compiler translates the WASM instructions to Java bytecode and stores them as `.class` files
that you package in your application.  The resulting code is usually expected to evaluate (much) faster and 
consume less memory than if it was interpreted.

The AoT compiler has several advantages over the [Runtime Compiler](runtime-compiler.md) such as: 

- improved instance initialization time: the translation occurs at build time
- no reflection needed: easier to use with `native-image`
- fewer runtime dependencies: asm is only needed at build time
- distribute Wasm modules as self-contained jars: making it a convenient way to distribute software that was not originally meant to run on the Java platform

You can use the AoT compiler at build-time via Maven plug-in, Gradle plug-in, or plain CLI

## Using Maven

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
            <!-- Translate the Wasm binary `add` into bytecode -->
            <wasmFile>src/main/resources/add.wasm</wasmFile>
            <!-- Generate classes under the following prefix -->
            <name>org.acme.wasm.Add</name>
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
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.InterpreterMachine;

docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "your.wasm");

// mocking up the generated code
class AddModule {

    public static WasmModule load() {
      return Parser.parse(new File("your.wasm"));
    }

    public static Machine create(Instance instance) {
        return new InterpreterMachine(instance);
    }
}
```
-->

```java
import com.dylibso.chicory.runtime.Instance;

// load the bundled module
var module = AddModule.load();

// instantiate the module with the pre-compiled code
var instance = Instance.builder(module).
        withMachineFactory(AddModule::create).
        build();
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
                <source>${project.build.directory}/generated-sources/chicory-aot</source>
            </sources>
        </configuration>
        </execution>
    </executions>
</plugin>
```

### Using Gradle [community]

Gradle users can leverage the [wasm2class-gradle-plugin](https://github.com/illarionov/wasm2class-gradle-plugin),
a third-party plugin that serves as an alternative to the Maven plugin, running the AoT compiler at build time
and enabling the use of pre-compiled Wasm code in Java, Kotlin, and Android projects.

To set it up, make sure MavenCentral is listed as a repository in the `pluginManagement` block of your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Configuration example in the `build.gradle.kts` file for the module:

```kotlin
plugins {
    id("at.released.wasm2class.plugin") version "<latest version>"
}

wasm2class {
    modules {
        // Target package for the generated classes
        targetPackage = "org.acme.wasm"
        //  Use "Add" as the base name for generated classes
        create("Add") {
            // Translate `add.wasm` into bytecode
            wasm = file("src/main/resources/add.wasm")
        }
    }
}
```

This generates the class `org.acme.wasm.AddModule`, which you can use to instantiate the module just like shown earlier
in the Maven example.

### Using CLI

Coming soon.

<!--
```java
docs.FileOps.writeResult("docs/experimental", "aot-compiler.md.result", "empty");
```
-->
