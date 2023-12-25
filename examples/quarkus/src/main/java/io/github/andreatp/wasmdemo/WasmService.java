package io.github.andreatp.wasmdemo;

import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import java.io.InputStream;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WasmService {

    private Module module;

    public void setProgram(InputStream program) {
        this.module = Module.build(program);
    }

    public String compute(String content) {
        if (this.module == null) {
            throw new IllegalArgumentException("The WASM program have not been set, please do it!");
        }

        var instance = module.instantiate();
        var alloc = instance.getExport("alloc");
        var countVowels = instance.getExport("count");
        var memory = instance.getMemory();
        var len = content.getBytes().length;
        var ptr = alloc.apply(Value.i32(len))[0].asInt();
        memory.put(ptr, content);
        var result = countVowels.apply(Value.i32(ptr), Value.i32(len));

        return "result: " + result[0].asInt();
    }
}
