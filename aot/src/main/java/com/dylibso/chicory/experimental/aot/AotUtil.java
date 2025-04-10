package com.dylibso.chicory.experimental.aot;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.lang.invoke.MethodType.methodType;
import static java.util.stream.Collectors.joining;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
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
import java.util.Locale;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class AotUtil {

    private AotUtil() {}

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

    public static Type asmType(ValueType type) {
        switch (type) {
            case I32:
            case ExternRef:
            case FuncRef:
                return INT_TYPE;
            case I64:
                return LONG_TYPE;
            case F32:
                return FLOAT_TYPE;
            case F64:
                return DOUBLE_TYPE;
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

    public static MethodType valueMethodType(List<ValueType> types) {
        return methodType(long[].class, jvmTypes(types));
    }

    public static MethodType callIndirectMethodType(FunctionType functionType) {
        return rawMethodTypeFor(functionType)
                .appendParameterTypes(int.class, int.class, Memory.class, Instance.class);
    }

    public static MethodType methodTypeFor(FunctionType type) {
        return rawMethodTypeFor(type).appendParameterTypes(Memory.class, Instance.class);
    }

    public static boolean hasTooManyParameters(FunctionType type) {
        return type.params().stream().mapToInt(AotUtil::slotCount).sum() > 253;
    }

    public static MethodType rawMethodTypeFor(FunctionType type) {
        var paramsTypes =
                hasTooManyParameters(type) ? new Class[] {long[].class} : jvmParameterTypes(type);
        return methodType(jvmReturnType(type), paramsTypes);
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

    public static void emitPop(MethodVisitor asm, ValueType type) {
        asm.visitInsn(slotCount(type) == 1 ? Opcodes.POP : Opcodes.POP2);
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

    public static String valueMethodName(List<ValueType> types) {
        return "value_"
                + types.stream()
                        .map(type -> type.name().toLowerCase(Locale.ROOT))
                        .collect(joining("_"));
    }

    public static String methodNameFor(int funcId) {
        return "func_" + funcId;
    }

    public static String callIndirectMethodName(int typeId) {
        return "call_indirect_" + typeId;
    }

    public static String internalClassName(String name) {
        return name.replace('.', '/');
    }
}
