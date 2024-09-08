package chicory.testing;

import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.runtime.HostFunction;

@HostModule("math")
public final class BasicMath {

    @WasmExport
    public long add(int a, int b) {
        return a + b;
    }

    @WasmExport("square")
    public double pow2(float x) {
        return x * x;
    }

    @WasmExport
    public int floorDiv(int x, int y) {
        return Math.floorDiv(x, y);
    }

    public HostFunction[] toHostFunctions() {
        return BasicMath_ModuleFactory.toHostFunctions(this);
    }
}
