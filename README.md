# Chicory Runtime

Chicory is a JVM native WebAssembly runtime.
It allows you to run WebAssembly programs with no native dependencies or JNI.

## Goals

* Be as safe as possible
  * This line is pretty relative
  * It depends on the resources we have to verify security
* Make it easy to run Wasm in any JVM environment without native code, including very restrictive environments.
* Fully support the core Wasm 1.0 and 2.0 spec specs
* Make integration with Java (and other host languages) easy and idiomatic.

## Non-Goals:

* Be a standalone runtime
* Be the fastest runtime
* Be the right choice for every JVM project

## Nice-to-Haves:

* WASI support
    * We may develop this outside of this library
    * Which version we target may depend on when we achieve the goals
* Support JITing to JVM bytecode
    * Only if it can be done safely
* Demonstrate performance benefits for at least some subset of use cases
* Support some future standards like Threads, GC, and Component Model


