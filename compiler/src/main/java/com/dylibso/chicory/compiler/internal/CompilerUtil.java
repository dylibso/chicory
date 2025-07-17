package com.dylibso.chicory.compiler.internal;

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
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class CompilerUtil {

    private CompilerUtil() {}

    // The maximum number of wasm parameters that can be passed to a function before we box them
    // since Java
    // methods have a limit of 255 parameters, but we need to reserve a few for the Instance and
    // Memory args.
    private static final int MAX_PARAMETER_COUNT = 253;

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

    public static Class<?> jvmType(ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                return int.class;
            case ValType.ID.I64:
                return long.class;
            case ValType.ID.F32:
                return float.class;
            case ValType.ID.F64:
                return double.class;
            default:
                throw new IllegalArgumentException("Unsupported ValType: " + type);
        }
    }

    public static Type asmType(ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                return INT_TYPE;
            case ValType.ID.I64:
                return LONG_TYPE;
            case ValType.ID.F32:
                return FLOAT_TYPE;
            case ValType.ID.F64:
                return DOUBLE_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static ValType localType(FunctionType type, FunctionBody body, int localIndex) {
        if (localIndex < type.params().size()) {
            return type.params().get(localIndex);
        } else {
            return body.localTypes().get(localIndex - type.params().size());
        }
    }

    public static void emitLongToJvm(MethodVisitor asm, ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                asm.visitInsn(Opcodes.L2I);
                return;
            case ValType.ID.I64:
                return;
            case ValType.ID.F32:
                emitInvokeStatic(asm, LONG_TO_F32);
                return;
            case ValType.ID.F64:
                emitInvokeStatic(asm, LONG_TO_F64);
                return;
            default:
                throw new IllegalArgumentException("Unsupported ValType: " + type);
        }
    }

    public static void emitJvmToLong(MethodVisitor asm, ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                asm.visitInsn(Opcodes.I2L);
                return;
            case ValType.ID.I64:
                return;
            case ValType.ID.F32:
                emitInvokeStatic(asm, F32_TO_LONG);
                return;
            case ValType.ID.F64:
                emitInvokeStatic(asm, F64_TO_LONG);
                return;
            default:
                throw new IllegalArgumentException("Unsupported ValType: " + type);
        }
    }

    public static MethodType valueMethodType(List<ValType> types) {
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
        return type.params().stream().mapToInt(CompilerUtil::slotCount).sum() > MAX_PARAMETER_COUNT;
    }

    public static MethodType rawMethodTypeFor(FunctionType type) {
        var paramsTypes =
                hasTooManyParameters(type) ? new Class[] {long[].class} : jvmParameterTypes(type);
        return methodType(jvmReturnType(type), paramsTypes);
    }

    public static Class<?>[] jvmTypes(List<ValType> types) {
        return types.stream().map(CompilerUtil::jvmType).toArray(Class[]::new);
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

    public static Object defaultValue(ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
                return 0;
            case ValType.ID.I64:
                return 0L;
            case ValType.ID.F32:
                return 0.0f;
            case ValType.ID.F64:
                return 0.0d;
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                return REF_NULL_VALUE;
            default:
                throw new IllegalArgumentException("Unsupported ValType: " + type);
        }
    }

    public static int slotCount(ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
            case ValType.ID.F32:
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                return 1;
            case ValType.ID.I64:
            case ValType.ID.F64:
                return 2;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static void emitPop(MethodVisitor asm, ValType type) {
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
                methodNameForFunc(funcId),
                methodTypeFor(functionType).toMethodDescriptorString(),
                false);
    }

    public static String valueMethodName(List<ValType> types) {
        return "value_"
                + types.stream()
                        .map(type -> type.name().toLowerCase(Locale.ROOT))
                        .collect(joining("_"));
    }

    public static String methodNameForFunc(int funcId) {
        return "func_" + funcId;
    }

    static String callMethodName(int funcId) {
        return "call_" + funcId;
    }

    public static String callIndirectMethodName(int typeId) {
        return "call_indirect_" + typeId;
    }

    public static String internalClassName(String name) {
        return name.replace('.', '/');
    }

    static String classNameForDispatch(String prefix, int id) {
        return prefix + "Dispatch_" + id;
    }

    static String callDispatchMethodName(int start) {
        return "call_dispatch_" + start;
    }

    static String classNameForCallIndirect(String prefix, int typeId, int start) {
        return prefix + "Indirect_" + typeId + "_" + start;
    }
}
