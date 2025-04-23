package chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;

import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.experimental.hostmodule.processor.HostModuleProcessor")
public final class BasicMath_ModuleFactory {

    private BasicMath_ModuleFactory() {
    }

    public static HostFunction[] toHostFunctions(BasicMath functions) {
        return toHostFunctions(functions,
                "math");
    }

    public static HostFunction[] toHostFunctions(BasicMath functions, String moduleName) {
        return new HostFunction[] { //
                new HostFunction(moduleName,
                        "add",
                        FunctionType.of(
                                List.of(ValType.I32,
                                        ValType.I32),
                                List.of(ValType.I64)),
                        (Instance instance, long... args) -> {
                            long result = functions.add((int) args[0],
                                    (int) args[1]);
                            return new long[] { result };
                        }), //
                new HostFunction(moduleName,
                        "square",
                        FunctionType.of(
                                List.of(ValType.F32),
                                List.of(ValType.F64)),
                        (Instance instance, long... args) -> {
                            double result = functions.pow2(Value.longToFloat(args[0]));
                            return new long[] { Value.doubleToLong(result) };
                        }), //
                new HostFunction(moduleName,
                        "floor_div",
                        FunctionType.of(
                                List.of(ValType.I32,
                                        ValType.I32),
                                List.of(ValType.I32)),
                        (Instance instance, long... args) -> {
                            int result = functions.floorDiv((int) args[0],
                                    (int) args[1]);
                            return new long[] { (long) result };
                        }) };
    }
}
