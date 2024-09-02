package chicory.testing;

import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.function.annotations.Buffer;
import com.dylibso.chicory.function.annotations.CString;
import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import java.util.Random;

@HostModule("simple")
public final class Simple {

    private final Random random;

    public Simple(Random random) {
        this.random = requireNonNull(random);
    }

    @WasmExport
    public void print(@Buffer String data) {
        System.out.println(data);
    }

    @WasmExport
    public void printx(@CString String data) {
        System.out.println(data);
    }

    @WasmExport
    public void randomGet(Memory memory, int ptr, int len) {
        byte[] data = new byte[len];
        random.nextBytes(data);
        memory.write(ptr, data);
    }

    @WasmExport
    public void exit() {
        throw new ChicoryException("exit");
    }

    public HostFunction[] toHostFunctions() {
        return Simple_ModuleFactory.toHostFunctions(this);
    }
}
