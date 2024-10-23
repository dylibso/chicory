# Using Memory to Share Data

Wasm only understands basic integer and float primitives. So passing more complex types across the boundary involves
passing pointers. To read, write, or allocate memory in a module, Chicory provides the `Memory` class. Let's look at an
example where we have a module `count_vowels.wasm`, written in Rust, that takes a string input and counts the number of vowels
in the string:

```bash
curl https://raw.githubusercontent.com/dylibso/chicory/main/wasm-corpus/src/main/resources/compiled/count_vowels.rs.wasm > count_vowels.wasm
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT
```
-->

<!--
```java
docs.FileOps.copyFromWasmCorpus("count_vowels.rs.wasm", "count_vowels.wasm");
```
-->

Build and instantiate this module:

```java
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;

Instance instance = Instance.builder(Parser.parse(new File("./count_vowels.wasm"))).build();
ExportFunction countVowels = instance.export("count_vowels");
```

To pass it a string, we first need to write the string into the module's memory. To make this easier and safe,
the module gives us some extra exports to allow us to allocate and deallocate memory:

```java
ExportFunction alloc = instance.export("alloc");
ExportFunction dealloc = instance.export("dealloc");
```

Let's allocate Wasm memory for a string and write it into the instance memory. We can do this with `Memory#put`:

```java
import com.dylibso.chicory.runtime.Memory;
Memory memory = instance.memory();
String message = "Hello, World!";
int len = message.getBytes().length;
// allocate {len} bytes of memory, this returns a pointer to that memory
int ptr = (int) alloc.apply(len)[0];
// We can now write the message to the module's memory:
memory.writeString(ptr, message);
```

Now we can call `countVowels` with this pointer to the string. It will do its job and return the count. We will
call `dealloc` to free that memory in the module. Though the module could do this itself if you want:

```java
var result = countVowels.apply(ptr, len)[0];
dealloc.apply(ptr, len);
assert(3L == result); // 3 vowels in Hello, World!
```

<!--
```java
docs.FileOps.writeResult("docs/usage", "memory.md.result", "" + result);
```
-->
