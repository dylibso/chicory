package chicory.testing;

import com.dylibso.chicory.hostmodule.annotations.HostModule;
import com.dylibso.chicory.hostmodule.annotations.WasmExport;

@HostModule("bad_param")
public final class InvalidParameterString {

    @WasmExport
    public long concat(int a, String s) {
        return (s + a).length();
    }
}