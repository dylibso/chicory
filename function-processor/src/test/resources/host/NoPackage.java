import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;

@HostModule("nopackage")
public final class NoPackage {

    @WasmExport
    public void print(Memory memory, int ptr, int len) {
        System.out.println(memory.readString(ptr, len));
    }

    @WasmExport
    public void exit() {
        throw new ChicoryException("exit");
    }

    public HostFunction[] toHostFunctions() {
        return NoPackage_ModuleFactory.toHostFunctions(this);
    }
}
