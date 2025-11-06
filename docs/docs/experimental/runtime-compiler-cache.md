---
sidebar_position: 6
sidebar_label: Runtime Compiler Cache
title: Runtime Compiler Cache
---

# Overview of the Runtime Compiler Cache

The runtime compiler cache lets the Chicory runtime compiler store the results of compiling WASM modules to Java bytecode. Subsequent executions can skip compilation and start faster.

Use the experimental directory-based cache, or implement the simple `com.dylibso.chicory.compiler.Cache` interface:

```java
public interface Cache {
    byte[] get(String key) throws IOException;
    void putIfAbsent(String key, byte[] data) throws IOException;
}
```

The compiler uses the module digest (default SHA-256) as the cache key. For example, `"sha-256:KRgyTkCm43c34ksqtA8gmdDw4YCfquC2G0qfIFCpb+w="` could be a key. The cached value is a JAR containing the module's compiled bytecode.

## The Directory Cache

The directory cache stores entries as files under a configured directory.   For example, 
ff the cache directory is `/cache`, and you store the following cache key: 

`sha-256:KRgyTkCm43c34ksqtA8gmdDw4YCfquC2G0qfIFCpb+w=`
 
then that will create the following file: 

`/cache/sha-256/kr/gytkcm43c34ksqta8gmddw4ycfquc2g0qfifcpb-w.jar` 

It transforms the key to:
* translate characters to be file-system-friendly
* use a two-character subdirectory prefix to avoid directory scaling issues

The implementation uses file system atomic moves (write to a temp file, then move to the final location). This makes the cache thread safe and safe to share across processes and avoids partial-write failures. Temp files are written under `/cache/.tmp`. Partially written temp files may be left after a crash; there is no automatic cleanup or eviction. The cache size is not limited—delete files manually to free disk space.

### Using the Directory Cache

We assume you already use the runtime compiler. If not, see the [Runtime Compiler](../usage/runtime-compiler) guide first.

### Add the Maven Dependency

Add the following dependency:

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>dircache-experimental</artifactId>
</dependency>
```

### Code

Create the cache:

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:compiler:999-SNAPSHOT
//DEPS com.dylibso.chicory:dircache-experimental:999-SNAPSHOT
```
-->
```java
import java.nio.file.Path;
import com.dylibso.chicory.experimental.dircache.DirectoryCache;

var cache = new DirectoryCache(Path.of("cache"));
```

Configure the compiler to use the cache via `MachineFactoryCompiler.builder(...)`:

<!--
```java
import java.io.File;
import java.nio.file.Files;
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.runtime.Instance;
docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "your.wasm");

var cache = new DirectoryCache(Files.createTempDirectory("cache"));
```
-->

```java
var module = Parser.parse(new File("your.wasm"));
var instance = Instance.builder(module).
        withMachineFactory(
            MachineFactoryCompiler.builder(module).withCache(cache).compile()
        ).
        build();
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
docs.FileOps.writeResult("docs/experimental", "runtime-compiler-cache.md.result", "empty");
```
-->
