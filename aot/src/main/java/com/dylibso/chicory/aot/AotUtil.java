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

    private static final Method UNBOX_I32;
    private static final Method UNBOX_I64;
    private static final Method UNBOX_F32;
    private static final Method UNBOX_F64;
    private static final Method UNBOX_EXTREF;
    private static final Method UNBOX_FUNCREF;
    private static final Method BOX_I32;
    private static final Method BOX_I64;
    private static final Method BOX_F32;
    private static final Method BOX_F64;
    private static final Method BOX_EXTREF;
    private static final Method BOX_FUNCREF;

    static {
        try {
            BOX_I32 = ValueConversions.class.getMethod("asLong", int.class);
            BOX_I64 = ValueConversions.class.getMethod("asLong", long.class);
            BOX_F32 = ValueConversions.class.getMethod("asLong", float.class);
            BOX_F64 = ValueConversions.class.getMethod("asLong", double.class);
            BOX_EXTREF = ValueConversions.class.getMethod("asLong", long.class);
            BOX_FUNCREF = ValueConversions.class.getMethod("asLong", long.class);
            UNBOX_I32 = ValueConversions.class.getMethod("toInt", long.class);
            UNBOX_I64 = ValueConversions.class.getMethod("toLong", long.class);
            UNBOX_F32 = ValueConversions.class.getMethod("toFloat", long.class);
            UNBOX_F64 = ValueConversions.class.getMethod("toDouble", long.class);
            UNBOX_EXTREF = ValueConversions.class.getMethod("toLong", long.class);
            UNBOX_FUNCREF = ValueConversions.class.getMethod("toLong", long.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public static Class<?> jvmType(ValueType type) {
        switch (type) {
            case I32:
                return int.class;
            case ExternRef:
            case FuncRef:
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
            case ExternRef:
                return UNBOX_EXTREF;
            case FuncRef:
                return UNBOX_FUNCREF;
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
            case ExternRef:
                return BOX_EXTREF;
            case FuncRef:
                return BOX_FUNCREF;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static MethodHandle unboxerHandle(ValueType type) {
        try {
            return publicLookup().unreflect(unboxer(type));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static MethodHandle boxerHandle(ValueType type) {
        try {
            return publicLookup().unreflect(boxer(type));
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
