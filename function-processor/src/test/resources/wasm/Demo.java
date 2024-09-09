package chicory.testing;

import com.dylibso.chicory.function.annotations.Allocate;
import com.dylibso.chicory.function.annotations.Buffer;
import com.dylibso.chicory.function.annotations.CString;
import com.dylibso.chicory.function.annotations.Free;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.function.annotations.WasmModule;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;

@WasmModule
public interface Demo {

    @Allocate
    @WasmExport
    int malloc(int size);

    @Free
    @WasmExport
    void free(int ptr);

    @WasmExport
    void print(@CString String data);

    @WasmExport
    int length(@Buffer String data);

    @WasmExport
    Value[] multiReturn(float x);

    static Demo create(Instance instance) {
        return Demo_ModuleFactory.create(instance);
    }
}
