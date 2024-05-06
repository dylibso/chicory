package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

public class AotUtil {

    private static final Map<ValueType, Class<?>> JVM_TYPES =
            Map.of(
                    ValueType.I32, int.class,
                    ValueType.I64, long.class,
                    ValueType.F32, float.class,
                    ValueType.F64, double.class);

    private static final Map<ValueType, String> UNBOX_METHODS =
            Map.of(
                    ValueType.I32, "asInt",
                    ValueType.I64, "asLong",
                    ValueType.F32, "asFloat",
                    ValueType.F64, "asDouble");

    public static Class<?> jvmType(ValueType type) {
        return Objects.requireNonNull(JVM_TYPES.get(type), "Unsupported ValueType: " + type.name());
    }

    public static ValueType localType(FunctionType type, FunctionBody body, int localIndex) {
        if (localIndex < type.params().size()) {
            return type.params().get(localIndex);
        } else {
            return body.localTypes().get(localIndex - type.params().size());
        }
    }

    public static String unboxMethodName(ValueType type) {
        return Objects.requireNonNull(
                UNBOX_METHODS.get(type), "Unsupported ValueType: " + type.name());
    }

    public static MethodHandle unboxer(ValueType type)
            throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup()
                .findVirtual(
                        Value.class, unboxMethodName(type), MethodType.methodType(jvmType(type)));
    }

    public static MethodType methodTypeFor(FunctionType type) {
        return MethodType.methodType(jvmReturnType(type), jvmParameterTypes(type));
    }

    public static Class<?>[] jvmParameterTypes(FunctionType type) {
        return type.params().stream().map(AotUtil::jvmType).toArray(Class[]::new);
    }

    public static Class<?> jvmReturnType(FunctionType type) {
        return int.class; // TODO
    }

    public static MethodHandle loadCallHandle(String name, FunctionType type, byte[] compiledBody)
            throws NoSuchMethodException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException {
        var loader = new ByteArrayClassLoader(AotUtil.class.getClassLoader());
        var cls = loader.loadFromBytes(name, compiledBody);
        var ins = cls.getConstructor().newInstance();
        return MethodHandles.lookup().findVirtual(cls, "call", methodTypeFor(type)).bindTo(ins);
    }
}
