# WASI

This library contains code for instantiating and running WASI modules.
[WASI](https://wasi.dev/) is a virtual system interface that can give you some familiar posix-like syscalls and is supported
by many of the compilers out there.

## Version Support

There are currently 2 versions of WASI at the moment, [preview1](https://github.com/WebAssembly/WASI/blob/main/legacy/README.md) and [preview2](https://github.com/WebAssembly/WASI/blob/main/preview2/README.md). This library is currently
aimed at `preview1`.

> **Note**: You might hear the terms `preview1/preview1` or `0.1/0.2` when referring to the versions inside the WASI docs.
> And you might hear `wasip1` when being used as a flag to a compiler target. We tend to prefer the nomenclature wasip1 / wasip2.

### wasip1

Although `wasip1` is marked as "legacy", this is the version that nearly all compilers support when compiling to a Wasm target. For that reason we are aiming to have good support, but have no immediate plans to "complete" the implementation.
Please reach out if you'd like to see more done to support wasip1.

See the [WasiPreview1.java](https://github.com/dylibso/chicory/blob/main/wasi/src/main/java/com/dylibso/chicory/wasi/WasiPreview1.java) class
to understand what we support today and what we do not support. Here are some features have basic coverage for currently:

* stdin / stdout / stderr
* environment variables
* command arguments
* clocks
* random
* basic reading and writing of files (through use of a [virtual file system](https://github.com/google/jimfs))

If your module calls a wasi function that we don't support, or uses a feature that we don't support, we will throw a `WASMRuntimeException`.

### wasip2

We do have intentions to support wasip2 in the future, however this work has not been started. Please reach out to us on zulip if you are interested in helping plan and execute this work.

## How to use

As a host who is running Wasm modules, WASI is just a collection of host imports that you need to provide
to a wasi-compiled module when instantiating it. You'll also need to configure some options for how
these functions behave and what the module can and cannot do.

### Bare-Bones Instantiation

So to instantiate a WASI module you need an instance of `WasiPreview1`. You can turn this instance into
import functions which can then be passed to the Module builder. 

```java
var logger = new SystemLogger();
// let's just use the default options for now
var options = WasiOptions.builder().build();
// create our instance of wasip1
var wasi = new WasiPreview1(this.logger, WasiOptions.builder().build());
// turn those into host imports. Here we could add any other custom imports we have
var imports = new HostImports(wasi.toHostFunctions());
// create the module
var module = Module.builder("hello-wasi.wasm").build();
// instantiate and connect our imports, this will execute the module
module.instantiate(imports);
```

> **Note**: Take note that we don't explicitly execute the module. The module will run when you instantiate it. This
> is part of the WASI spec. They will implicitly call `_start`. To learn more [read this blog post](https://dylibso.com/blog/wasi-command-reactor/).

### stdin, stdout, and stderr

At the very least, you probably want to orchestrate stdin, stdout, and stderr of the module.
Often, this is the way you communicate with basic WASI-enabled modules by way of the [command pattern](https://dylibso.com/blog/wasi-command-reactor/).
In order to make it easy to manipulate these streams, we expose stdin as an [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)
and stdout/stderr as an [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html).

```java
// Let's create a fake stdin stream with the bytes "Andrea"
var fakeStdin = new ByteArrayInputStream("Andrea".getBytes());
// We will create two output streams to capture stdout and stderr
var fakeStdout = new ByteArrayOutputStream();
var fakeStderr = new ByteArrayOutputStream();
// now pass those to our wasi options builder
var wasiOpts = WasiOptions
        .builder()
        .withStdout(fakeStdout)
        .withStderr(fakeStderr)
        .withStdin(fakeStdin)
        .build();

var wasi = new WasiPreview1(this.logger, wasiOpts);
var imports = new HostImports(wasi.toHostFunctions());

// greet-wasi is a rust program that greets the string passed in stdin
var module = Module.builder("greet-wasi.rs.wasm").build();

// instantiating will execute the module
module.instantiate(imports);

// check that we output the greeting
assertEquals(fakeStdout.toString(), "Hello, Andrea!");
// there should be no bytes in stderr!
assertEquals(fakeStderr.toString(), "");
```







