package com.dylibso.chicory.aot;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.lang.invoke.MethodType.methodType;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
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

    private static final Method LONG_TO_F32;
    private static final Method LONG_TO_F64;
    private static final Method F32_TO_LONG;
    private static final Method F64_TO_LONG;

    static {
        try {
            LONG_TO_F32 = Value.class.getMethod("longToFloat", long.class);
            LONG_TO_F64 = Value.class.getMethod("longToDouble", long.class);
            F32_TO_LONG = Value.class.getMethod("floatToLong", float.class);
            F64_TO_LONG = Value.class.getMethod("doubleToLong", double.class);
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

    public static void emitLongToJvm(MethodVisitor asm, ValueType type) {
        switch (type) {
            case I32:
            case ExternRef:
            case FuncRef:
                asm.visitInsn(Opcodes.L2I);
                return;
            case I64:
                return;
            case F32:
                emitInvokeStatic(asm, LONG_TO_F32);
                return;
            case F64:
                emitInvokeStatic(asm, LONG_TO_F64);
                return;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static void emitJvmToLong(MethodVisitor asm, ValueType type) {
        switch (type) {
            case I32:
            case ExternRef:
            case FuncRef:
                asm.visitInsn(Opcodes.I2L);
                return;
            case I64:
                return;
            case F32:
                emitInvokeStatic(asm, F32_TO_LONG);
                return;
            case F64:
                emitInvokeStatic(asm, F64_TO_LONG);
                return;
            default:
                throw new IllegalArgumentException("Unsupported ValueType: " + type.name());
        }
    }

    public static MethodType callIndirectMethodType(FunctionType functionType) {
        return rawMethodTypeFor(functionType)
                .appendParameterTypes(int.class, int.class, Memory.class, Instance.class);
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

    public static StackSize stackSize(ValueType type) {
        switch (type) {
            case I32:
            case F32:
            case ExternRef:
            case FuncRef:
                return StackSize.ONE;
            case I64:
            case F64:
                return StackSize.TWO;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
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

    public static void emitInvokeFunction(
            MethodVisitor asm, String internalClassName, int funcId, FunctionType functionType) {
        asm.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                internalClassName,
                methodNameFor(funcId),
                methodTypeFor(functionType).toMethodDescriptorString(),
                false);
    }

    public static String methodNameFor(int funcId) {
        return "func_" + funcId;
    }

    public static String callIndirectMethodName(int typeId) {
        return "call_indirect_" + typeId;
    }
}
