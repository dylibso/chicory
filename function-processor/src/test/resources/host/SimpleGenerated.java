package chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.function.processor.HostModuleProcessor")
public final class Simple_ModuleFactory {

    private Simple_ModuleFactory() {}

    public static HostFunction[] toHostFunctions(Simple functions) {
        return new HostFunction[] {
            new HostFunction(
                    (Instance instance, Value... args) -> {
                        functions.print(
                                instance.memory().readString(args[0].asInt(), args[1].asInt()));
                        return null;
                    },
                    "simple",
                    "print",
                    List.of(ValueType.I32, ValueType.I32),
                    List.of()
            ),
            new HostFunction(
                    (Instance instance, Value... args) -> {
                        functions.printx(instance.memory().readCString(args[0].asInt()));
                        return null;
                    },
                    "simple",
                    "printx",
                    List.of(ValueType.I32, ValueType.I32),
                    List.of()
            ),
            new HostFunction(
                    (Instance instance, Value... args) -> {
                        functions.randomGet(instance.memory(), args[0].asInt(), args[1].asInt());
                        return null;
                    },
                    "simple",
                    "random_get",
                    List.of(ValueType.I32, ValueType.I32),
                    List.of()
            ),
            new HostFunction(
                    (Instance instance, Value... args) -> {
                        functions.exit();
                        return null;
                    },
                    "simple",
                    "exit",
                    List.of(),
                    List.of()
            ),
        };
    }
}
