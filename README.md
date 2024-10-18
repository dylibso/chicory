# Chicory Runtime

[![Interpreter Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-interpreter.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-interpreter.svg)
[![AOT Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-aot.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-aot.svg)
[![WASI Test Results](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-wasi.svg)](https://gist.githubusercontent.com/andreaTP/69354d1cc6cf23e4c3c4a9a8daf7ea15/raw/badge-wasi.svg)

[![Zulip](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://chicory.zulipchat.com/join/g4gqsxoys6orfxlrk6hn4cyp/)

<img align="right" width="200" src="chicory1.png">

Chicory is a JVM native WebAssembly runtime. It allows you to run WebAssembly programs with
zero native dependencies or JNI. Chicory can run Wasm anywhere that the JVM can go. It is designed with
simplicity and safety in mind. See the [development section](#development) for a better idea of what we are trying to achieve and why.

> *Reach out to us*: Chicory is very early in development and there will be rough edges. We're hoping to talk to some early adopters and contributors before we formally announce it a beta to the world. Please [join our team Zulip chat with this invite link](https://chicory.zulipchat.com/join/g4gqsxoys6orfxlrk6hn4cyp/) if you're interested in providing feedback or contributing. Or just keeping up with development.

## Getting Started (as a user)

To get started visit the [official documentation](https://chicory.dev/docs/).

## Development

### Why is this needed?

If you'd prefer to watch a video instead of reading, see our [2024 Wasm I/O](https://2024.wasmio.tech/) on the subject:

[![Wasm I/O Chicory talk](https://img.youtube.com/vi/00LYdZS0YlI/0.jpg)](https://www.youtube.com/watch?v=00LYdZS0YlI)

There are a number of mature Wasm runtimes to choose from to execute a Wasm module.
To name a few [v8](https://v8.dev/), [wasmtime](https://wasmtime.dev/), [wasmer](https://wasmer.io/), [wasmedge](https://wasmedge.org/), etc.

Although these can be great choices for running a Wasm application, embedding them into your existing
Java application has some downsides. Because these runtimes are written in C/C++/Rust/etc, they must be distributed
and run as native code. This causes two main friction points:

#### 1. Distribution

If you're distributing a Java library (jar, war, etc), you must now distribute along with it a native object targeting the correct
architecture and operating system. This matrix can become quite large. This eliminates a lot of the simplicity and original benefit of shipping Java code.

#### 2. Runtime

At runtime, you must use FFI to execute the module. While there might be performance benefits to doing this for some modules,
when you do, you're effectively escaping the safety and observability of the JVM. Having a pure JVM runtime means all your
security and memory guarantees, and your tools, can stay in place.

### Goals

* Be as safe as possible
  * In that we are willing to sacrifice things like performance for safety and simplicity
* Make it easy to run Wasm in any JVM environment without native code, including very restrictive environments.
* Fully support the core Wasm spec
* Make integration with Java (and other host languages) easy and idiomatic.

### Non-Goals:

* Be a standalone runtime
* Be the fastest runtime
* Be the right choice for every JVM project

### Roadmap

Chicory development was started in September, 2023. The following are the milestones we're aiming for. These
are subject to change but represent our best guesses with current information. These are not necessarily sequential
and some may be happening in parallel. Unless specified, any unchecked box is still not planned or started.
If you have an interest in working on any of these please reach out in Zulip!

#### Bootstrap a bytecode interpreter and test suite (EOY 2023)

* [x] Wasm binary parser [link](wasm/)
* [x] Simple bytecode interpreter
* [x] Establish basic coding and testing patterns
* [x] Generate JUnit tests from wasm test suite [link](test-gen-plugin/)

#### Make the interpreter production ready (Summer 2024)

* [x] Make all tests green with the interpreter (important for correctness)
* [ ] Implement validation logic (important for safety)
  * Nearing completion
* [ ] Draft of the v1.0 API (important for stability and dx)

#### Make it fast (EOY 2024)

The primary goal here is to create an AOT compiler that generates JVM bytecode
as interpreting bytecode can only be so fast.

* [x] Decouple interpreter and create separate compiler and interpreter "engines"
* [x] Proof of concept AOT compiler (run some subset of modules)
* [ ] AOT engine passes all the same specs as interpreter (stretch goal)
  * In Progress
* [ ] Off-heap linear memory (stretch goal)

#### Make it compatible (EOY 2024)

* [ ] WASIp1 Support (including test gen)
  * We have [partial support for wasip1](wasi/) and [test generation](wasi-test-gen-plugin/)
* [ ] SIMD Support
  * Started
* [ ] Multi-Memory Support
* [ ] GC Support
* [ ] Threads Support
* [ ] Component Model Support

### Prior Art

* [asmble](https://github.com/cretz/asmble)
* [kwasm](https://github.com/jasonwyatt/KWasm)
* [wazero](https://wazero.io/)

### Building the Runtime

Contributors and other advanced users may want to build the runtime from source. To do so, you'll need to have Maven installed.
`Java version 11+` required for a proper build. You can download and install [Java 11 Temurin](https://adoptium.net/temurin/releases/?version=11)

Basic steps:

* `mvn clean install` to run all of the project's tests and install the library in your local repo
* `mvn -Dquickly` to install the library skipping all tests
* `mvn -Ddev <...goals>` to disable linters and enforcers during development
* `mvn spotless:apply` to autoformat the code
* `./scripts/compile-resources.sh` will recompile and regenerate the `resources/compiled` folders

NOTE: The `install` target relies on the `wabt` library to compile the test suite. This is not currently released for ARM (e.g. new Macs with Apple Silicon). However, `wabt` is available from Homebrew, so `brew install wabt` before running `mvn clean install` should work.
