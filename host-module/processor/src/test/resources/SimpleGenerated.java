package chicory.testing;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.hostmodule.processor.HostModuleProcessor")
public final class Simple_ModuleFactory {

    private Simple_ModuleFactory() {
    }

    public static HostFunction[] toHostFunctions(Simple functions) {
        return new HostFunction[] { //
        new HostFunction("simple",
                         "print",
                         List.of(ValueType.I32,
                                 ValueType.I32),
                         List.of(),
                         (Instance instance, long... args) -> {
                             functions.print(instance.memory().readString((int) args[0],
                                                                          (int) args[1]));
                             return null;
                         }), //
        new HostFunction("simple",
                         "printx",
                         List.of(ValueType.I32),
                         List.of(),
                         (Instance instance, long... args) -> {
                             functions.printx(instance.memory().readCString((int) args[0]));
                             return null;
                         }), //
        new HostFunction("simple",
                         "random_get",
                         List.of(ValueType.I32,
                                 ValueType.I32),
                         List.of(),
                         (Instance instance, long... args) -> {
                             functions.randomGet(instance.memory(),
                                                 (int) args[0],
                                                 (int) args[1]);
                             return null;
                         }), //
        new HostFunction("simple",
                         "exit",
                         List.of(),
                         List.of(),
                         (Instance instance, long... args) -> {
                             functions.exit();
                             return null;
                         }) };
    }
}
