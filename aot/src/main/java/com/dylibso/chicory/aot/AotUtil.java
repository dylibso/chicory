package com.dylibso.chicory.aot;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class AotUtil {

    private AotUtil() {}

    public enum StackSize {
        ONE,
        TWO
    }

    private static final Method LONG_2_I32;
    private static final Method LONG_2_I64;
    private static final Method LONG_2_F32;
    private static final Method LONG_2_F64;
    private static final Method LONG_2_EXTREF;
    private static final Method LONG_2_FUNCREF;
    private static final Method I32_2_LONG;
    private static final Method I64_2_LONG;
    private static final Method F32_2_LONG;
    private static final Method F64_2_LONG;
    private static final Method EXTREF_2_LONG;
    private static final Method FUNCREF_2_LONG;

    static {
        try {
            I32_2_LONG = ValueConversions.class.getMethod("asLong", int.class);
            I64_2_LONG = ValueConversions.class.getMethod("asLong", long.class);
            F32_2_LONG = ValueConversions.class.getMethod("asLong", float.class);
            F64_2_LONG = ValueConversions.class.getMethod("asLong", double.class);
            EXTREF_2_LONG = ValueConversions.class.getMethod("asLong", int.class);
            FUNCREF_2_LONG = ValueConversions.class.getMethod("asLong", int.class);
            LONG_2_I32 = ValueConversions.class.getMethod("toInt", long.class);
            LONG_2_I64 = ValueConversions.class.getMethod("toLong", long.class);
            LONG_2_F32 = ValueConversions.class.getMethod("toFloat", long.class);
            LONG_2_F64 = ValueConversions.class.getMethod("toDouble", long.class);
            LONG_2_EXTREF = ValueConversions.class.getMethod("toInt", long.class);
            LONG_2_FUNCREF = ValueConversions.class.getMethod("toInt", long.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public static Class<?> jvmType(ValueType type) {
        switch (type) {
            case I32:
            case ExternRef:
            case FuncRef:
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

    public static int loadTypeOpcode(ValueType type) {
        switch (type) {
            case I32:
            case ExternRef:
            case FuncRef:
                return Opcodes.ILOAD;
            case I64:
                return Opcodes.LLOAD;
            case F32:
                return Opcodes.FLOAD;
            case F64:
                return Opcodes.DLOAD;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static int storeTypeOpcode(ValueType type) {
        switch (type) {
            case I32:
            case ExternRef:
            case FuncRef:
                return Opcodes.ISTORE;
            case I64:
                return Opcodes.LSTORE;
            case F32:
                return Opcodes.FSTORE;
            case F64:
                return Opcodes.DSTORE;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static ValueType localType(FunctionType type, FunctionBody body, int localIndex) {
        if (localIndex < type.params().size()) {
            return type.params().get(localIndex);
        } else {
            return body.localTypes().get(localIndex - type.params().size());
        }
    }

    public static Method convertFromLong(ValueType type) {
        switch (type) {
            case I32:
                return LONG_2_I32;
            case I64:
                return LONG_2_I64;
            case F32:
                return LONG_2_F32;
            case F64:
                return LONG_2_F64;
            case ExternRef:
                return LONG_2_EXTREF;
            case FuncRef:
                return LONG_2_FUNCREF;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static Method convertToLong(ValueType type) {
        switch (type) {
            case I32:
                return I32_2_LONG;
            case I64:
                return I64_2_LONG;
            case F32:
                return F32_2_LONG;
            case F64:
                return F64_2_LONG;
            case ExternRef:
                return EXTREF_2_LONG;
            case FuncRef:
                return FUNCREF_2_LONG;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static MethodHandle convertFromLongHandle(ValueType type) {
        try {
            return publicLookup().unreflect(convertFromLong(type));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static MethodHandle convertToLongHandle(ValueType type) {
        try {
            return publicLookup().unreflect(convertToLong(type));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static MethodType callIndirectMethodType(FunctionType functionType) {
        return rawMethodTypeFor(functionType)
                .appendParameterTypes(int.class, int.class, Instance.class);
    }

    public static MethodType methodTypeFor(FunctionType type) {
        return rawMethodTypeFor(type).appendParameterTypes(Memory.class, Instance.class);
    }

    public static MethodType rawMethodTypeFor(FunctionType type) {
        return methodType(jvmReturnType(type), jvmParameterTypes(type));
    }

    public static Class<?>[] jvmTypes(List<ValueType> types) {
        return types.stream().map(AotUtil::jvmType).toArray(Class[]::new);
    }

    public static Class<?>[] jvmParameterTypes(FunctionType type) {
        return jvmTypes(type.params());
    }

    public static Class<?> jvmReturnType(FunctionType type) {
        switch (type.returns().size()) {
            case 0:
                return void.class;
            case 1:
                return jvmType(type.returns().get(0));
            default:
                return long[].class;
        }
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
            case ExternRef:
            case FuncRef:
                return REF_NULL_VALUE;
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

    public static int slotCount(ValueType type) {
        switch (type) {
            case I32:
            case F32:
            case ExternRef:
            case FuncRef:
                return 1;
            case I64:
            case F64:
                return 2;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static void emitPop(MethodVisitor asm, StackSize size) {
        asm.visitInsn(size == StackSize.ONE ? Opcodes.POP : Opcodes.POP2);
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

    public static String methodNameFor(int funcId) {
        return "func_" + funcId;
    }

    public static String callIndirectMethodName(int typeId) {
        return "call_indirect_" + typeId;
    }
}
