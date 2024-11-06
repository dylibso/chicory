# Chicory

<p align="center">
  <picture>
    <img width="200" src="chicory1.png">
  </picture>
  <br>
  <a href="https://chicory.dev/">Website</a> |
  <a href="https://chicory.dev/docs/#getting-started">Getting started</a> |
  <a href="https://chicory.dev/blog">Blog</a>
  <a href="/CONTRIBUTING.md">Blog</a>
</p>


[![Interpreter Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-interpreter.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-interpreter.svg)
[![AOT Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-aot.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-aot.svg)
[![WASI Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-wasi.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-wasi.svg)

[![Zulip](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://chicory.zulipchat.com/join/g4gqsxoys6orfxlrk6hn4cyp/)

Chicory is a JVM native WebAssembly runtime. It allows you to run WebAssembly programs with
zero native dependencies or JNI. Chicory can run Wasm anywhere that the JVM can go. It is designed with
simplicity and safety in mind. See the [development section](#development) for a better idea of what we are trying to achieve and why.

> *Reach out to us*: let us know what you are building with Chicory.
> [Join our team Zulip chat with this invite link](https://chicory.zulipchat.com/join/g4gqsxoys6orfxlrk6hn4cyp/).

Get started now with the [official documentation](https://chicory.dev/docs/)

## Why?

There are a number of mature Wasm runtimes to choose from to execute a Wasm module.
To name a few [v8](https://v8.dev/), [wasmtime](https://wasmtime.dev/), [wasmer](https://wasmer.io/), [wasmedge](https://wasmedge.org/), [wazero](https://wazero.io/) etc.

Although these can be great choices for running a Wasm application, embedding them into your existing
Java application has some downsides. Because these runtimes are written in C/C++/Rust/etc, they must be distributed
and run as native code. This causes two main friction points:

### 1. Distribution

If you're distributing a Java library (jar, war, etc), you must now distribute along with it a native object targeting the correct
architecture and operating system. This matrix can become quite large. This eliminates a lot of the simplicity and original benefit of shipping Java code.

### 2. Runtime

At runtime, you must use FFI to execute the module. While there might be performance benefits to doing this for some modules,
when you do, you're effectively escaping the safety and observability of the JVM. Having a pure JVM runtime means all your
security and memory guarantees, and your tools, can stay in place.

## Goals

* Be as safe as possible
  * In that we are willing to sacrifice things like performance for safety and simplicity
* Make it easy to run Wasm in any JVM environment without native code, including very restrictive environments.
* Fully support the core Wasm spec
* Make integration with Java (and other host languages) easy and idiomatic.

## Non-Goals:

* Be a standalone runtime
* Be the fastest runtime
* Be the right choice for every JVM project

## Roadmap

Chicory development was started in September 2023. The following are the milestones we're aiming for. These
are subject to change but represent our best guesses with current information. These are not necessarily sequential
and some may be happening in parallel. Unless specified, any unchecked box is still not planned or started.
If you have an interest in working on any of these please reach out in Zulip!

### 2023

* [x] Wasm binary parser
* [x] Simple bytecode interpreter
* [x] Establish basic coding and testing patterns
* [x] Generate JUnit tests from wasm test suite

### 2024

* [x] Make all tests green with the interpreter (important for correctness)
* [x] Implement validation logic (important for safety)
* [x] Draft of the v1.0 API (important for stability and dx)
* [x] Decouple interpreter and create separate compiler and interpreter "engines"
* [x] Proof of concept AOT compiler (run some subset of modules)
* [x] AOT engine passes all the same specs as interpreter (stretch goal)
* [ ] Off-heap linear memory (stretch goal)

### Proposals and Specs

* [x] WASIp1 Support (including test gen)
  * [Read more details in the documentation](https://chicory.dev/docs/usage/wasi/)
* [ ] SIMD Support
  * Started
* [ ] Tail Call
* [ ] Exception Handling
* [ ] Multi-Memory Support
* [ ] GC Support
* [ ] Threads Support

## On the press

- [Chicory: A Zero Dependency Wasm Runtime for the JVM](https://www.javaadvent.com/2023/12/chicory-wasm-jvm.html) on [Java Advent 2023](https://www.javaadvent.com/2023/12)
- [Chicory - a WebAssembly Interpreter Written Purely in Java with Zero Native Dependencies](https://www.infoq.com/news/2024/05/chicory-wasm-java-interpreter/) on [InfoQ](https://www.infoq.com)
- [Chicory: Write to WebAssembly, Overcome JVM Shortcomings](https://thenewstack.io/chicory-write-to-webassembly-overcome-jvm-shortcomings/) on [The New Stack](https://thenewstack.io)
- [Meet Chicory, exploit the power of WebAssembly on the server side! by Andrea Peruffo](https://www.youtube.com/watch?v=7a1yrDSh9rA) (Devoxx BE 2024)
- [WebAssembly, the Safer Alternative to Integrating Native Code in Java](https://www.infoq.com/articles/sqlite-java-integration-webassembly/) on [InfoQ](https://www.infoq.com)
- [Chicory: Creating a Language-Native Wasm Runtime by Benjamin Eckel / Andrea Peruffo](https://www.youtube.com/watch?v=00LYdZS0YlI) (Wasm I/O 2024)

## Prior Art

* [asmble](https://github.com/cretz/asmble)
* [kwasm](https://github.com/jasonwyatt/KWasm)
* [wazero](https://wazero.io/)
