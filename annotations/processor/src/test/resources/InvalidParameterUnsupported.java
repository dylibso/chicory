package chicory.testing;

import com.dylibso.chicory.annotations.HostModule;
import com.dylibso.chicory.annotations.WasmExport;
import java.math.BigDecimal;

@HostModule("bad_param")
public final class InvalidParameterUnsupported {

    @WasmExport
    public long square(BigDecimal x) {
        return x.pow(2);
    }
}
