package chicory.testing;

import com.dylibso.chicory.hostmodule.annotations.HostModule;
import com.dylibso.chicory.hostmodule.annotations.WasmExport;

@HostModule("bad_return")
public final class InvalidReturn {

    @WasmExport
    public String toString(int x) {
        return String.valueOf(x);
    }
}
