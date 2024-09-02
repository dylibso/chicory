package chicory.testing;

import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;

@HostModule("simple")
public final class Simple {

    @WasmExport
    public void print(Memory memory, int ptr, int len) {
        System.out.println(memory.readString(ptr, len));
    }

    @WasmExport
    public void exit() {
        throw new ChicoryException("exit");
    }

    public HostFunction[] toHostFunctions() {
        return Simple_ModuleFactory.toHostFunctions(this);
    }
}
