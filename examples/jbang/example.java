///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.dylibso.chicory:wasm:1.0-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:1.0-SNAPSHOT

import static java.lang.System.*;

import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;

public class example {

    public static void main(String... args) {
        if (args.length != 2) {
            err.println("Please pass as argument a wasm file and a sentence");
        }
        
        var instance = Module.build(args[0]).instantiate();
        var alloc = instance.getExport("alloc");
        var countVowels = instance.getExport("count");
        var memory = instance.getMemory();
        var len = args[1].getBytes().length;
        var ptr = alloc.apply(Value.i32(len)).asInt();
        memory.put(ptr, args[1]);
        var result = countVowels.apply(Value.i32(ptr), Value.i32(len));

        out.println("Count is: " + result.asInt());
    }
}
