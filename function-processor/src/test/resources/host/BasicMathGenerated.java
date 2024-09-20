package chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.function.processor.HostModuleProcessor")
public final class BasicMath_ModuleFactory {

    private BasicMath_ModuleFactory() {}

    public static HostFunction[] toHostFunctions(BasicMath functions) {
        return new HostFunction[] {
            new HostFunction(
                    (Instance instance, Value... args) -> {
                        long result = functions.add(args[0].asInt(), args[1].asInt());
                        return new Value[] { Value.i64(result) };
                    },
                    "math",
                    "add",
                    List.of(ValueType.I32, ValueType.I32),
                    List.of(ValueType.I64)
            ),
            new HostFunction(
                    (Instance instance, Value... args) -> {
                        double result = functions.pow2(args[0].asFloat());
                        return new Value[] { Value.fromDouble(result) };
                    },
                    "math",
                    "square",
                    List.of(ValueType.F32),
                    List.of(ValueType.F64)
            ),
            new HostFunction(
                    (Instance instance, Value... args) -> {
                        int result = functions.floorDiv(args[0].asInt(), args[1].asInt())
                        return new Value[] { Value.i32(result) };
                    },
                    "math",
                    "floor_div",
                    List.of(ValueType.I32, ValueType.I32),
                    List.of(ValueType.I32)
            ),
        };
    }
}
