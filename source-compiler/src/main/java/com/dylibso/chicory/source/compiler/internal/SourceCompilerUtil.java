package com.dylibso.chicory.source.compiler.internal;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.PrimitiveType;
import java.util.List;

/**
 * Utility methods for source code compilation.
 * Mirrors the structure of the ASM compiler's CompilerUtil.
 */
final class SourceCompilerUtil {

    private SourceCompilerUtil() {}

    // The maximum number of wasm parameters that can be passed to a function before we box them
    // since Java methods have a limit of 255 parameters, but we need to reserve a few for the
    // Instance and Memory args.
    private static final int MAX_PARAMETER_COUNT = 253;

    /**
     * Convert ValType to JVM type (mirrors CompilerUtil.jvmType).
     */
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

    /**
     * Get JVM return type for a function (mirrors CompilerUtil.jvmReturnType).
     */
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

    /**
     * Get JVM parameter types for a function (mirrors CompilerUtil.jvmParameterTypes).
     */
    public static Class<?>[] jvmParameterTypes(FunctionType type) {
        return jvmTypes(type.params());
    }

    /**
     * Convert list of ValTypes to JVM types (mirrors CompilerUtil.jvmTypes).
     */
    public static Class<?>[] jvmTypes(List<ValType> types) {
        return types.stream().map(SourceCompilerUtil::jvmType).toArray(Class[]::new);
    }

    /**
     * Check if function has too many parameters (mirrors CompilerUtil.hasTooManyParameters).
     */
    public static boolean hasTooManyParameters(FunctionType type) {
        return type.params().stream().mapToInt(SourceCompilerUtil::slotCount).sum()
                > MAX_PARAMETER_COUNT;
    }

    /**
     * Get slot count for a ValType (mirrors CompilerUtil.slotCount).
     */
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

    /**
     * Get local variable type (mirrors CompilerUtil.localType).
     */
    public static ValType localType(FunctionType type, FunctionBody body, int localIndex) {
        if (localIndex < type.params().size()) {
            return type.params().get(localIndex);
        } else {
            return body.localTypes().get(localIndex - type.params().size());
        }
    }

    /**
     * Get default value for a ValType as a Java source code string.
     * Mirrors CompilerUtil.defaultValue but returns String for source code generation.
     */
    public static String defaultValue(ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
                return "0";
            case ValType.ID.I64:
                return "0L";
            case ValType.ID.F32:
                return "0.0f";
            case ValType.ID.F64:
                return "0.0";
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                return "0"; // ref null is represented as 0 in int
            default:
                throw new IllegalArgumentException("Unsupported ValType: " + type);
        }
    }

    /**
     * Get Java type name as string for a ValType.
     */
    public static String javaTypeName(ValType type) {
        return javaTypeName(jvmType(type));
    }

    /**
     * Get Java type name as string for a JVM type.
     */
    public static String javaTypeName(Class<?> jvmType) {
        if (jvmType == int.class) {
            return "int";
        } else if (jvmType == long.class) {
            return "long";
        } else if (jvmType == float.class) {
            return "float";
        } else if (jvmType == double.class) {
            return "double";
        } else if (jvmType == void.class) {
            return "void";
        } else {
            return jvmType.getName();
        }
    }

    /**
     * Convert JVM type to JavaParser type.
     */
    public static com.github.javaparser.ast.type.Type javaParserType(Class<?> jvmType) {
        if (jvmType == int.class) {
            return PrimitiveType.intType();
        } else if (jvmType == long.class) {
            return PrimitiveType.longType();
        } else if (jvmType == float.class) {
            return PrimitiveType.floatType();
        } else if (jvmType == double.class) {
            return PrimitiveType.doubleType();
        } else if (jvmType == void.class) {
            return StaticJavaParser.parseType("void");
        } else if (jvmType == long[].class) {
            return new ArrayType(PrimitiveType.longType());
        } else {
            return StaticJavaParser.parseType(jvmType.getName());
        }
    }

    /**
     * Generate source code to unbox long to JVM type (mirrors CompilerUtil.emitLongToJvm).
     */
    public static String unboxLongToJvm(String longExpr, ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                return "((int) " + longExpr + ")";
            case ValType.ID.I64:
                return longExpr;
            case ValType.ID.F32:
                return "com.dylibso.chicory.runtime.Value.longToFloat(" + longExpr + ")";
            case ValType.ID.F64:
                return "com.dylibso.chicory.runtime.Value.longToDouble(" + longExpr + ")";
            default:
                throw new IllegalArgumentException("Unsupported ValType: " + type);
        }
    }

    /**
     * Generate source code to box JVM type to long (mirrors CompilerUtil.emitJvmToLong).
     */
    public static String boxJvmToLong(String jvmExpr, ValType type) {
        switch (type.opcode()) {
            case ValType.ID.I32:
            case ValType.ID.Ref:
            case ValType.ID.RefNull:
            case ValType.ID.ExnRef:
                return "((long) " + jvmExpr + ")";
            case ValType.ID.I64:
                return jvmExpr;
            case ValType.ID.F32:
                return "com.dylibso.chicory.runtime.Value.floatToLong(" + jvmExpr + ")";
            case ValType.ID.F64:
                return "com.dylibso.chicory.runtime.Value.doubleToLong(" + jvmExpr + ")";
            default:
                throw new IllegalArgumentException("Unsupported ValType: " + type);
        }
    }
}
