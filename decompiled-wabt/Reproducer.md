# JDK-8376400 reproducer

This is a self contained reproducer for JDK-8376400.
We keep the pom.wml for conveninece, but it can be reproduced with plain `javac`/`java` executions.

To build: `./build.sh`
To run: `./run.sh`

On Java 18+, or on Java 17 after applying this commit: `f3eb5014aa75af4463308f52f2bc6e9fcd2da36c` the run command completes successfully.

On Java <= 17 the run fails with this error(much down the line):

```
Exception in thread "main" com.dylibso.chicory.runtime.WasmRuntimeException: out of bounds memory access: attempted to access address: 668598272 but limit is: 668598272 and size: 8
        at com.dylibso.chicory.runtime.ByteBufferMemory.outOfBoundsException(ByteBufferMemory.java:313)
        at com.dylibso.chicory.runtime.ByteBufferMemory.writeLong(ByteBufferMemory.java:424)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_1890(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_1890(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_231(Wat2WasmMachine.java:22513)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_231(Wat2WasmMachine.java:22523)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_224(Wat2WasmMachine.java:22138)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_224(Wat2WasmMachine.java:22152)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_41(Wat2WasmMachine.java:2699)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_41(Wat2WasmMachine.java:3555)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_40(Wat2WasmMachine.java:2488)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_40(Wat2WasmMachine.java:2679)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_21(Wat2WasmMachine.java:678)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_21(Wat2WasmMachine.java:998)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_33(Wat2WasmMachine.java:1758)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_33(Wat2WasmMachine.java:1765)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_1791(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_1791(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.func_18(Wat2WasmMachine.java:191)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call_18(Wat2WasmMachine.java:208)
        at com.dylibso.chicory.wabt.Wat2WasmMachine.call(Wat2WasmMachine.java)
        at com.dylibso.chicory.runtime.Instance$Exports.lambda$function$0(Instance.java:219)
        at com.dylibso.chicory.runtime.Instance.initialize(Instance.java:184)
        at com.dylibso.chicory.runtime.Instance.<init>(Instance.java:118)
        at com.dylibso.chicory.runtime.Instance$Builder.build(Instance.java:884)
        at com.dylibso.chicory.wabt.Wat2Wasm.parse(Wat2Wasm.java:65)
        at com.dylibso.chicory.wabt.Wat2Wasm.parse(Wat2Wasm.java:41)
        at com.dylibso.chicory.wabt.Main.main(Main.java:8)
```
