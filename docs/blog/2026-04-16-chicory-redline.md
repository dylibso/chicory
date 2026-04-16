---
slug: chicory-redline
title: 'Introducing Chicory Redline: Native Speed for Wasm on the JVM'
authors: [andreaTP]
tags: [wasm, chicory, redline]
---

![Chicory Redline](chicory-redline.png)

[Chicory Redline](https://github.com/roastedroot/chicory-redline) brings native speed Wasm execution to the JVM by using Cranelift behind the curtains.

<!-- truncate -->

Chicory has always been about running WebAssembly on the JVM with zero native dependencies. Its build-time compiler turns Wasm into JVM bytecode, and that works remarkably well. But for some workloads, "remarkably well" isn't enough. Cryptography, parsing, data processing: these are the cases where you look at the numbers and think, *we're leaving performance on the table*.

That's how Chicory Redline started with a simple question: *what if we could get native compiler performance without giving up what makes Chicory special?* Specifically: zero native dependencies, pure Java, runs everywhere.

## The Problem

Writing a Wasm-to-native compiler from scratch is a multi-year effort. [Cranelift](https://cranelift.dev/) exists and is battle-tested (it powers Wasmtime), but the obvious integration path is JNI bindings to a native Wasmtime library. That means shipping platform-specific binaries, managing native build toolchains, and losing the "just add a dependency" simplicity that Chicory users expect.

We didn't want to maintain a full-fledged Wasm-to-assembly compiler. We also didn't want to become a JNI wrapper around someone else's runtime. We wanted something better: a level of interoperability that JNI bindings can't offer, where the compiler itself runs inside the JVM and the generated code operates on off-heap Java-managed memory.

## The Idea

Cranelift is written in Rust, and Rust compiles to Wasm. So instead of linking Cranelift as a native library through JNI, we compiled it to a `.wasm` module and run it through Chicory itself at build time. Cranelift-as-Wasm reads your Wasm functions and emits native machine code. That code gets bundled into your JAR as a resource. At runtime, we mmap it and call it through Panama FFM (Java 25+) or jffi (Java 11+).

No native compiler to ship. No JNI. On Java 25+ (Panama FFM), the entire stack has **zero runtime dependencies**: just your JAR and the JDK. On Java 11+, the only addition is `jffi` with broad compatibility at ~200KB.

```
Build time:

  your_module.wasm ──> Chicory runs cranelift_bridge.wasm ──> x86_64/aarch64 machine code
                                                                  │
                                                                  v
                                                          bundled in JAR as resources

Runtime:

  JAR resource ──> mmap (RX) ──> Panama downcall / jffi invoke ──> native execution
```

## What We Tried (and What We Threw Away)

This wasn't a straight line.

We initially built a hybrid dispatch system that could run some functions natively and fall back to Chicory bytecode for others. It was clever, and benchmarks looked promising. But cross-boundary dispatch introduced subtle bugs that weren't worth the complexity. We ended up removing it entirely: compile everything or compile nothing.

Testing against real projects like SQLite and Prism proved invaluable. Each one exercised patterns that spec tests alone don't cover: memory.grow under call stacks, multi-threaded instance reuse, Windows-specific memory management. These are exactly the kind of bugs you want to find early, and we're looking for more projects to test against. If you have a Wasm workload you'd like to try, we'd love to hear about it.

## What Works

Redline passes the a good chunk WebAssembly spec test suite: about 28,000 tests across both the Panama and jffi backends.

Real-world validation on actual projects:

| Project | Wasm Size | vs Chicory AOT |
|---|---|---|
| Prism (Ruby parser) | 564 KB | **~10x faster** |
| SQLite | 849 KB | **~4x faster** |
| toml2json | 237 KB | **~6.5x faster** |

Prism has historically been our fellow partner in shipping early new versions, and the Redline integration is [already upstream](https://github.com/ruby/prism/pull/4071) and released, it will power the next version of JRuby with that 10x parsing improvement speed.

The [shootout benchmarks](https://github.com/bytecodealliance/wasm-score/tree/main/benchmarks/shootout) show the full range, from modest 1.5x improvements on memory-bound workloads to 22x on compute-heavy ones like Keccak. Some benchmarks (like fib2) show no improvement because the JVM's JIT is already excellent at simple integer loops. That's expected. Redline shines on workloads where the Wasm-to-bytecode translation introduces overhead that the JIT can't fully optimize away.

Cross-compilation works: a single build produces native code for all six supported targets (Linux/macOS/Windows on x86_64/aarch64), and the right one is selected at runtime. On unsupported platforms, Chicory's bytecode compiler takes over transparently.

## What Doesn't (Yet)

Redline supports the base Wasm specification and the Threads proposal. Some newer proposals like Exception Handling and GC are not yet implemented. We're looking to bridge the gap going forward, prioritizing based on early adopters' requests.

On the safety side, Cranelift is a mature, security-focused compiler: it generates bounds-checked assembly for every memory access, and code pages are mapped read-execute only. Because Redline compiles to native code rather than JVM bytecode, you get the full benefit of Cranelift's hardened code generation while enjoying near-native performance.

## Quick Start

Add the dependency and Maven plugin to your `pom.xml`:

```xml
<dependency>
    <groupId>io.roastedroot</groupId>
    <artifactId>redline</artifactId>
    <version>0.0.1</version>
</dependency>
```

```xml
<plugin>
    <groupId>io.roastedroot</groupId>
    <artifactId>redline-compiler-maven-plugin</artifactId>
    <version>0.0.1</version>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <name>com.example.MyModule</name>
                <wasmFile>src/main/resources/my-module.wasm</wasmFile>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The plugin compiles your Wasm module to native code at build time. Use the generated class at runtime:

```java
try (var instance = MyModule.builder().build()) {
    var result = instance.instance().export("my_function").apply();
    System.out.println(result[0]);
}
```

That's it. No JNI, no native libraries to install. On Java 25+ the Panama backend kicks in with zero dependencies; on Java 11+ the `jffi` backend is automatically selected instead. If native code isn't available for the current platform, Chicory's produced Java bytecode takes over.

Check which backend you're running on:

```java
if (instance.isNative()) {
    // Cranelift native code
} else {
    // Chicory bytecode fallback
}
```

## Acknowledgements

None of this would exist without [Cranelift](https://cranelift.dev/) and the Bytecode Alliance. We're standing on the shoulders of a production-grade compiler and found a nifty way to use it from Java. And of course [Chicory](https://github.com/dylibso/chicory), which not only provides the bytecode fallback but is literally the engine that runs Cranelift itself during compilation.

Redline is experimental. The API will change. But `0.0.1` is out, it passes 28k tests, and it makes some workloads measurably faster. Give it a try.

[GitHub](https://github.com/roastedroot/chicory-redline) | [CONTRIBUTING](https://github.com/roastedroot/chicory-redline/blob/main/CONTRIBUTING.md) | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
