package chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;

import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.experimental.hostmodule.processor.HostModuleProcessor")
public final class Simple_ModuleFactory {

    private Simple_ModuleFactory() {
    }

    public static HostFunction[] toHostFunctions(Simple functions) {
        return toHostFunctions(functions, "simple");
    }

    public static HostFunction[] toHostFunctions(Simple functions, String moduleName) {
        return new HostFunction[] { //
                new HostFunction(moduleName,
                        "print",
                        FunctionType.of(
                                List.of(ValType.I32,
                                        ValType.I32),
                                List.of()),
                        (Instance instance, long... args) -> {
                            functions.print(instance.memory().readString((int) args[0],
                                    (int) args[1]));
                            return null;
                        }), //
                new HostFunction(moduleName,
                        "printx",
                        FunctionType.of(
                                List.of(ValType.I32),
                                List.of()),
                        (Instance instance, long... args) -> {
                            functions.printx(instance.memory().readCString((int) args[0]));
                            return null;
                        }), //
                new HostFunction(moduleName,
                        "random_get",
                        FunctionType.of(
                                List.of(ValType.I32,
                                        ValType.I32),
                                List.of()),
                        (Instance instance, long... args) -> {
                            functions.randomGet(instance.memory(),
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
