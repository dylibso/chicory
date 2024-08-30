import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.gen.OpaModule;
import com.dylibso.chicory.runtime.HostImports;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import java.util.Arrays;
import java.util.List;

@HostModule("env")
class OpaTestModule extends OpaModule {
    private final Memory memory;
    private final Instance instance;

    public OpaTestModule() {
        this.memory = new Memory(new MemoryLimits(10));
        this.instance =
                Instance.builder(Parser.parse(OpaTest.class.getResourceAsStream("/policy.wasm")))
                        .withHostImports(
                                HostImports.builder()
                                        .withFunctions(
                                                Arrays.asList(
                                                        OpaTestModule_ModuleFactory.toHostFunctions(
                                                                this)))
                                        .withMemories(List.of(toHostMemory()))
                                        .build())
                        .build();
    }

    @Override
    public Memory memory() {
        return this.memory;
    }

    @Override
    public Instance instance() {
        return this.instance;
    }

    @WasmExport
    @Override
    public int opaBuiltin0(int arg0, int arg1) {
        throw new RuntimeException("opa_builtin0 - not implemented");
    }

    @WasmExport
    @Override
    public int opaBuiltin1(int arg0, int arg1, int arg2) {
        throw new RuntimeException("opa_builtin1 - not implemented");
    }

    @WasmExport
    @Override
    public int opaBuiltin2(int arg0, int arg1, int arg2, int arg3) {
        throw new RuntimeException("opa_builtin2 - not implemented");
    }

    @WasmExport
    @Override
    public int opaBuiltin3(int arg0, int arg1, int arg2, int arg3, int arg4) {
        throw new RuntimeException("opa_builtin3 - not implemented");
    }

    @WasmExport
    @Override
    public int opaBuiltin4(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
        throw new RuntimeException("opa_builtin4 - not implemented");
    }

    @WasmExport
    @Override
    public void opaAbort(int arg0) {
        System.exit(arg0);
    }
}
