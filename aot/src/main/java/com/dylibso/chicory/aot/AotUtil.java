package com.dylibso.chicory.aot;

import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AotUtil {

    public enum StackSize {
        ONE,
        TWO
    }

    private static final Method UNBOX_I32;
    private static final Method UNBOX_I64;
    private static final Method UNBOX_F32;
    private static final Method UNBOX_F64;
    private static final Method BOX_I32;
    private static final Method BOX_I64;
    private static final Method BOX_F32;
    private static final Method BOX_F64;

    static {
        try {
            UNBOX_I32 = Value.class.getMethod("asInt");
            UNBOX_I64 = Value.class.getMethod("asLong");
            UNBOX_F32 = Value.class.getMethod("asFloat");
            UNBOX_F64 = Value.class.getMethod("asDouble");
            BOX_I32 = Value.class.getMethod("i32", int.class);
            BOX_I64 = Value.class.getMethod("i64", long.class);
            BOX_F32 = Value.class.getMethod("fromFloat", float.class);
            BOX_F64 = Value.class.getMethod("fromDouble", double.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

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

    public static Method unboxer(ValueType type) {
        switch (type) {
            case I32:
                return UNBOX_I32;
            case I64:
                return UNBOX_I64;
            case F32:
                return UNBOX_F32;
            case F64:
                return UNBOX_F64;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static Method boxer(ValueType type) {
        switch (type) {
            case I32:
                return BOX_I32;
            case I64:
                return BOX_I64;
            case F32:
                return BOX_F32;
            case F64:
                return BOX_F64;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
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

    public static Object defaultValue(ValueType type) {
        switch (type) {
            case I32:
                return 0;
            case I64:
                return 0L;
            case F32:
                return 0.0f;
            case F64:
                return 0.0d;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void validateArgumentType(Class<?> clazz) {
        stackSize(clazz);
    }

    public static StackSize stackSize(Class<?> clazz) {
        if (clazz == int.class || clazz == float.class) {
            return StackSize.ONE;
        }
        if (clazz == long.class || clazz == double.class) {
            return StackSize.TWO;
        }
        throw new IllegalArgumentException("Unsupported JVM type: " + clazz);
    }

    public static void emitInvokeStatic(MethodVisitor asm, Method method) {
        assert Modifier.isStatic(method.getModifiers());
        asm.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                getInternalName(method.getDeclaringClass()),
                method.getName(),
                getMethodDescriptor(method),
                false);
    }

    public static void emitInvokeVirtual(MethodVisitor asm, Method method) {
        assert !Modifier.isStatic(method.getModifiers());
        assert !method.getDeclaringClass().isInterface();
        asm.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                getInternalName(method.getDeclaringClass()),
                method.getName(),
                getMethodDescriptor(method),
                false);
    }
}
