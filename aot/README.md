# Chicory AOT

Experimental Wasm -> JVM bytecode AOT engine.

To enable use the AotMachine factory when building the module:

```java
// ...
import com.dylibso.chicory.aot.AotMachine;
// ...

var module = Module
        .builder("mymodule.wasm")
        .withMachineFactory(AotMachine::new)
        )
        .build();
```
