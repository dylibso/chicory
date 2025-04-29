import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;

import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.experimental.hostmodule.processor.HostModuleProcessor")
public final class NoPackage_ModuleFactory {

    private NoPackage_ModuleFactory() {
    }

    public static HostFunction[] toHostFunctions(NoPackage functions) {
        return toHostFunctions(functions, "nopackage");
    }

    public static HostFunction[] toHostFunctions(NoPackage functions, String moduleName) {
        return new HostFunction[] { //
                new HostFunction(moduleName,
                        "print",
                        FunctionType.of(
                                List.of(ValType.I32,
                                        ValType.I32),
                                List.of()),
                        (Instance instance, long... args) -> {
                            functions.print(instance.memory(),
                                    (int) args[0],
                                    (int) args[1]);
                            return null;
                        }), //
                new HostFunction(moduleName,
                        "exit",
                        FunctionType.of(List.of(), List.of()),
                        (Instance instance, long... args) -> {
                            functions.exit();
                            return null;
                        }) };
    }
}
