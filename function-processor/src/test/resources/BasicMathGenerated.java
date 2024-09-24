package chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.function.processor.FunctionProcessor")
public final class BasicMath_ModuleFactory {

    private BasicMath_ModuleFactory() {
    }

    public static HostFunction[] toHostFunctions(BasicMath functions) {
        return new HostFunction[] { //
                new HostFunction("math",
                        "add",
                        (Instance instance, long... args) -> {
                            long result = functions.add((int) args[0],
                                    (int) args[1]);
                            return new long[] { result };
                        },
                        List.of(ValueType.I32,
                                ValueType.I32),
                        List.of(ValueType.I64)), //
                new HostFunction("math",
                        "square",
                        (Instance instance, long... args) -> {
                            double result = functions.pow2(Float.intBitsToFloat((int) args[0]));
                            return new long[] { Double.doubleToLongBits(result) };
                        },
                        List.of(ValueType.F32),
                        List.of(ValueType.F64)), //
                new HostFunction("math",
                        "floor_div",
                        (Instance instance, long... args) -> {
                            int result = functions.floorDiv((int) args[0],
                                    (int) args[1]);
                            return new long[] { (long) result };
                        },
                        List.of(ValueType.I32,
                                ValueType.I32),
                        List.of(ValueType.I32)) };
    }
}
