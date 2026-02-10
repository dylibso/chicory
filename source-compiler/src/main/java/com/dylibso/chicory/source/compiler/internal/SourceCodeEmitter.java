package com.dylibso.chicory.source.compiler.internal;

import com.dylibso.chicory.wasm.WasmModule;
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
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
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
     * Generate Java source code for all functions in the module.
     * Mirrors the structure of the ASM compiler's compileFunction method.
     */
    public static String generateSource(
            String packageName,
            String className,
            WasmModule module,
            WasmAnalyzer analyzer,
            List<FunctionType> functionTypes,
            int functionImports) {

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

        // Generate func_xxx methods for all functions
        for (int funcId = 0; funcId < functionTypes.size(); funcId++) {
            FunctionType functionType = functionTypes.get(funcId);
            List<CompilerInstruction> instructions = analyzer.analyze(funcId);

            if (funcId < functionImports) {
                // Host function - delegate to instance
                generateHostFunctionMethod(clazz, funcId, functionType);
            } else {
                // Regular function - generate full implementation
                FunctionBody body = module.codeSection().getFunctionBody(funcId - functionImports);
                generateFunctionMethod(
                        clazz, funcId, functionType, instructions, body, functionTypes);
            }
        }

        // Generate call() method that dispatches to the appropriate func_xxx
        generateCallMethod(clazz, functionTypes, functionImports);

        return cu.toString();
    }

    /**
     * Generate the call() method that dispatches to the appropriate function.
     */
    private static void generateCallMethod(
            ClassOrInterfaceDeclaration clazz,
            List<FunctionType> functionTypes,
            int functionImports) {

        MethodDeclaration callMethod =
                clazz.addMethod("call", Modifier.Keyword.PUBLIC)
                        .setType(new ArrayType(PrimitiveType.longType()));
        callMethod
                .addParameter(PrimitiveType.intType(), "funcId")
                .addParameter(new ArrayType(PrimitiveType.longType()), "args");

        BlockStmt callBody = new BlockStmt();
        callMethod.setBody(callBody);

        callBody.addStatement(
                StaticJavaParser.parseStatement(
                        "com.dylibso.chicory.runtime.Memory memory = this.instance.memory();"));

        com.github.javaparser.ast.stmt.SwitchStmt switchStmt =
                new com.github.javaparser.ast.stmt.SwitchStmt();
        switchStmt.setSelector(new NameExpr("funcId"));

        // Host functions all share the same case (matching ASM compiler's hostLabel)
        com.github.javaparser.ast.stmt.SwitchEntry hostEntry = null;
        if (functionImports > 0) {
            hostEntry = new com.github.javaparser.ast.stmt.SwitchEntry();
            BlockStmt hostBody = new BlockStmt();
            MethodCallExpr hostCall = new MethodCallExpr();
            hostCall.setScope(
                    StaticJavaParser.parseExpression(
                            "com.dylibso.chicory.compiler.internal.Shaded"));
            hostCall.setName("callHostFunction");
            hostCall.addArgument(new FieldAccessExpr(new ThisExpr(), "instance"));
            hostCall.addArgument(new NameExpr("funcId"));
            hostCall.addArgument(new NameExpr("args"));
            hostBody.addStatement(new ReturnStmt(hostCall));
            hostEntry.getStatements().addAll(hostBody.getStatements());
        }

        for (int funcId = 0; funcId < functionTypes.size(); funcId++) {
            FunctionType functionType = functionTypes.get(funcId);

            com.github.javaparser.ast.stmt.SwitchEntry entry =
                    new com.github.javaparser.ast.stmt.SwitchEntry();
            entry.getLabels().add(new IntegerLiteralExpr(String.valueOf(funcId)));

            BlockStmt body = new BlockStmt();

            if (funcId < functionImports) {
                // Host function - use shared host entry
                if (hostEntry != null) {
                    entry.getStatements().addAll(hostEntry.getStatements());
                }
            } else {
                // Regular function - call func_xxx
                MethodCallExpr funcCall = new MethodCallExpr(new ThisExpr(), "func_" + funcId);
                for (int i = 0; i < functionType.params().size(); i++) {
                    ValType paramType = functionType.params().get(i);
                    String unboxExpr =
                            SourceCompilerUtil.unboxLongToJvm("args[" + i + "]", paramType);
                    funcCall.addArgument(StaticJavaParser.parseExpression(unboxExpr));
                }
                funcCall.addArgument(new NameExpr("memory"));
                funcCall.addArgument(new FieldAccessExpr(new ThisExpr(), "instance"));

                Class<?> returnType = SourceCompilerUtil.jvmReturnType(functionType);
                if (returnType == void.class) {
                    body.addStatement(new ExpressionStmt(funcCall));
                    body.addStatement(StaticJavaParser.parseStatement("return null;"));
                } else if (returnType == long[].class) {
                    body.addStatement(new ReturnStmt(funcCall));
                } else {
                    String varName = "result";
                    String varDecl =
                            SourceCompilerUtil.javaTypeName(returnType)
                                    + " "
                                    + varName
                                    + " = "
                                    + funcCall.toString()
                                    + ";";
                    body.addStatement(StaticJavaParser.parseStatement(varDecl));
                    body.addStatement(StaticJavaParser.parseStatement("long[] out = new long[1];"));
                    String boxExpr =
                            SourceCompilerUtil.boxJvmToLong(varName, functionType.returns().get(0));
                    body.addStatement(StaticJavaParser.parseStatement("out[0] = " + boxExpr + ";"));
                    body.addStatement(StaticJavaParser.parseStatement("return out;"));
                }
                entry.getStatements().addAll(body.getStatements());
            }

            switchStmt.getEntries().add(entry);
        }

        com.github.javaparser.ast.stmt.SwitchEntry defaultEntry =
                new com.github.javaparser.ast.stmt.SwitchEntry();
        MethodCallExpr throwCall = new MethodCallExpr();
        throwCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        throwCall.setName("throwUnknownFunction");
        throwCall.addArgument(new NameExpr("funcId"));
        defaultEntry.getStatements().add(new ThrowStmt(throwCall));
        switchStmt.getEntries().add(defaultEntry);

        callBody.addStatement(switchStmt);
    }

    /**
     * Generate a host function method that delegates to instance.callHostFunction().
     */
    private static void generateHostFunctionMethod(
            ClassOrInterfaceDeclaration clazz, int funcId, FunctionType functionType) {

        MethodDeclaration method =
                clazz.addMethod("func_" + funcId, Modifier.Keyword.PRIVATE)
                        .setType(new ArrayType(PrimitiveType.longType()));

        // Add individual typed parameters
        for (int i = 0; i < functionType.params().size(); i++) {
            ValType paramType = functionType.params().get(i);
            method.addParameter(
                    SourceCompilerUtil.javaParserType(SourceCompilerUtil.jvmType(paramType)),
                    "arg" + i);
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

        // Box arguments into long[]
        block.addStatement(
                StaticJavaParser.parseStatement(
                        "long[] boxedArgs = new long[" + functionType.params().size() + "];"));
        for (int i = 0; i < functionType.params().size(); i++) {
            ValType paramType = functionType.params().get(i);
            String boxExpr = SourceCompilerUtil.boxJvmToLong("arg" + i, paramType);
            block.addStatement(
                    StaticJavaParser.parseStatement("boxedArgs[" + i + "] = " + boxExpr + ";"));
        }

        // Call instance's host function handler
        block.addStatement(
                StaticJavaParser.parseStatement(
                        "var imprt = instance.imports().function(" + funcId + ");"));
        block.addStatement(
                StaticJavaParser.parseStatement(
                        "return imprt.handle().apply(instance, boxedArgs);"));
    }

    private static void generateFunctionMethod(
            ClassOrInterfaceDeclaration clazz,
            int funcId,
            FunctionType functionType,
            List<CompilerInstruction> instructions,
            FunctionBody body,
            List<FunctionType> allFunctionTypes) {

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
        boolean hasExplicitReturn = false;

        // Emit instructions - mirror the ASM compiler structure
        for (CompilerInstruction ins : instructions) {
            if (ins.opcode() == CompilerOpCode.RETURN) {
                hasExplicitReturn = true;
            }
            emitInstruction(ins, block, stack, localVarNames, functionType, allFunctionTypes);
        }

        // Implicit return at end of function (if no explicit RETURN was encountered)
        if (!hasExplicitReturn) {
            Class<?> implicitReturnType = SourceCompilerUtil.jvmReturnType(functionType);
            if (implicitReturnType == void.class) {
                if (!stack.isEmpty()) {
                    stack.pop(); // pop and discard
                }
                block.addStatement(StaticJavaParser.parseStatement("return;"));
            } else if (implicitReturnType == long[].class) {
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
                    block.addStatement(
                            StaticJavaParser.parseStatement("return " + defaultValue + ";"));
                }
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
            List<String> localVarNames,
            FunctionType functionType,
            List<FunctionType> allFunctionTypes) {
        CompilerOpCode op = ins.opcode();
        switch (op) {
            case I32_CONST:
                I32_CONST(ins, stack);
                break;
            case I32_ADD:
                I32_ADD(ins, stack);
                break;
            case I32_SUB:
                I32_SUB(ins, stack);
                break;
            case I32_MUL:
                I32_MUL(ins, stack);
                break;
            case I32_DIV_S:
                I32_DIV_S(ins, stack);
                break;
            case I32_DIV_U:
                I32_DIV_U(ins, stack);
                break;
            case I32_REM_S:
                I32_REM_S(ins, stack);
                break;
            case I32_REM_U:
                I32_REM_U(ins, stack);
                break;
            case I32_AND:
                I32_AND(ins, stack);
                break;
            case I32_OR:
                I32_OR(ins, stack);
                break;
            case I32_XOR:
                I32_XOR(ins, stack);
                break;
            case I32_SHL:
                I32_SHL(ins, stack);
                break;
            case I32_SHR_S:
                I32_SHR_S(ins, stack);
                break;
            case I32_SHR_U:
                I32_SHR_U(ins, stack);
                break;
            case I32_ROTL:
                I32_ROTL(ins, stack);
                break;
            case I32_ROTR:
                I32_ROTR(ins, stack);
                break;
            case I32_CLZ:
                I32_CLZ(ins, stack);
                break;
            case I32_CTZ:
                I32_CTZ(ins, stack);
                break;
            case I32_POPCNT:
                I32_POPCNT(ins, stack);
                break;
            case I32_EXTEND_8_S:
                I32_EXTEND_8_S(ins, stack);
                break;
            case I32_EXTEND_16_S:
                I32_EXTEND_16_S(ins, stack);
                break;
            case I32_EQZ:
                I32_EQZ(ins, stack);
                break;
            case I32_EQ:
                I32_EQ(ins, stack);
                break;
            case I32_NE:
                I32_NE(ins, stack);
                break;
            case I32_LT_S:
                I32_LT_S(ins, stack);
                break;
            case I32_LT_U:
                I32_LT_U(ins, stack);
                break;
            case I32_GT_S:
                I32_GT_S(ins, stack);
                break;
            case I32_GT_U:
                I32_GT_U(ins, stack);
                break;
            case I32_LE_S:
                I32_LE_S(ins, stack);
                break;
            case I32_LE_U:
                I32_LE_U(ins, stack);
                break;
            case I32_GE_S:
                I32_GE_S(ins, stack);
                break;
            case I32_GE_U:
                I32_GE_U(ins, stack);
                break;
            case LOCAL_GET:
                LOCAL_GET(ins, stack, localVarNames);
                break;
            case LOCAL_SET:
                LOCAL_SET(ins, block, stack, localVarNames);
                break;
            case LOCAL_TEE:
                LOCAL_TEE(ins, block, stack, localVarNames);
                break;
            case CALL:
                CALL(ins, block, stack, allFunctionTypes);
                break;
            case RETURN:
                RETURN(ins, block, stack, functionType);
                break;
            case I32_LOAD:
                I32_LOAD(ins, block, stack);
                break;
            case I32_STORE:
                I32_STORE(ins, block, stack);
                break;
            case I32_LOAD8_S:
                I32_LOAD8_S(ins, block, stack);
                break;
            case I32_LOAD8_U:
                I32_LOAD8_U(ins, block, stack);
                break;
            case I32_LOAD16_S:
                I32_LOAD16_S(ins, block, stack);
                break;
            case I32_LOAD16_U:
                I32_LOAD16_U(ins, block, stack);
                break;
            case I32_STORE8:
                I32_STORE8(ins, block, stack);
                break;
            case I32_STORE16:
                I32_STORE16(ins, block, stack);
                break;
            case I64_CONST:
                I64_CONST(ins, stack);
                break;
            case GLOBAL_GET:
                GLOBAL_GET(ins, block, stack);
                break;
            case GLOBAL_SET:
                GLOBAL_SET(ins, block, stack);
                break;
            case DROP:
                DROP(ins, stack);
                break;
            case SELECT:
                SELECT(ins, block, stack);
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
     * Emit I32_SUB: pop two values, subtract them, push result
     */
    public static void I32_SUB(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.MINUS));
    }

    /**
     * Emit I32_MUL: pop two values, multiply them, push result
     */
    public static void I32_MUL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.MULTIPLY));
    }

    /**
     * Emit I32_DIV_S: signed division
     */
    public static void I32_DIV_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.DIVIDE));
    }

    /**
     * Emit I32_DIV_U: unsigned division
     */
    public static void I32_DIV_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        // For unsigned division, we need to use Integer.divideUnsigned
        MethodCallExpr divCall = new MethodCallExpr();
        divCall.setScope(StaticJavaParser.parseExpression("Integer"));
        divCall.setName("divideUnsigned");
        divCall.addArgument(a);
        divCall.addArgument(b);
        stack.push(divCall);
    }

    /**
     * Emit I32_REM_S: signed remainder
     */
    public static void I32_REM_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.REMAINDER));
    }

    /**
     * Emit I32_REM_U: unsigned remainder
     */
    public static void I32_REM_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        // For unsigned remainder, we need to use Integer.remainderUnsigned
        MethodCallExpr remCall = new MethodCallExpr();
        remCall.setScope(StaticJavaParser.parseExpression("Integer"));
        remCall.setName("remainderUnsigned");
        remCall.addArgument(a);
        remCall.addArgument(b);
        stack.push(remCall);
    }

    /**
     * Emit I32_AND: bitwise AND
     */
    public static void I32_AND(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.BINARY_AND));
    }

    /**
     * Emit I32_OR: bitwise OR
     */
    public static void I32_OR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.BINARY_OR));
    }

    /**
     * Emit I32_XOR: bitwise XOR
     */
    public static void I32_XOR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.XOR));
    }

    /**
     * Emit I32_SHL: left shift
     */
    public static void I32_SHL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.LEFT_SHIFT));
    }

    /**
     * Emit I32_SHR_S: signed right shift
     */
    public static void I32_SHR_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.SIGNED_RIGHT_SHIFT));
    }

    /**
     * Emit I32_SHR_U: unsigned right shift
     */
    public static void I32_SHR_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT));
    }

    /**
     * Emit I32_ROTL: rotate left
     */
    public static void I32_ROTL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr rotlCall = new MethodCallExpr();
        rotlCall.setScope(StaticJavaParser.parseExpression("Integer"));
        rotlCall.setName("rotateLeft");
        rotlCall.addArgument(a);
        rotlCall.addArgument(b);
        stack.push(rotlCall);
    }

    /**
     * Emit I32_ROTR: rotate right
     */
    public static void I32_ROTR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr rotrCall = new MethodCallExpr();
        rotrCall.setScope(StaticJavaParser.parseExpression("Integer"));
        rotrCall.setName("rotateRight");
        rotrCall.addArgument(a);
        rotrCall.addArgument(b);
        stack.push(rotrCall);
    }

    /**
     * Emit I32_CLZ: count leading zeros
     */
    public static void I32_CLZ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr clzCall = new MethodCallExpr();
        clzCall.setScope(StaticJavaParser.parseExpression("Integer"));
        clzCall.setName("numberOfLeadingZeros");
        clzCall.addArgument(a);
        stack.push(clzCall);
    }

    /**
     * Emit I32_CTZ: count trailing zeros
     */
    public static void I32_CTZ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr ctzCall = new MethodCallExpr();
        ctzCall.setScope(StaticJavaParser.parseExpression("Integer"));
        ctzCall.setName("numberOfTrailingZeros");
        ctzCall.addArgument(a);
        stack.push(ctzCall);
    }

    /**
     * Emit I32_POPCNT: population count (number of 1 bits)
     */
    public static void I32_POPCNT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr popcntCall = new MethodCallExpr();
        popcntCall.setScope(StaticJavaParser.parseExpression("Integer"));
        popcntCall.setName("bitCount");
        popcntCall.addArgument(a);
        stack.push(popcntCall);
    }

    public static void I32_EXTEND_8_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        CastExpr cast = new CastExpr(PrimitiveType.byteType(), a);
        stack.push(new CastExpr(PrimitiveType.intType(), cast));
    }

    public static void I32_EXTEND_16_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        CastExpr cast = new CastExpr(PrimitiveType.shortType(), a);
        stack.push(new CastExpr(PrimitiveType.intType(), cast));
    }

    public static void I32_EQZ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_EQZ");
        call.addArgument(a);
        stack.push(call);
    }

    public static void I32_EQ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_EQ");
        call.addArgument(b);
        call.addArgument(a);
        stack.push(call);
    }

    public static void I32_NE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_NE");
        call.addArgument(b);
        call.addArgument(a);
        stack.push(call);
    }

    public static void I32_LT_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_LT_S");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I32_LT_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_LT_U");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I32_GT_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_GT_S");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I32_GT_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_GT_U");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I32_LE_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_LE_S");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I32_LE_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_LE_U");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I32_GE_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_GE_S");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I32_GE_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_GE_U");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
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
     * Emit LOCAL_TEE: store value to local but keep it on stack
     * ASM: dup/dup2 then store
     * Java: assign to local, keep value on stack
     */
    public static void LOCAL_TEE(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            List<String> localVarNames) {
        int index = (int) ins.operand(0);
        String varName = localVarNames.get(index);
        com.github.javaparser.ast.expr.Expression value = stack.peek();
        block.addStatement(
                new ExpressionStmt(
                        new AssignExpr(new NameExpr(varName), value, AssignExpr.Operator.ASSIGN)));
        // Value stays on stack
    }

    /**
     * Emit CALL: call function by ID
     * ASM: calls call_xxx static method
     * Java: call this.call(funcId, args) and handle return value
     */
    public static void CALL(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            List<FunctionType> allFunctionTypes) {
        int funcId = (int) ins.operand(0);
        FunctionType calledFunctionType = allFunctionTypes.get(funcId);

        // Build args array from stack (pop in reverse order)
        int paramCount = calledFunctionType.params().size();
        if (paramCount == 0) {
            MethodCallExpr call = new MethodCallExpr(new ThisExpr(), "call");
            call.addArgument(new IntegerLiteralExpr(String.valueOf(funcId)));
            call.addArgument(StaticJavaParser.parseExpression("new long[0]"));
            com.github.javaparser.ast.expr.Expression result = call;

            // Handle return value
            if (calledFunctionType.returns().isEmpty()) {
                block.addStatement(new ExpressionStmt(result));
                // No return value
            } else if (calledFunctionType.returns().size() == 1) {
                // Single return value - unbox from long[]
                String varName = "callResult";
                block.addStatement(
                        StaticJavaParser.parseStatement(
                                "long[] " + varName + " = " + result.toString() + ";"));
                String unboxExpr =
                        SourceCompilerUtil.unboxLongToJvm(
                                varName + "[0]", calledFunctionType.returns().get(0));
                stack.push(StaticJavaParser.parseExpression(unboxExpr));
            } else {
                // Multiple return values - keep as long[]
                stack.push(result);
            }
        } else {
            // Build args array
            block.addStatement(
                    StaticJavaParser.parseStatement(
                            "long[] callArgs = new long[" + paramCount + "];"));
            for (int i = paramCount - 1; i >= 0; i--) {
                com.github.javaparser.ast.expr.Expression arg = stack.pop();
                String boxExpr =
                        SourceCompilerUtil.boxJvmToLong(
                                arg.toString(), calledFunctionType.params().get(i));
                block.addStatement(
                        StaticJavaParser.parseStatement("callArgs[" + i + "] = " + boxExpr + ";"));
            }

            MethodCallExpr call = new MethodCallExpr(new ThisExpr(), "call");
            call.addArgument(new IntegerLiteralExpr(String.valueOf(funcId)));
            call.addArgument(new NameExpr("callArgs"));
            com.github.javaparser.ast.expr.Expression result = call;

            // Handle return value
            if (calledFunctionType.returns().isEmpty()) {
                block.addStatement(new ExpressionStmt(result));
            } else if (calledFunctionType.returns().size() == 1) {
                String varName = "callResult";
                block.addStatement(
                        StaticJavaParser.parseStatement(
                                "long[] " + varName + " = " + result.toString() + ";"));
                String unboxExpr =
                        SourceCompilerUtil.unboxLongToJvm(
                                varName + "[0]", calledFunctionType.returns().get(0));
                stack.push(StaticJavaParser.parseExpression(unboxExpr));
            } else {
                stack.push(result);
            }
        }
    }

    /**
     * Emit RETURN: handle inline, matching ASM compiler behavior
     */
    public static void RETURN(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            FunctionType functionType) {
        Class<?> returnType = SourceCompilerUtil.jvmReturnType(functionType);

        if (returnType == void.class) {
            // Pop and discard any values on stack
            while (!stack.isEmpty()) {
                stack.pop();
            }
            block.addStatement(StaticJavaParser.parseStatement("return;"));
        } else if (returnType == long[].class) {
            // Multiple return values - box into long[]
            block.addStatement(
                    StaticJavaParser.parseStatement(
                            "long[] out = new long[" + functionType.returns().size() + "];"));
            for (int i = functionType.returns().size() - 1; i >= 0; i--) {
                com.github.javaparser.ast.expr.Expression resultExpr = stack.pop();
                String boxExpr =
                        SourceCompilerUtil.boxJvmToLong(
                                resultExpr.toString(), functionType.returns().get(i));
                block.addStatement(
                        StaticJavaParser.parseStatement("out[" + i + "] = " + boxExpr + ";"));
            }
            block.addStatement(new ReturnStmt(new NameExpr("out")));
        } else {
            // Single return value - return directly
            com.github.javaparser.ast.expr.Expression resultExpr = stack.pop();
            block.addStatement(new ReturnStmt(resultExpr));
        }
    }

    public static void I32_LOAD(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("memoryReadInt");
        call.addArgument(base);
        call.addArgument(new IntegerLiteralExpr(String.valueOf((int) offset)));
        call.addArgument(new NameExpr("memory"));
        stack.push(call);
    }

    public static void I32_STORE(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("memoryWriteInt");
        call.addArgument(base);
        call.addArgument(value);
        call.addArgument(new IntegerLiteralExpr(String.valueOf((int) offset)));
        call.addArgument(new NameExpr("memory"));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I32_LOAD8_S(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("memoryReadByte");
        call.addArgument(base);
        call.addArgument(new IntegerLiteralExpr(String.valueOf((int) offset)));
        call.addArgument(new NameExpr("memory"));
        // Sign extend byte to int
        CastExpr cast = new CastExpr(PrimitiveType.byteType(), call);
        stack.push(new CastExpr(PrimitiveType.intType(), cast));
    }

    public static void I32_LOAD8_U(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("memoryReadByte");
        call.addArgument(base);
        call.addArgument(new IntegerLiteralExpr(String.valueOf((int) offset)));
        call.addArgument(new NameExpr("memory"));
        // Unsigned: mask with 0xFF
        BinaryExpr masked =
                new BinaryExpr(
                        new CastExpr(
                                PrimitiveType.intType(),
                                new CastExpr(PrimitiveType.byteType(), call)),
                        new IntegerLiteralExpr("0xFF"),
                        BinaryExpr.Operator.BINARY_AND);
        stack.push(masked);
    }

    public static void I32_LOAD16_S(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("memoryReadShort");
        call.addArgument(base);
        call.addArgument(new IntegerLiteralExpr(String.valueOf((int) offset)));
        call.addArgument(new NameExpr("memory"));
        // Sign extend short to int
        CastExpr cast = new CastExpr(PrimitiveType.shortType(), call);
        stack.push(new CastExpr(PrimitiveType.intType(), cast));
    }

    public static void I32_LOAD16_U(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("memoryReadShort");
        call.addArgument(base);
        call.addArgument(new IntegerLiteralExpr(String.valueOf((int) offset)));
        call.addArgument(new NameExpr("memory"));
        // Unsigned: mask with 0xFFFF
        BinaryExpr masked =
                new BinaryExpr(
                        new CastExpr(
                                PrimitiveType.intType(),
                                new CastExpr(PrimitiveType.shortType(), call)),
                        new IntegerLiteralExpr("0xFFFF"),
                        BinaryExpr.Operator.BINARY_AND);
        stack.push(masked);
    }

    public static void I32_STORE8(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        CastExpr byteValue = new CastExpr(PrimitiveType.byteType(), value);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("memoryWriteByte");
        call.addArgument(base);
        call.addArgument(byteValue);
        call.addArgument(new IntegerLiteralExpr(String.valueOf((int) offset)));
        call.addArgument(new NameExpr("memory"));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I32_STORE16(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        CastExpr shortValue = new CastExpr(PrimitiveType.shortType(), value);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("memoryWriteShort");
        call.addArgument(base);
        call.addArgument(shortValue);
        call.addArgument(new IntegerLiteralExpr(String.valueOf((int) offset)));
        call.addArgument(new NameExpr("memory"));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I64_CONST(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        long value = ins.operand(0);
        stack.push(new com.github.javaparser.ast.expr.LongLiteralExpr(String.valueOf(value) + "L"));
    }

    public static void GLOBAL_GET(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        int globalIndex = (int) ins.operand(0);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("readGlobal");
        call.addArgument(new IntegerLiteralExpr(String.valueOf(globalIndex)));
        call.addArgument(new FieldAccessExpr(new ThisExpr(), "instance"));
        stack.push(call);
    }

    public static void GLOBAL_SET(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        int globalIndex = (int) ins.operand(0);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.compiler.internal.Shaded"));
        call.setName("writeGlobal");
        call.addArgument(value);
        call.addArgument(new IntegerLiteralExpr(String.valueOf(globalIndex)));
        call.addArgument(new FieldAccessExpr(new ThisExpr(), "instance"));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void DROP(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static void SELECT(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression falseValue = stack.pop();
        com.github.javaparser.ast.expr.Expression trueValue = stack.pop();
        com.github.javaparser.ast.expr.Expression condition = stack.pop();
        // SELECT: if condition != 0 then trueValue else falseValue
        BinaryExpr condExpr =
                new BinaryExpr(
                        condition, new IntegerLiteralExpr("0"), BinaryExpr.Operator.NOT_EQUALS);
        com.github.javaparser.ast.expr.ConditionalExpr ternary =
                new com.github.javaparser.ast.expr.ConditionalExpr(condExpr, trueValue, falseValue);
        stack.push(ternary);
    }
}
