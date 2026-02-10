package com.dylibso.chicory.source.compiler.internal;

import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Source code emitters that mirror the ASM Emitters structure.
 * Uses Java expressions to naturally leverage the Java operand stack,
 * just like the ASM compiler uses the JVM operand stack.
 */
final class SourceCodeEmitter {

    private SourceCodeEmitter() {}

    /**
     * Generate Java source code for a simple function.
     * Mirrors the structure of the ASM compiler's compileFunction method.
     */
    public static String generateSource(
            String packageName,
            String className,
            int funcId,
            FunctionType functionType,
            List<CompilerInstruction> instructions,
            WasmModule module,
            WasmAnalyzer analyzer) {

        CompilationUnit cu = new CompilationUnit(packageName);
        ClassOrInterfaceDeclaration clazz =
                cu.addClass(className, Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL);

        // implements Machine
        ClassOrInterfaceType machineType =
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Machine");
        clazz.addImplementedType(machineType);

        // field: private final Instance instance;
        clazz.addField(
                "com.dylibso.chicory.runtime.Instance",
                "instance",
                Modifier.Keyword.PRIVATE,
                Modifier.Keyword.FINAL);

        // constructor: public C(Instance instance) { this.instance = instance; }
        var ctor =
                clazz.addConstructor(Modifier.Keyword.PUBLIC)
                        .addParameter("com.dylibso.chicory.runtime.Instance", "instance")
                        .createBody();
        ctor.addStatement(
                new ExpressionStmt(
                        new AssignExpr(
                                new FieldAccessExpr(new ThisExpr(), "instance"),
                                new NameExpr("instance"),
                                AssignExpr.Operator.ASSIGN)));

        // method: public long[] call(int funcId, long[] args)
        MethodDeclaration callMethod =
                clazz.addMethod("call", Modifier.Keyword.PUBLIC)
                        .setType(new ArrayType(PrimitiveType.longType()));
        callMethod
                .addParameter(PrimitiveType.intType(), "funcId")
                .addParameter(new ArrayType(PrimitiveType.longType()), "args");

        BlockStmt callBody = new BlockStmt();
        callMethod.setBody(callBody);

        // For now, only handle funcId == functionImports with simple I32_ADD
        int functionImports = module.importSection().count(ExternalType.FUNCTION);
        if (funcId == functionImports) {
            // Unbox long[] args to individual parameters and get Memory/Instance
            // Get Memory from instance
            callBody.addStatement(
                    StaticJavaParser.parseStatement(
                            "com.dylibso.chicory.runtime.Memory memory = this.instance.memory();"));

            // Build method call with unboxed parameters
            MethodCallExpr funcCall = new MethodCallExpr(new ThisExpr(), "func_" + funcId);

            // Add individual typed parameters (unbox from long[])
            for (int i = 0; i < functionType.params().size(); i++) {
                ValType paramType = functionType.params().get(i);
                String unboxExpr = SourceCompilerUtil.unboxLongToJvm("args[" + i + "]", paramType);
                funcCall.addArgument(StaticJavaParser.parseExpression(unboxExpr));
            }

            // Add Memory and Instance parameters
            funcCall.addArgument(new NameExpr("memory"));
            funcCall.addArgument(new FieldAccessExpr(new ThisExpr(), "instance"));

            // Box result back to long[] if needed
            Class<?> returnType = SourceCompilerUtil.jvmReturnType(functionType);
            if (returnType == void.class) {
                callBody.addStatement(new ExpressionStmt(funcCall));
                callBody.addStatement(StaticJavaParser.parseStatement("return null;"));
            } else if (returnType == long[].class) {
                callBody.addStatement(new ReturnStmt(funcCall));
            } else {
                // Box single return value to long[]
                String varName = "result";
                // Create variable declaration with method call using parseStatement
                String varDecl =
                        SourceCompilerUtil.javaTypeName(returnType)
                                + " "
                                + varName
                                + " = "
                                + funcCall.toString()
                                + ";";
                callBody.addStatement(StaticJavaParser.parseStatement(varDecl));
                callBody.addStatement(StaticJavaParser.parseStatement("long[] out = new long[1];"));
                String boxExpr =
                        SourceCompilerUtil.boxJvmToLong(varName, functionType.returns().get(0));
                callBody.addStatement(StaticJavaParser.parseStatement("out[0] = " + boxExpr + ";"));
                callBody.addStatement(StaticJavaParser.parseStatement("return out;"));
            }
        } else {
            // throw new IllegalArgumentException("Unknown function " + funcId);
            callBody.addStatement(
                    StaticJavaParser.parseStatement(
                            "throw new IllegalArgumentException(\"Unknown function \" + funcId);"));
        }

        // Generate the actual function method
        FunctionBody body = module.codeSection().getFunctionBody(funcId - functionImports);
        generateFunctionMethod(clazz, funcId, functionType, instructions, body);

        return cu.toString();
    }

    private static void generateFunctionMethod(
            ClassOrInterfaceDeclaration clazz,
            int funcId,
            FunctionType functionType,
            List<CompilerInstruction> instructions,
            FunctionBody body) {

        // Method signature: private <ReturnType> func_0(<TypeN> argN..., Memory memory, Instance
        // instance)
        // Mirrors ASM compiler: func_xxx takes individual typed parameters + Memory + Instance
        MethodDeclaration method = clazz.addMethod("func_" + funcId, Modifier.Keyword.PRIVATE);

        // Set return type based on function type
        Class<?> returnType = SourceCompilerUtil.jvmReturnType(functionType);
        if (returnType == void.class) {
            method.setType(StaticJavaParser.parseType("void"));
        } else if (returnType == long[].class) {
            method.setType(new ArrayType(PrimitiveType.longType()));
        } else {
            method.setType(SourceCompilerUtil.javaParserType(returnType));
        }

        // Add individual typed parameters
        List<String> localVarNames = new ArrayList<>();
        for (int i = 0; i < functionType.params().size(); i++) {
            ValType paramType = functionType.params().get(i);
            String paramName = "arg" + i;
            localVarNames.add(paramName);
            method.addParameter(
                    SourceCompilerUtil.javaParserType(SourceCompilerUtil.jvmType(paramType)),
                    paramName);
        }

        // Add Memory and Instance parameters
        method.addParameter(
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Memory"),
                "memory");
        method.addParameter(
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Instance"),
                "instance");

        BlockStmt block = new BlockStmt();
        method.setBody(block);

        // Initialize WASM local variables to default values (mirrors ASM compiler)
        for (int i = functionType.params().size();
                i < functionType.params().size() + body.localTypes().size();
                i++) {
            ValType localType = body.localTypes().get(i - functionType.params().size());
            String varName = "local" + i;
            localVarNames.add(varName);
            String defaultValue = SourceCompilerUtil.defaultValue(localType);
            block.addStatement(
                    StaticJavaParser.parseStatement(
                            SourceCompilerUtil.javaTypeName(localType)
                                    + " "
                                    + varName
                                    + " = "
                                    + defaultValue
                                    + ";"));
        }

        // Stack of expressions - represents the Java operand stack
        Deque<com.github.javaparser.ast.expr.Expression> stack = new ArrayDeque<>();

        // Emit instructions - mirror the ASM compiler structure
        for (CompilerInstruction ins : instructions) {
            emitInstruction(ins, block, stack, localVarNames);
        }

        // Return result - handle based on return type
        // Mirrors ASM compiler's return handling
        if (returnType == void.class) {
            if (!stack.isEmpty()) {
                stack.pop(); // pop and discard
            }
            block.addStatement(StaticJavaParser.parseStatement("return;"));
        } else if (returnType == long[].class) {
            // Multiple return values - box into long[]
            block.addStatement(
                    StaticJavaParser.parseStatement(
                            "long[] out = new long[" + functionType.returns().size() + "];"));
            for (int i = 0; i < functionType.returns().size(); i++) {
                com.github.javaparser.ast.expr.Expression resultExpr = stack.pop();
                String boxExpr =
                        SourceCompilerUtil.boxJvmToLong(
                                resultExpr.toString(), functionType.returns().get(i));
                block.addStatement(
                        StaticJavaParser.parseStatement("out[" + i + "] = " + boxExpr + ";"));
            }
            block.addStatement(new ReturnStmt(new NameExpr("out")));
        } else {
            // Single return value - return directly (not boxed)
            if (!stack.isEmpty()) {
                com.github.javaparser.ast.expr.Expression resultExpr = stack.pop();
                block.addStatement(new ReturnStmt(resultExpr));
            } else {
                // Default return value
                String defaultValue =
                        SourceCompilerUtil.defaultValue(functionType.returns().get(0));
                block.addStatement(StaticJavaParser.parseStatement("return " + defaultValue + ";"));
            }
        }
    }

    /**
     * Emit instruction - mirrors the ASM compiler's instruction handling.
     * Uses a stack of expressions to represent the Java operand stack.
     */
    private static void emitInstruction(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            List<String> localVarNames) {
        CompilerOpCode op = ins.opcode();
        switch (op) {
            case I32_CONST:
                I32_CONST(ins, stack);
                break;
            case I32_ADD:
                I32_ADD(ins, stack);
                break;
            case LOCAL_GET:
                LOCAL_GET(ins, stack, localVarNames);
                break;
            case LOCAL_SET:
                LOCAL_SET(ins, block, stack, localVarNames);
                break;
            case RETURN:
                RETURN(ins, block, stack);
                break;
            default:
                throw new IllegalArgumentException("Unsupported opcode: " + op);
        }
    }

    /**
     * Emit I32_CONST: push constant onto stack
     * ASM: asm.iconst(value) or asm.visitLdcInsn(value)
     * Java: push IntegerLiteralExpr onto expression stack
     */
    public static void I32_CONST(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        int value = (int) ins.operand(0);
        stack.push(new IntegerLiteralExpr(String.valueOf(value)));
    }

    /**
     * Emit I32_ADD: pop two values, add them, push result
     * ASM: asm.visitInsn(Opcodes.IADD) - assumes two ints are on stack
     * Java: pop two expressions, create BinaryExpr with +, push result
     */
    public static void I32_ADD(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.PLUS));
    }

    /**
     * Emit LOCAL_GET: load local variable onto stack
     * ASM: asm.load(ctx.localSlotIndex(loadIndex), asmType(localType))
     * Java: push NameExpr of local variable onto expression stack
     */
    public static void LOCAL_GET(
            CompilerInstruction ins,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            List<String> localVarNames) {
        int localIndex = (int) ins.operand(0);
        String varName = localVarNames.get(localIndex);
        stack.push(new NameExpr(varName));
    }

    /**
     * Emit LOCAL_SET: pop value from stack, store to local
     * ASM: asm.store(ctx.localSlotIndex(index), asmType(localType))
     * Java: pop expression, assign to local variable
     */
    public static void LOCAL_SET(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            List<String> localVarNames) {
        int index = (int) ins.operand(0);
        String varName = localVarNames.get(index);
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        block.addStatement(
                new ExpressionStmt(
                        new AssignExpr(new NameExpr(varName), value, AssignExpr.Operator.ASSIGN)));
    }

    /**
     * Emit RETURN: handled at the end of function
     */
    public static void RETURN(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        // Return is handled at the end of the function
    }
}
