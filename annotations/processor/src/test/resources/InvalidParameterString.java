package chicory.testing;

import com.dylibso.chicory.annotations.HostModule;
import com.dylibso.chicory.annotations.WasmExport;

@HostModule("bad_param")
public final class InvalidParameterString {

    @WasmExport
    public long concat(int a, String s) {
        return (s + a).length();
    }
}
