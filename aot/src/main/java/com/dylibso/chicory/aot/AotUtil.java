package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;

public class AotUtil {

    public static Class<?> jvmType(ValueType type) {
        switch (type) {
            case I32:
                return int.class;
            case I64:
                return long.class;
            case F32:
                return float.class;
            case F64:
                return double.class;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static ValueType localType(FunctionType type, FunctionBody body, int localIndex) {
        if (localIndex < type.params().size()) {
            return type.params().get(localIndex);
        } else {
            return body.localTypes().get(localIndex - type.params().size());
        }
    }

    public static String unboxMethodName(ValueType type) {
        switch (type) {
            case I32:
                return "asInt";
            case I64:
                return "asLong";
            case F32:
                return "asFloat";
            case F64:
                return "asDouble";
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static MethodHandle unboxer(ValueType type)
            throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup()
                .findVirtual(
                        Value.class, unboxMethodName(type), MethodType.methodType(jvmType(type)));
    }

    public static String boxMethodName(ValueType type) {
        switch (type) {
            case I32:
                return "i32";
            case I64:
                return "i64";
            case F32:
                return "fromFloat";
            case F64:
                return "fromDouble";
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static MethodHandle boxer(ValueType type)
            throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup()
                .findStatic(
                        Value.class,
                        boxMethodName(type),
                        MethodType.methodType(Value.class, jvmType(type)));
    }

    public static MethodType methodTypeFor(FunctionType type) {
        return MethodType.methodType(jvmReturnType(type), jvmParameterTypes(type));
    }

    public static Class<?>[] jvmParameterTypes(FunctionType type) {
        return type.params().stream().map(AotUtil::jvmType).toArray(Class[]::new);
    }

    public static Class<?> jvmReturnType(FunctionType type) {
        if (type.returns().size() == 0) {
            return void.class;
        } else if (type.returns().size() == 1) {
            return jvmType(type.returns().get(0));
        }
        // TODO
        throw new IllegalArgumentException("Multi-returns are not currently supported");
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
