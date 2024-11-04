package chicory.testing;

import com.dylibso.chicory.experimental.hostmodule.annotations.HostModule;
import com.dylibso.chicory.experimental.hostmodule.annotations.WasmExport;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.ChicoryException;

public class Box {

    @HostModule("nested")
    public final class Nested {

        @WasmExport
        public void print(Memory memory, int ptr, int len) {
            System.out.println(memory.readString(ptr, len));
        }

        @WasmExport
        public void exit() {
            throw new ChicoryException("exit");
        }

        public HostFunction[] toHostFunctions() {
            return Nested_ModuleFactory.toHostFunctions(this);
        }
    }

}
