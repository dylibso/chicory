---
sidebar_position: 130
sidebar_label: Build time Compilation
title: Build Time Compilation
---
## Overview

The build time compiler backend is a drop-in replacement for the interpreter, and it passes 100% of the same 
spec tests that the interpreter already supports.

This compiler translates the WASM instructions to Java bytecode and stores them as `.class` files
that you package in your application.  The resulting code is usually expected to evaluate (much) faster and 
consume less memory than if it was interpreted.

The build time compiler has several advantages over the [Runtime Compiler](runtime-compiler.md) such as: 

- improved instance initialization time: the translation occurs at build time
- no reflection needed: easier to use with `native-image`
- fewer runtime dependencies: asm is only needed at build time
- distribute Wasm modules as self-contained jars: making it a convenient way to distribute software that was not originally meant to run on the Java platform

You can use the compiler at build-time via Maven plug-in, Gradle plug-in, or plain CLI

### Interpreter Fall Back

The WASM to bytecode compiler translates each WASM function into JVM method.  Occasionally you will find WASM module where functions are bigger than the maximum method size allowed by the JVM.  In these rare cases, we fall back to executing these large functions in the interpreter.  

Since interpreted functions have worse performance, we want to make sure you are aware this is happening so the build time compiler will FAIL if it finds any functions that are too large.  The build tool will produce a message that contains text like:

```text
WASM function size exceeds the Java method size limits and cannot be compiled to Java bytecode. It can only be run in the interpreter. Either reduce the size of the function or enable the interpreter fallback mode: WASM function index: 3938
```

If this happens you can configure your build tool, to just issue warning messages, or to be silent.  Another way to silence the message is to configure the build too with an explicit list of functions that should be interpreted. 

## Using Maven

Example configuration of the Maven plug-in:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.dylibso.chicory</groupId>
      <artifactId>chicory-compiler-maven-plugin</artifactId>
      <executions>
        <execution>
          <id>compiler-gen</id>
          <goals>
            <goal>chicory-compiler-gen</goal>
          </goals>
          <configuration>
            <!-- Translate the Wasm binary `add` into bytecode -->
            <wasmFile>src/main/resources/add.wasm</wasmFile>
            <!-- Name of the generated class to be used -->
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
class Add {

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
var module = Add.load();

// instantiate the module with the pre-compiled code
var instance = Instance.builder(module).
        withMachineFactory(Add::create).
        build();
```
### wasm-aot-gen Goal Configuration Parameters

<table>
<thead>
<tr>
    <th>Name</th>
    <th>Type</th>
    <th>Description</th>
</tr>
</thead>
<tbody>
<tr>
    <td>wasmFile</td>
    <td>`String`</td>
    <td>Path to the Wasm binary file to translate into Java bytecode.</td>
</tr><tr>
    <td>name</td>
    <td>`String`</td>
    <td>The name for the generated class that you will use .</td>
</tr><tr>
    <td>interpreterFallback</td>
    <td>`boolean`</td>
    <td>
        The action to take if the compiler needs to use the interpreter because a function is too big.
        <br/>**options:** SILENT | WARN | FAIL 
        <br/>**default:** <code>FAIL</code>.
    </td>
</tr><tr>
    <td>interpretedFunctions</td>
    <td>`List<String>`</td>
    <td>
        A list of functions that should be interpreted.  Example usage:
```xml
<configuration>
    ...
    <interpretedFunctions>
        <function>3938</function>
        <function>4116</function>
    </interpretedFunctions>
    ...
</configuration>
```
    </td>
</tr><tr>
    <td>targetClassFolder</td>
    <td>`String`</td>
    <td>The target folder to generate classes. 
    <br/>**default** `${project.build.directory}/generated-resources/chicory-aot`.</td>
</tr><tr>
    <td>targetSourceFolder</td>
    <td>`String`</td>
    <td>The target source folder to generate the Machine implementation. 
    <br/>**default:** `${project.build.director}/generated-sources/chicory-aot`.</td>
</tr><tr>
    <td>targetWasmFolder</td>
    <td>`String`</td>
    <td>The target wasm folder to generate the stripped meta wasm module. 
    <br/>**default:** `${project.build.directory}/generated-resources/chicory-aot`.</td>
</tr>
</tbody>
</table>

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
                <source>${project.build.directory}/generated-sources/chicory-compiler</source>
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

This generates the class `org.acme.wasm.Add`, which you can use to instantiate the module just like shown earlier
in the Maven example.

<!--
```java
docs.FileOps.writeResult("docs/usage", "build-time-compiler.md.result", "empty");
```
-->
