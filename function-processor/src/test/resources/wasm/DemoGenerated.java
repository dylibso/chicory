package chicory.testing;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.function.processor.WasmModuleProcessor")
public final class Demo_ModuleFactory {

    private Demo_ModuleFactory() {}

    public static Demo create(Instance instance) {
        return new Demo_Instance(instance);
    }

    private static final class Demo_Instance implements Demo {

        private final Memory memory;
        private final ExportFunction _malloc;
        private final ExportFunction _free;
        private final ExportFunction _print;
        private final ExportFunction _length;
        private final ExportFunction _multiReturn;

        public Demo_Instance(Instance instance) {
            memory = instance.memory();

            _malloc = instance.export("malloc");
            checkType(instance, "malloc", FunctionType.of(List.of(ValueType.I32), List.of(ValueType.I32)));

            _free = instance.export("free");
            checkType(instance, "free", FunctionType.of(List.of(ValueType.I32), List.of()));

            _print = instance.export("print");
            checkType(instance, "print", FunctionType.of(List.of(ValueType.I32), List.of()));

            _length = instance.export("length");
            checkType(instance, "length", FunctionType.of(List.of(ValueType.I32, ValueType.I32), List.of(ValueType.I32)));

            _multiReturn = instance.export("multi_return");
            checkType(instance, "multi_return", List.of(ValueType.F32));
        }

        @Override
        public int malloc(int _size) {
            Value[] args = new Value[] { Value.i32(_size) };
            int result = _malloc.apply(args)[0].asInt();
            return result;
        }

        @Override
        public void free(int _ptr) {
            Value[] args = new Value[] { Value.i32(_ptr) };
            _free.apply(args);
        }

        @Override
        public void print(String _data) {
            byte[] bytes_data = _data.getBytes(StandardCharsets.UTF_8);
            int ptr_data = malloc(bytes_data.length + 1);
            memory.write(ptr_data, bytes_data);
            memory.writeByte(ptr_data + bytes_data.length, (byte) 0);
            Value[] args = new Value[] { Value.i32(ptr_data) };
            _print.apply(args);
            free(ptr_data);
        }

        @Override
        public int length(String _data) {
            byte[] bytes_data = _data.getBytes(StandardCharsets.UTF_8);
            int ptr_data = malloc(bytes_data.length);
            memory.write(ptr_data, bytes_data);
            Value[] args = new Value[] { Value.i32(ptr_data), Value.i32(bytes_data.length) };
            int result = _length.apply(args)[0].asInt();
            free(ptr_data);
            return result;
        }

        @Override
        public Value[] multiReturn(float _x) {
            Value[] args = new Value[] { Value.fromFloat(_x) };
            Value[] result = _multiReturn.apply(args);
            return result;
        }

        private static void checkType(Instance instance, String name, FunctionType expected) {
            checkType(name, expected, instance.exportType(name));
        }

        private static void checkType(Instance instance, String name, List<ValueType> expected) {
            checkType(name, expected, instance.exportType(name).params());
        }

        private static <T> void checkType(String name, T expected, T actual) {
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException(String.format(
                        "Function type mismatch for '%s': expected %s <=> actual %s", name, expected, actual));
            }
        }
    }
}
