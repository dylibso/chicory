# WebAssembly System Interface

<!--
```java
//DEPS com.dylibso.chicory:wasi:999-SNAPSHOT
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
```
-->

The WebAssembly System Interface is a suite of host functions that a Wasm module can import
to provide system-level capabilities, such as:

* stdin / stdout / stderr
* environment variables
* command line arguments
* system clock
* random number generation
* basic reading and writing of files (through use of a [virtual file system](https://github.com/google/jimfs))

All such capabilities are virtualized; i.e., the _guest_ will not have direct
access to the corresponding _host_ resources, but they will be mediated by the WASI layer,
which can be configured to limit their surface.

## How to use

As a host who is running Wasm modules, WASI is just a collection of imports that you need to provide
to a wasi-compiled module when instantiating it. You'll also need to configure some options for how
these functions behave and what the module can and cannot do.

### Bare-Bones Instantiation

In order to instantiate a WASI module you need an instance of `WasiPreview1`. 

For instance, download the following example from the link or with curl:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/hello-wasi.wat.wasm > hello-wasi.wasm
```

<!--
```java
docs.FileOps.copyFromWasmCorpus("hello-wasi.wat.wasm", "hello-wasi.wasm");
```
-->

```java
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.runtime.Store;

import java.io.File;

var logger = new SystemLogger();
// let's just use the default options for now
var options = WasiOptions.builder().build();
// create our instance of wasip1
var wasi = new WasiPreview1(logger, WasiOptions.builder().build());
// create the module and connect the host functions
var store = new Store().addFunction(wasi.toHostFunctions());
// instantiate and execute the main entry point
store.instantiate("hello-wasi", Parser.parse(new File("hello-wasi.wasm")));
```

> **Note**: Notice that we don't explicitly execute the module. The module will run when you instantiate it. This
> is part of the WASI spec. A WASI module will implicitly call [`_start`](https://webassembly.github.io/spec/core/syntax/modules.html#start-function). To learn more [read this blog post](https://dylibso.com/blog/wasi-command-reactor/).

### stdin, stdout, and stderr

At the very least, you probably want to orchestrate stdin, stdout, and stderr of the module.
Often, this is the way you communicate with basic WASI-enabled modules by way of the [command pattern](https://dylibso.com/blog/wasi-command-reactor/).
In order to make it easy to manipulate these streams, we expose stdin as an [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)
and stdout/stderr as an [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html).

Download from the link or with curl:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/greet-wasi.rs.wasm > greet-wasi.wasm
```

<!--
```java
docs.FileOps.copyFromWasmCorpus("greet-wasi.rs.wasm", "greet-wasi.wasm");
```
-->

```java
// Let's create a fake stdin stream with the bytes "Andrea"
var fakeStdin = new ByteArrayInputStream("Andrea".getBytes());
// We will create two output streams to capture stdout and stderr
var fakeStdout = new ByteArrayOutputStream();
var fakeStderr = new ByteArrayOutputStream();
// now pass those to our wasi options builder
var wasiOpts = WasiOptions.builder().withStdout(fakeStdout).withStderr(fakeStderr).withStdin(fakeStdin).build();

var wasi = new WasiPreview1(logger, wasiOpts);

// greet-wasi is a rust program that greets the string passed in stdin
var store = new Store().addFunction(wasi.toHostFunctions());
// instantiating will execute the module if it's a WASI command-pattern module
store.instantiate("hello-wasi", Parser.parse(new File("greet-wasi.wasm")));


// check that we output the greeting
assert(fakeStdout.toString().equals("Hello, Andrea!"));
// there should be no bytes in stderr!
assert(fakeStderr.toString().equals(""));
```

<!--
```java
docs.FileOps.writeResult("docs/usage", "wasi.md.result", fakeStdout.toString() + fakeStderr.toString());
```
-->

Notice that it is always possible to connect standard output, standard input and standard error to the system's real streams. 
For instance, in the case of stdout, you would write:

```java
var wasi = WasiOptions.builder().withStdout(System.out).withStderr(System.err).withStdin(System.in).build();
```

## Supported Features


If your module calls a wasi function that we don't support, or uses a feature that we don't support, we will throw a `WasmRuntimeException`.

For the most up-to-date info, and to see what specific functions we support, see the [WasiPreview1.java](https://github.com/dylibso/chicory/blob/main/wasi/src/main/java/com/dylibso/chicory/wasi/WasiPreview1.java) and the following table:


| WASI Function           | Supported  | Notes                                                                     |
|-------------------------|------------|---------------------------------------------------------------------------|
| args_get                | ✅         |                                                                           |
| args_sizes_get          | ✅         |                                                                           |
| clock_res_get           | 🟡         | See `clock_time_get`.                                                     |
| clock_time_get          | 🟡         | Clock IDs `process_cputime_id` and `thread_cputime_id` are not supported. |
| environ_get             | ✅         |                                                                           |
| environ_sizes_get       | ✅         |                                                                           |
| fd_advise               | ✅         |                                                                           |
| fd_allocate             | ✅         |                                                                           |
| fd_close                | ✅         |                                                                           |
| fd_datasync             | ✅         |                                                                           |
| fd_fdstat_get           | ✅         |                                                                           |
| fd_fdstat_set_flags     | ✅         |                                                                           |
| fd_fdstat_set_rights    | ❌         |                                                                           |
| fd_filestat_get         | ✅         |                                                                           |
| fd_filestat_set_size    | ✅         |                                                                           |
| fd_filestat_set_times   | ✅         |                                                                           |
| fd_pread                | ✅         |                                                                           |
| fd_prestat_dir_name     | ✅         |                                                                           |
| fd_prestat_get          | ✅         |                                                                           |
| fd_pwrite               | 🟡         | Not supported for files opened in append mode.                            |
| fd_read                 | ✅         |                                                                           |
| fd_readdir              | ✅         |                                                                           |
| fd_renumber             | ✅         |                                                                           |
| fd_seek                 | ✅         |                                                                           |
| fd_sync                 | ✅         |                                                                           |
| fd_tell                 | ✅         |                                                                           |
| fd_write                | ✅         |                                                                           |
| path_create_directory   | ✅         |                                                                           |
| path_filestat_get       | ✅         |                                                                           |
| path_filestat_set_times | ✅         |                                                                           |
| path_link               | ❌         |                                                                           |
| path_open               | ✅         |                                                                           |
| path_readlink           | ❌         |                                                                           |
| path_remove_directory   | ✅         |                                                                           |
| path_rename             | ✅         |                                                                           |
| path_symlink            | ❌         |                                                                           |
| path_unlink_file        | ✅         |                                                                           |
| poll_oneoff             | ❌         |                                                                           |
| proc_exit               | ✅         |                                                                           |
| proc_raise              | 💀         | This function is no longer part of WASI.                                  |
| random_get              | ✅         |                                                                           |
| sched_yield             | ✅         |                                                                           |
| sock_accept             | ❌         |                                                                           |
| sock_recv               | ❌         |                                                                           |
| sock_send               | ❌         |                                                                           |
| sock_shutdown           | ✅         |                                                                           |
