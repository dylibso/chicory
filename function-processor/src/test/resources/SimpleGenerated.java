package chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.function.processor.FunctionProcessor")
public final class Simple_ModuleFactory {

    private Simple_ModuleFactory() {
    }

    public static HostFunction[] toHostFunctions(Simple functions) {
        return new HostFunction[] { //
                new HostFunction("simple",
                        "print",
                        (Instance instance, long... args) -> {
                            functions.print(instance.memory().readString((int) args[0],
                                    (int) args[1]));
                            return null;
                        },
                        List.of(ValueType.I32,
                                ValueType.I32),
                        List.of()), //
                new HostFunction("simple",
                        "printx",
                        (Instance instance, long... args) -> {
                            functions.printx(instance.memory().readCString((int) args[0]));
                            return null;
                        },
                        List.of(ValueType.I32),
                        List.of()), //
                new HostFunction("simple",
                        "random_get",
                        (Instance instance, long... args) -> {
                            functions.randomGet(instance.memory(),
                                    (int) args[0],
                                    (int) args[1]);
                            return null;
                        },
                        List.of(ValueType.I32,
                                ValueType.I32),
                        List.of()), //
                new HostFunction("simple",
                        "exit",
                        (Instance instance, long... args) -> {
                            functions.exit();
                            return null;
                        },
                        List.of(),
                        List.of()) };
    }
}
