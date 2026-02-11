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
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // Generate func_xxx methods for all functions (public static, matching ASM compiler)
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
                // Generate call_xxx bridge method (matching ASM compiler)
                generateCallFunctionMethod(clazz, className, funcId, functionType);
            }
        }

        // Generate call() method that dispatches to the appropriate func_xxx
        generateCallMethod(clazz, className, functionTypes, functionImports);

        return cu.toString();
    }

    /**
     * Generate the call() method that dispatches to the appropriate function.
     */
    private static void generateCallMethod(
            ClassOrInterfaceDeclaration clazz,
            String className,
            List<FunctionType> functionTypes,
            int functionImports) {

        MethodDeclaration callMethodDecl =
                clazz.addMethod("call", Modifier.Keyword.PUBLIC)
                        .setType(new ArrayType(PrimitiveType.longType()));
        callMethodDecl
                .addParameter(PrimitiveType.intType(), "funcId")
                .addParameter(new ArrayType(PrimitiveType.longType()), "args");

        BlockStmt callBody = new BlockStmt();
        callMethodDecl.setBody(callBody);

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
            // Inline callHostFunction: instance.imports().function(funcId).handle().apply(instance,
            // args)
            MethodCallExpr importsCall = new MethodCallExpr();
            importsCall.setScope(new FieldAccessExpr(new ThisExpr(), "instance"));
            importsCall.setName("imports");
            MethodCallExpr functionCall = new MethodCallExpr();
            functionCall.setScope(importsCall);
            functionCall.setName("function");
            functionCall.addArgument(new NameExpr("funcId"));
            MethodCallExpr handleCall = new MethodCallExpr();
            handleCall.setScope(functionCall);
            handleCall.setName("handle");
            MethodCallExpr applyCall = new MethodCallExpr();
            applyCall.setScope(handleCall);
            applyCall.setName("apply");
            applyCall.addArgument(new FieldAccessExpr(new ThisExpr(), "instance"));
            applyCall.addArgument(new NameExpr("args"));
            hostBody.addStatement(new ReturnStmt(applyCall));
            hostEntry.getStatements().addAll(hostBody.getStatements());
        }

        for (int funcId = 0; funcId < functionTypes.size(); funcId++) {
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
                // Regular function - call static call_xxx method (matching ASM compiler)
                MethodCallExpr callMethodExpr = new MethodCallExpr();
                callMethodExpr.setScope(StaticJavaParser.parseExpression(className));
                callMethodExpr.setName("call_" + funcId);
                callMethodExpr.addArgument(new NameExpr("memory"));
                callMethodExpr.addArgument(new FieldAccessExpr(new ThisExpr(), "instance"));
                callMethodExpr.addArgument(new NameExpr("args"));
                // call_xxx returns long[] directly (already boxed)
                body.addStatement(new ReturnStmt(callMethodExpr));
                entry.getStatements().addAll(body.getStatements());
            }

            switchStmt.getEntries().add(entry);
        }

        com.github.javaparser.ast.stmt.SwitchEntry defaultEntry =
                new com.github.javaparser.ast.stmt.SwitchEntry();
        // Inline throwUnknownFunction: throw new InvalidException("unknown function " + funcId)
        MethodCallExpr formatCall = new MethodCallExpr();
        formatCall.setScope(StaticJavaParser.parseExpression("String"));
        formatCall.setName("format");
        formatCall.addArgument(
                new com.github.javaparser.ast.expr.StringLiteralExpr("unknown function %d"));
        formatCall.addArgument(new NameExpr("funcId"));
        com.github.javaparser.ast.expr.ObjectCreationExpr exception =
                new com.github.javaparser.ast.expr.ObjectCreationExpr();
        exception.setType(
                StaticJavaParser.parseClassOrInterfaceType(
                        "com.dylibso.chicory.wasm.InvalidException"));
        exception.addArgument(formatCall);
        defaultEntry.getStatements().add(new ThrowStmt(exception));
        switchStmt.getEntries().add(defaultEntry);

        callBody.addStatement(switchStmt);
    }

    /**
     * Generate a host function method that delegates to instance.callHostFunction().
     */
    private static void generateHostFunctionMethod(
            ClassOrInterfaceDeclaration clazz, int funcId, FunctionType functionType) {

        // Host functions are public static, matching ASM compiler
        MethodDeclaration method =
                clazz.addMethod("func_" + funcId, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
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

        // Method signature: public static <ReturnType> func_xxx(<TypeN> argN..., Memory memory,
        // Instance instance)
        // Mirrors ASM compiler: func_xxx is public static with individual typed parameters + Memory
        // + Instance
        MethodDeclaration method =
                clazz.addMethod("func_" + funcId, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);

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

        // Allocate labels for all label targets (matching ASM compiler)
        Map<Long, String> labelNames = new HashMap<>();
        for (CompilerInstruction ins : instructions) {
            for (long target : ins.labelTargets()) {
                if (!labelNames.containsKey(target)) {
                    labelNames.put(target, "label_" + target);
                }
            }
        }

        // Track visited targets to detect backward jumps (matching ASM compiler)
        Set<Long> visitedTargets = new HashSet<>();

        // Track current block for proper label scoping (Java break label; needs enclosing blocks)
        BlockStmt currentBlock = block;

        // Stack of expressions - represents the Java operand stack
        Deque<com.github.javaparser.ast.expr.Expression> stack = new ArrayDeque<>();
        boolean hasExplicitReturn = false;

        // Emit instructions - mirror the ASM compiler structure 1:1
        for (CompilerInstruction ins : instructions) {
            if (ins.opcode() == CompilerOpCode.RETURN) {
                hasExplicitReturn = true;
            }

            // Handle LABEL by creating labeled block and switching current block
            if (ins.opcode() == CompilerOpCode.LABEL) {
                long target = ins.operand(0);
                String labelName = labelNames.get(target);
                if (labelName != null) {
                    visitedTargets.add(target);
                    // Create labeled block - subsequent code goes here (matching ASM compiler's
                    // flat structure)
                    BlockStmt labeledBlock = new BlockStmt();
                    LabeledStmt labeledStmt = new LabeledStmt();
                    labeledStmt.setLabel(new SimpleName(labelName));
                    labeledStmt.setStatement(labeledBlock);
                    currentBlock.addStatement(labeledStmt);
                    currentBlock = labeledBlock; // Switch to adding to labeled block
                }
                continue;
            }

            emitInstruction(
                    ins,
                    currentBlock,
                    stack,
                    localVarNames,
                    functionType,
                    allFunctionTypes,
                    labelNames,
                    visitedTargets);
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
     * Generate call_xxx bridge method: public static long[] call_xxx(Memory memory, Instance instance, long[] args)
     * Mirrors ASM compiler's compileCallFunction method.
     */
    private static void generateCallFunctionMethod(
            ClassOrInterfaceDeclaration clazz,
            String className,
            int funcId,
            FunctionType functionType) {
        MethodDeclaration method =
                clazz.addMethod("call_" + funcId, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
                        .setType(new ArrayType(PrimitiveType.longType()));
        method.addParameter(
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Memory"),
                "memory");
        method.addParameter(
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Instance"),
                "instance");
        method.addParameter(new ArrayType(PrimitiveType.longType()), "args");

        BlockStmt block = new BlockStmt();
        method.setBody(block);

        // Unbox arguments from long[]
        if (SourceCompilerUtil.hasTooManyParameters(functionType)) {
            // For functions with too many parameters, pass long[] directly
            MethodCallExpr funcCall = new MethodCallExpr();
            funcCall.setScope(StaticJavaParser.parseExpression(className));
            funcCall.setName("func_" + funcId);
            funcCall.addArgument(new NameExpr("args"));
            funcCall.addArgument(new NameExpr("memory"));
            funcCall.addArgument(new NameExpr("instance"));
            block.addStatement(new ReturnStmt(funcCall));
        } else {
            // Unbox arguments
            for (int i = 0; i < functionType.params().size(); i++) {
                ValType paramType = functionType.params().get(i);
                String unboxExpr = SourceCompilerUtil.unboxLongToJvm("args[" + i + "]", paramType);
                String paramName = "arg" + i;
                block.addStatement(
                        StaticJavaParser.parseStatement(
                                SourceCompilerUtil.javaTypeName(paramType)
                                        + " "
                                        + paramName
                                        + " = "
                                        + unboxExpr
                                        + ";"));
            }

            // Call func_xxx
            MethodCallExpr funcCall = new MethodCallExpr();
            funcCall.setScope(StaticJavaParser.parseExpression(className));
            funcCall.setName("func_" + funcId);
            for (int i = 0; i < functionType.params().size(); i++) {
                funcCall.addArgument(new NameExpr("arg" + i));
            }
            funcCall.addArgument(new NameExpr("memory"));
            funcCall.addArgument(new NameExpr("instance"));

            // Box result into long[]
            Class<?> returnType = SourceCompilerUtil.jvmReturnType(functionType);
            if (returnType == void.class) {
                block.addStatement(new ExpressionStmt(funcCall));
                block.addStatement(StaticJavaParser.parseStatement("return null;"));
            } else if (returnType == long[].class) {
                block.addStatement(new ReturnStmt(funcCall));
            } else {
                String varName = "result";
                String varDecl =
                        SourceCompilerUtil.javaTypeName(returnType)
                                + " "
                                + varName
                                + " = "
                                + funcCall.toString()
                                + ";";
                block.addStatement(StaticJavaParser.parseStatement(varDecl));
                block.addStatement(StaticJavaParser.parseStatement("long[] out = new long[1];"));
                String boxExpr =
                        SourceCompilerUtil.boxJvmToLong(varName, functionType.returns().get(0));
                block.addStatement(StaticJavaParser.parseStatement("out[0] = " + boxExpr + ";"));
                block.addStatement(StaticJavaParser.parseStatement("return out;"));
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
            List<FunctionType> allFunctionTypes,
            Map<Long, String> labelNames,
            Set<Long> visitedTargets) {
        CompilerOpCode op = ins.opcode();
        switch (op) {
            case LABEL:
                LABEL(ins, block, labelNames, visitedTargets);
                break;
            case GOTO:
                GOTO(ins, block, labelNames, visitedTargets);
                break;
            case IFEQ:
                IFEQ(ins, block, stack, labelNames, visitedTargets);
                break;
            case IFNE:
                IFNE(ins, block, stack, labelNames, visitedTargets);
                break;
            case SWITCH:
                SWITCH(ins, block, stack, labelNames, visitedTargets);
                break;
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
            case I64_ADD:
                I64_ADD(ins, stack);
                break;
            case I64_SUB:
                I64_SUB(ins, stack);
                break;
            case I64_MUL:
                I64_MUL(ins, stack);
                break;
            case I64_EQZ:
                I64_EQZ(ins, stack);
                break;
            case I64_EQ:
                I64_EQ(ins, stack);
                break;
            case I64_NE:
                I64_NE(ins, stack);
                break;
            case I64_LT_S:
                I64_LT_S(ins, stack);
                break;
            case I64_LT_U:
                I64_LT_U(ins, stack);
                break;
            case I64_GT_S:
                I64_GT_S(ins, stack);
                break;
            case I64_GT_U:
                I64_GT_U(ins, stack);
                break;
            case I64_LE_S:
                I64_LE_S(ins, stack);
                break;
            case I64_LE_U:
                I64_LE_U(ins, stack);
                break;
            case I64_GE_S:
                I64_GE_S(ins, stack);
                break;
            case I64_GE_U:
                I64_GE_U(ins, stack);
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
            case I64_LOAD:
                I64_LOAD(ins, block, stack);
                break;
            case I64_LOAD8_S:
                I64_LOAD8_S(ins, block, stack);
                break;
            case I64_LOAD8_U:
                I64_LOAD8_U(ins, block, stack);
                break;
            case I64_LOAD16_S:
                I64_LOAD16_S(ins, block, stack);
                break;
            case I64_LOAD16_U:
                I64_LOAD16_U(ins, block, stack);
                break;
            case I64_LOAD32_S:
                I64_LOAD32_S(ins, block, stack);
                break;
            case I64_LOAD32_U:
                I64_LOAD32_U(ins, block, stack);
                break;
            case I64_STORE:
                I64_STORE(ins, block, stack);
                break;
            case I64_STORE8:
                I64_STORE8(ins, block, stack);
                break;
            case I64_STORE16:
                I64_STORE16(ins, block, stack);
                break;
            case I64_STORE32:
                I64_STORE32(ins, block, stack);
                break;
            case F32_NEG:
                F32_NEG(ins, stack);
                break;
            case F32_CONST:
                F32_CONST(ins, stack);
                break;
            case F32_ABS:
                F32_ABS(ins, stack);
                break;
            case F32_CEIL:
                F32_CEIL(ins, stack);
                break;
            case F32_FLOOR:
                F32_FLOOR(ins, stack);
                break;
            case F32_NEAREST:
                F32_NEAREST(ins, stack);
                break;
            case F32_TRUNC:
                F32_TRUNC(ins, stack);
                break;
            case F32_EQ:
                F32_EQ(ins, stack);
                break;
            case F32_NE:
                F32_NE(ins, stack);
                break;
            case F32_LT:
                F32_LT(ins, stack);
                break;
            case F32_LE:
                F32_LE(ins, stack);
                break;
            case F32_GT:
                F32_GT(ins, stack);
                break;
            case F32_GE:
                F32_GE(ins, stack);
                break;
            case F32_ADD:
                F32_ADD(ins, stack);
                break;
            case F32_SUB:
                F32_SUB(ins, stack);
                break;
            case F32_MUL:
                F32_MUL(ins, stack);
                break;
            case F32_DIV:
                F32_DIV(ins, stack);
                break;
            case F32_MIN:
                F32_MIN(ins, stack);
                break;
            case F32_MAX:
                F32_MAX(ins, stack);
                break;
            case F32_SQRT:
                F32_SQRT(ins, stack);
                break;
            case F32_LOAD:
                F32_LOAD(ins, block, stack);
                break;
            case F32_STORE:
                F32_STORE(ins, block, stack);
                break;
            case F64_NEG:
                F64_NEG(ins, stack);
                break;
            case F64_CONST:
                F64_CONST(ins, stack);
                break;
            case F64_CONVERT_I64_U:
                F64_CONVERT_I64_U(ins, stack);
                break;
            case F64_CONVERT_I32_S:
                F64_CONVERT_I32_S(ins, stack);
                break;
            case F64_CONVERT_I32_U:
                F64_CONVERT_I32_U(ins, stack);
                break;
            case F64_PROMOTE_F32:
                F64_PROMOTE_F32(ins, stack);
                break;
            case F64_ADD:
                F64_ADD(ins, stack);
                break;
            case F64_SUB:
                F64_SUB(ins, stack);
                break;
            case F64_MUL:
                F64_MUL(ins, stack);
                break;
            case F64_DIV:
                F64_DIV(ins, stack);
                break;
            case F64_SQRT:
                F64_SQRT(ins, stack);
                break;
            case F64_LOAD:
                F64_LOAD(ins, block, stack);
                break;
            case F64_STORE:
                F64_STORE(ins, block, stack);
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

    public static void I64_ADD(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.PLUS));
    }

    public static void I64_SUB(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.MINUS));
    }

    public static void I64_MUL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.MULTIPLY));
    }

    /**
     * Emit I32_DIV_S: signed division (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_DIV_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr divCall = new MethodCallExpr();
        divCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        divCall.setName("I32_DIV_S");
        divCall.addArgument(a);
        divCall.addArgument(b);
        stack.push(divCall);
    }

    /**
     * Emit I32_DIV_U: unsigned division (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_DIV_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr divCall = new MethodCallExpr();
        divCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        divCall.setName("I32_DIV_U");
        divCall.addArgument(a);
        divCall.addArgument(b);
        stack.push(divCall);
    }

    /**
     * Emit I32_REM_S: signed remainder (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_REM_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr remCall = new MethodCallExpr();
        remCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        remCall.setName("I32_REM_S");
        remCall.addArgument(a);
        remCall.addArgument(b);
        stack.push(remCall);
    }

    /**
     * Emit I32_REM_U: unsigned remainder (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_REM_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr remCall = new MethodCallExpr();
        remCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        remCall.setName("I32_REM_U");
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
     * Emit I32_ROTL: rotate left (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_ROTL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr rotlCall = new MethodCallExpr();
        rotlCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        rotlCall.setName("I32_ROTL");
        rotlCall.addArgument(a);
        rotlCall.addArgument(b);
        stack.push(rotlCall);
    }

    /**
     * Emit I32_ROTR: rotate right (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_ROTR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr rotrCall = new MethodCallExpr();
        rotrCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        rotrCall.setName("I32_ROTR");
        rotrCall.addArgument(a);
        rotrCall.addArgument(b);
        stack.push(rotrCall);
    }

    /**
     * Emit I32_CLZ: count leading zeros (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_CLZ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr clzCall = new MethodCallExpr();
        clzCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        clzCall.setName("I32_CLZ");
        clzCall.addArgument(a);
        stack.push(clzCall);
    }

    /**
     * Emit I32_CTZ: count trailing zeros (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_CTZ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr ctzCall = new MethodCallExpr();
        ctzCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        ctzCall.setName("I32_CTZ");
        ctzCall.addArgument(a);
        stack.push(ctzCall);
    }

    /**
     * Emit I32_POPCNT: population count (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_POPCNT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr popcntCall = new MethodCallExpr();
        popcntCall.setScope(
                StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        popcntCall.setName("I32_POPCNT");
        popcntCall.addArgument(a);
        stack.push(popcntCall);
    }

    /**
     * Emit I32_EXTEND_8_S: sign extend 8-bit to 32-bit (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_EXTEND_8_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_EXTEND_8_S");
        call.addArgument(a);
        stack.push(call);
    }

    /**
     * Emit I32_EXTEND_16_S: sign extend 16-bit to 32-bit (matching ASM compiler - uses OpcodeImpl)
     */
    public static void I32_EXTEND_16_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I32_EXTEND_16_S");
        call.addArgument(a);
        stack.push(call);
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

    public static void I64_EQZ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_EQZ");
        call.addArgument(a);
        stack.push(call);
    }

    public static void I64_EQ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_EQ");
        call.addArgument(b);
        call.addArgument(a);
        stack.push(call);
    }

    public static void I64_NE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_NE");
        call.addArgument(b);
        call.addArgument(a);
        stack.push(call);
    }

    public static void I64_LT_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_LT_S");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I64_LT_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_LT_U");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I64_GT_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_GT_S");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I64_GT_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_GT_U");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I64_LE_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_LE_S");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I64_LE_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_LE_U");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I64_GE_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_GE_S");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void I64_GE_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("I64_GE_U");
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

    /**
     * Generate getAddr expression: (base < 0) ? base : base + offset
     * Matching ASM compiler's Shaded.getAddr logic
     */
    private static com.github.javaparser.ast.expr.Expression getAddrExpr(
            com.github.javaparser.ast.expr.Expression base, int offset) {
        BinaryExpr basePlusOffset =
                new BinaryExpr(
                        base,
                        new IntegerLiteralExpr(String.valueOf(offset)),
                        BinaryExpr.Operator.PLUS);
        BinaryExpr baseLessThanZero =
                new BinaryExpr(base, new IntegerLiteralExpr("0"), BinaryExpr.Operator.LESS);
        return new ConditionalExpr(baseLessThanZero, base, basePlusOffset);
    }

    private static MethodCallExpr opcodeImplCall(
            String name, com.github.javaparser.ast.expr.Expression... args) {
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName(name);
        for (var arg : args) {
            call.addArgument(arg);
        }
        return call;
    }

    public static void I32_LOAD(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("readInt");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        stack.push(call);
    }

    public static void I32_STORE(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeI32");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(new CastExpr(PrimitiveType.intType(), value));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I32_LOAD8_S(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr readCall = new MethodCallExpr();
        readCall.setScope(new NameExpr("memory"));
        readCall.setName("read");
        readCall.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        stack.push(
                new CastExpr(
                        PrimitiveType.intType(), new CastExpr(PrimitiveType.byteType(), readCall)));
    }

    public static void I32_LOAD8_U(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr readCall = new MethodCallExpr();
        readCall.setScope(new NameExpr("memory"));
        readCall.setName("read");
        readCall.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        stack.push(
                new BinaryExpr(
                        new CastExpr(PrimitiveType.intType(), readCall),
                        new IntegerLiteralExpr("0xFF"),
                        BinaryExpr.Operator.BINARY_AND));
    }

    public static void I32_LOAD16_S(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr readCall = new MethodCallExpr();
        readCall.setScope(new NameExpr("memory"));
        readCall.setName("readShort");
        readCall.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        stack.push(
                new CastExpr(
                        PrimitiveType.intType(),
                        new CastExpr(PrimitiveType.shortType(), readCall)));
    }

    public static void I32_LOAD16_U(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr readCall = new MethodCallExpr();
        readCall.setScope(new NameExpr("memory"));
        readCall.setName("readShort");
        readCall.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        stack.push(
                new BinaryExpr(
                        new CastExpr(PrimitiveType.intType(), readCall),
                        new IntegerLiteralExpr("0xFFFF"),
                        BinaryExpr.Operator.BINARY_AND));
    }

    public static void I32_STORE8(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        CastExpr byteValue = new CastExpr(PrimitiveType.byteType(), value);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeByte");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(byteValue);
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I32_STORE16(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        CastExpr shortValue = new CastExpr(PrimitiveType.shortType(), value);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeShort");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(shortValue);
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I64_CONST(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        long value = ins.operand(0);
        stack.push(new com.github.javaparser.ast.expr.LongLiteralExpr(String.valueOf(value) + "L"));
    }

    public static void I64_LOAD(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("readLong");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        stack.push(call);
    }

    public static void I64_LOAD8_S(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        I32_LOAD8_S(ins, block, stack);
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        stack.push(new CastExpr(PrimitiveType.longType(), value));
    }

    public static void I64_LOAD8_U(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        I32_LOAD8_U(ins, block, stack);
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        stack.push(new CastExpr(PrimitiveType.longType(), value));
    }

    public static void I64_LOAD16_S(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        I32_LOAD16_S(ins, block, stack);
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        stack.push(new CastExpr(PrimitiveType.longType(), value));
    }

    public static void I64_LOAD16_U(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        I32_LOAD16_U(ins, block, stack);
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        stack.push(new CastExpr(PrimitiveType.longType(), value));
    }

    public static void I64_LOAD32_S(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        I32_LOAD(ins, block, stack);
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        stack.push(new CastExpr(PrimitiveType.longType(), value));
    }

    public static void I64_LOAD32_U(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        I32_LOAD(ins, block, stack);
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        BinaryExpr masked =
                new BinaryExpr(
                        new CastExpr(PrimitiveType.longType(), value),
                        new com.github.javaparser.ast.expr.LongLiteralExpr("0xFFFFFFFFL"),
                        BinaryExpr.Operator.BINARY_AND);
        stack.push(masked);
    }

    public static void I64_STORE(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeLong");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(new CastExpr(PrimitiveType.longType(), value));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I64_STORE8(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        CastExpr intValue = new CastExpr(PrimitiveType.intType(), value);
        CastExpr byteValue = new CastExpr(PrimitiveType.byteType(), intValue);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeByte");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(byteValue);
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I64_STORE16(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        CastExpr intValue = new CastExpr(PrimitiveType.intType(), value);
        CastExpr shortValue = new CastExpr(PrimitiveType.shortType(), intValue);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeShort");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(shortValue);
        block.addStatement(new ExpressionStmt(call));
    }

    public static void I64_STORE32(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        CastExpr intValue = new CastExpr(PrimitiveType.intType(), value);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeI32");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(intValue);
        block.addStatement(new ExpressionStmt(call));
    }

    public static void F32_LOAD(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("readFloat");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        stack.push(call);
    }

    public static void F32_STORE(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeF32");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(new CastExpr(PrimitiveType.floatType(), value));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void F32_NEG(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new UnaryExpr(a, UnaryExpr.Operator.MINUS));
    }

    public static void F32_CONST(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        int bits = (int) ins.operand(0);
        float value = Float.intBitsToFloat(bits);
        String literal = Float.toString(value) + "f";
        stack.push(StaticJavaParser.parseExpression(literal));
    }

    public static void F32_ABS(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_ABS", a));
    }

    public static void F32_CEIL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_CEIL", a));
    }

    public static void F32_FLOOR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_FLOOR", a));
    }

    public static void F32_NEAREST(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_NEAREST", a));
    }

    public static void F32_TRUNC(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_TRUNC", a));
    }

    public static void F32_COPYSIGN(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_COPYSIGN", a, b));
    }

    public static void F32_REINTERPRET_I32(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_REINTERPRET_I32", a));
    }

    public static void F32_CONVERT_I32_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_CONVERT_I32_S", a));
    }

    public static void F32_CONVERT_I32_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_CONVERT_I32_U", a));
    }

    public static void F32_CONVERT_I64_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_CONVERT_I64_S", a));
    }

    public static void F32_CONVERT_I64_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_CONVERT_I64_U", a));
    }

    public static void F32_EQ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_EQ", a, b));
    }

    public static void F32_NE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_NE", a, b));
    }

    public static void F32_LT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_LT", a, b));
    }

    public static void F32_LE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_LE", a, b));
    }

    public static void F32_GT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_GT", a, b));
    }

    public static void F32_GE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F32_GE", a, b));
    }

    public static void F32_ADD(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.PLUS));
    }

    public static void F32_SUB(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.MINUS));
    }

    public static void F32_MUL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.MULTIPLY));
    }

    public static void F32_DIV(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.DIVIDE));
    }

    public static void F32_MIN(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("F32_MIN");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void F32_MAX(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("F32_MAX");
        call.addArgument(a);
        call.addArgument(b);
        stack.push(call);
    }

    public static void F32_SQRT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("F32_SQRT");
        call.addArgument(a);
        stack.push(call);
    }

    public static void F64_LOAD(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("readDouble");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        stack.push(call);
    }

    public static void F64_STORE(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        com.github.javaparser.ast.expr.Expression base = stack.pop();
        long offset = ins.operand(1);
        com.github.javaparser.ast.expr.Expression addrExpr = getAddrExpr(base, (int) offset);
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(new NameExpr("memory"));
        call.setName("writeF64");
        call.addArgument(new CastExpr(PrimitiveType.intType(), addrExpr));
        call.addArgument(new CastExpr(PrimitiveType.doubleType(), value));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void F64_NEG(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new UnaryExpr(a, UnaryExpr.Operator.MINUS));
    }

    public static void F64_CONST(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        long bits = ins.operand(0);
        double value = Double.longBitsToDouble(bits);
        String literal = Double.toString(value);
        stack.push(StaticJavaParser.parseExpression(literal));
    }

    public static void F64_CONVERT_I64_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("F64_CONVERT_I64_U");
        call.addArgument(a);
        stack.push(call);
    }

    public static void F64_CONVERT_I32_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("F64_CONVERT_I32_S");
        call.addArgument(a);
        stack.push(call);
    }

    public static void F64_CONVERT_I32_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("F64_CONVERT_I32_U");
        call.addArgument(a);
        stack.push(call);
    }

    public static void F64_PROMOTE_F32(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new CastExpr(PrimitiveType.doubleType(), a));
    }

    public static void F64_ADD(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.PLUS));
    }

    public static void F64_SUB(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.MINUS));
    }

    public static void F64_MUL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.MULTIPLY));
    }

    public static void F64_DIV(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new BinaryExpr(a, b, BinaryExpr.Operator.DIVIDE));
    }

    public static void F64_SQRT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(StaticJavaParser.parseExpression("com.dylibso.chicory.runtime.OpcodeImpl"));
        call.setName("F64_SQRT");
        call.addArgument(a);
        stack.push(call);
    }

    public static void GLOBAL_GET(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        int globalIndex = (int) ins.operand(0);
        // Inline readGlobal: instance.global(globalIndex).getValue()
        MethodCallExpr globalCall = new MethodCallExpr();
        globalCall.setScope(new FieldAccessExpr(new ThisExpr(), "instance"));
        globalCall.setName("global");
        globalCall.addArgument(new IntegerLiteralExpr(String.valueOf(globalIndex)));
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(globalCall);
        call.setName("getValue");
        stack.push(call);
    }

    public static void GLOBAL_SET(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        int globalIndex = (int) ins.operand(0);
        // Inline writeGlobal: instance.global(globalIndex).setValue(value)
        MethodCallExpr globalCall = new MethodCallExpr();
        globalCall.setScope(new FieldAccessExpr(new ThisExpr(), "instance"));
        globalCall.setName("global");
        globalCall.addArgument(new IntegerLiteralExpr(String.valueOf(globalIndex)));
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(globalCall);
        call.setName("setValue");
        call.addArgument(value);
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

    /**
     * Emit LABEL: mark a label location (matching ASM compiler 1:1).
     * ASM: asm.mark(label)
     * Java: label: { ... } (labeled block for break label; to work)
     */
    public static void LABEL(
            CompilerInstruction ins,
            BlockStmt block,
            Map<Long, String> labelNames,
            Set<Long> visitedTargets) {
        long target = ins.operand(0);
        String labelName = labelNames.get(target);
        if (labelName != null) {
            visitedTargets.add(target);
            // Create a labeled block - this allows break label; to jump here (matching ASM
            // compiler's label behavior)
            BlockStmt labeledBlock = new BlockStmt();
            LabeledStmt labeledStmt = new LabeledStmt();
            labeledStmt.setLabel(new SimpleName(labelName));
            labeledStmt.setStatement(labeledBlock);
            block.addStatement(labeledStmt);
            // Note: Subsequent instructions will be added to 'block', not 'labeledBlock'
            // This matches ASM compiler where labels are just marks in the instruction stream
        }
    }

    /**
     * Emit GOTO: unconditional jump (matching ASM compiler 1:1).
     * ASM: asm.goTo(label) with CHECK_INTERRUPTION for backward jumps
     * Java: break label; (with checkInterruption for backward jumps)
     */
    public static void GOTO(
            CompilerInstruction ins,
            BlockStmt block,
            Map<Long, String> labelNames,
            Set<Long> visitedTargets) {
        long target = ins.operand(0);
        String labelName = labelNames.get(target);
        if (labelName != null) {
            if (visitedTargets.contains(target)) {
                // Backward jump - emit CHECK_INTERRUPTION (matching ASM compiler)
                // Inline: if (Thread.currentThread().isInterrupted()) throw new
                // ChicoryInterruptedException("Thread interrupted");
                MethodCallExpr isInterruptedCall = new MethodCallExpr();
                isInterruptedCall.setScope(
                        StaticJavaParser.parseExpression("Thread.currentThread()"));
                isInterruptedCall.setName("isInterrupted");
                com.github.javaparser.ast.expr.ObjectCreationExpr exception =
                        new com.github.javaparser.ast.expr.ObjectCreationExpr();
                exception.setType(
                        StaticJavaParser.parseClassOrInterfaceType(
                                "com.dylibso.chicory.runtime.ChicoryInterruptedException"));
                exception.addArgument(
                        new com.github.javaparser.ast.expr.StringLiteralExpr("Thread interrupted"));
                ThrowStmt throwStmt = new ThrowStmt(exception);
                IfStmt ifStmt = new IfStmt(isInterruptedCall, throwStmt, null);
                block.addStatement(ifStmt);
            }
            BreakStmt breakStmt = new BreakStmt();
            breakStmt.setLabel(new SimpleName(labelName));
            block.addStatement(breakStmt);
        }
    }

    /**
     * Emit IFEQ: jump if equal to 0 (matching ASM compiler).
     * ASM: asm.ifeq(label) - forward jumps only
     * Java: if (value == 0) break label;
     */
    public static void IFEQ(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            Map<Long, String> labelNames,
            Set<Long> visitedTargets) {
        long target = ins.operand(0);
        String labelName = labelNames.get(target);
        if (labelName != null) {
            if (visitedTargets.contains(target)) {
                throw new IllegalArgumentException("Unexpected backward jump");
            }
            com.github.javaparser.ast.expr.Expression value = stack.pop();
            BinaryExpr condition =
                    new BinaryExpr(value, new IntegerLiteralExpr("0"), BinaryExpr.Operator.EQUALS);
            BreakStmt breakStmt = new BreakStmt();
            breakStmt.setLabel(new SimpleName(labelName));
            IfStmt ifStmt = new IfStmt(condition, breakStmt, null);
            block.addStatement(ifStmt);
        }
    }

    /**
     * Emit IFNE: jump if not equal to 0 (matching ASM compiler).
     * ASM: asm.ifne(label) with CHECK_INTERRUPTION for backward jumps
     * Java: if (value != 0) break label; (with checkInterruption for backward jumps)
     */
    public static void IFNE(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            Map<Long, String> labelNames,
            Set<Long> visitedTargets) {
        long target = ins.operand(0);
        String labelName = labelNames.get(target);
        if (labelName != null) {
            com.github.javaparser.ast.expr.Expression value = stack.pop();
            if (visitedTargets.contains(target)) {
                // Backward jump - emit CHECK_INTERRUPTION (matching ASM compiler)
                BinaryExpr condition =
                        new BinaryExpr(
                                value, new IntegerLiteralExpr("0"), BinaryExpr.Operator.EQUALS);
                BreakStmt breakStmt = new BreakStmt();
                breakStmt.setLabel(new SimpleName(labelName));
                // Inline checkInterruption: if (Thread.currentThread().isInterrupted()) throw new
                // ChicoryInterruptedException("Thread interrupted");
                MethodCallExpr isInterruptedCall = new MethodCallExpr();
                isInterruptedCall.setScope(
                        StaticJavaParser.parseExpression("Thread.currentThread()"));
                isInterruptedCall.setName("isInterrupted");
                com.github.javaparser.ast.expr.ObjectCreationExpr exception =
                        new com.github.javaparser.ast.expr.ObjectCreationExpr();
                exception.setType(
                        StaticJavaParser.parseClassOrInterfaceType(
                                "com.dylibso.chicory.runtime.ChicoryInterruptedException"));
                exception.addArgument(
                        new com.github.javaparser.ast.expr.StringLiteralExpr("Thread interrupted"));
                ThrowStmt throwStmt = new ThrowStmt(exception);
                IfStmt checkIfStmt = new IfStmt(isInterruptedCall, throwStmt, null);
                BlockStmt thenBlock = new BlockStmt();
                thenBlock.addStatement(checkIfStmt);
                thenBlock.addStatement(breakStmt);
                IfStmt ifStmt = new IfStmt(condition, new EmptyStmt(), thenBlock);
                block.addStatement(ifStmt);
            } else {
                BinaryExpr condition =
                        new BinaryExpr(
                                value, new IntegerLiteralExpr("0"), BinaryExpr.Operator.NOT_EQUALS);
                BreakStmt breakStmt = new BreakStmt();
                breakStmt.setLabel(new SimpleName(labelName));
                IfStmt ifStmt = new IfStmt(condition, breakStmt, null);
                block.addStatement(ifStmt);
            }
        }
    }

    /**
     * Emit SWITCH: table switch (matching ASM compiler).
     * ASM: asm.tableswitch(0, table.length - 1, defaultLabel, table)
     * Java: switch (value) { case 0: break label0; ... default: break defaultLabel; }
     */
    public static void SWITCH(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            Map<Long, String> labelNames,
            Set<Long> visitedTargets) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();

        // Check for backward jumps (matching ASM compiler)
        if (ins.operands().anyMatch(visitedTargets::contains)) {
            // Inline checkInterruption: if (Thread.currentThread().isInterrupted()) throw new
            // ChicoryInterruptedException("Thread interrupted");
            MethodCallExpr isInterruptedCall = new MethodCallExpr();
            isInterruptedCall.setScope(StaticJavaParser.parseExpression("Thread.currentThread()"));
            isInterruptedCall.setName("isInterrupted");
            com.github.javaparser.ast.expr.ObjectCreationExpr exception =
                    new com.github.javaparser.ast.expr.ObjectCreationExpr();
            exception.setType(
                    StaticJavaParser.parseClassOrInterfaceType(
                            "com.dylibso.chicory.runtime.ChicoryInterruptedException"));
            exception.addArgument(
                    new com.github.javaparser.ast.expr.StringLiteralExpr("Thread interrupted"));
            ThrowStmt throwStmt = new ThrowStmt(exception);
            IfStmt ifStmt = new IfStmt(isInterruptedCall, throwStmt, null);
            block.addStatement(ifStmt);
        }

        // Table switch using the last entry as the default (matching ASM compiler)
        int tableSize = ins.operandCount() - 1;
        SwitchStmt switchStmt = new SwitchStmt();
        switchStmt.setSelector(value);

        // Add cases for each table entry
        for (int i = 0; i < tableSize; i++) {
            long target = ins.operand(i);
            String labelName = labelNames.get(target);
            if (labelName != null) {
                com.github.javaparser.ast.stmt.SwitchEntry entry =
                        new com.github.javaparser.ast.stmt.SwitchEntry();
                entry.getLabels().add(new IntegerLiteralExpr(String.valueOf(i)));
                BreakStmt breakStmt = new BreakStmt();
                breakStmt.setLabel(new SimpleName(labelName));
                entry.getStatements().add(breakStmt);
                switchStmt.getEntries().add(entry);
            }
        }

        // Default case (last entry)
        long defaultTarget = ins.operand(tableSize);
        String defaultLabelName = labelNames.get(defaultTarget);
        if (defaultLabelName != null) {
            com.github.javaparser.ast.stmt.SwitchEntry defaultEntry =
                    new com.github.javaparser.ast.stmt.SwitchEntry();
            BreakStmt breakStmt = new BreakStmt();
            breakStmt.setLabel(new SimpleName(defaultLabelName));
            defaultEntry.getStatements().add(breakStmt);
            switchStmt.getEntries().add(defaultEntry);
        }

        block.addStatement(switchStmt);
    }
}
