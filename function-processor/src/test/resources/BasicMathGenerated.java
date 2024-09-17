package chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.function.processor.FunctionProcessor")
public final class BasicMath_ModuleFactory {

    private BasicMath_ModuleFactory() {}

    public static HostFunction[] toHostFunctions(BasicMath functions) {
        return new HostFunction[] {
            new HostFunction(
                    "math", "add", (Instance instance, Value... args) -> {
                        long result = functions.add(args[0].asInt(), args[1].asInt());
                        return new Value[] { Value.i64(result) };
                    },
                    List.of(ValueType.I32, ValueType.I32),
                    List.of(ValueType.I64)
            ),
            new HostFunction(
                    "math", "square", (Instance instance, Value... args) -> {
                        double result = functions.pow2(args[0].asFloat());
                        return new Value[] { Value.fromDouble(result) };
                    },
                    List.of(ValueType.F32),
                    List.of(ValueType.F64)
            ),
            new HostFunction(
                    "math", "floor_div", (Instance instance, Value... args) -> {
                        int result = functions.floorDiv(args[0].asInt(), args[1].asInt())
                        return new Value[] { Value.i32(result) };
                    },
                    List.of(ValueType.I32, ValueType.I32),
                    List.of(ValueType.I32)
            ),
        };
    }
}
