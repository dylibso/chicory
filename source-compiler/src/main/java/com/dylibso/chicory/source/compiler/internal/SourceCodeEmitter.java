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
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
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

    /** Tracks scope nesting in the emitter for structured control flow. */
    private static final class EmitterScope {
        final int label;
        final String type; // "block", "loop", "if", "else"
        final BlockStmt block;
        final String[] resultVarNames;
        final IfStmt ifStmt; // only for "if" scopes

        EmitterScope(
                int label, String type, BlockStmt block, String[] resultVarNames, IfStmt ifStmt) {
            this.label = label;
            this.type = type;
            this.block = block;
            this.resultVarNames = resultVarNames;
            this.ifStmt = ifStmt;
        }
    }

    private static EmitterScope findScope(Deque<EmitterScope> scopeStack, int label) {
        for (EmitterScope scope : scopeStack) {
            if (scope.label == label) {
                return scope;
            }
        }
        return null;
    }

    private static void emitInterruptCheck(BlockStmt block) {
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

    private static String javaTypeNameForId(long typeId) {
        if (typeId == ValType.I32.id()) {
            return "int";
        } else if (typeId == ValType.I64.id()) {
            return "long";
        } else if (typeId == ValType.F32.id()) {
            return "float";
        } else if (typeId == ValType.F64.id()) {
            return "double";
        }
        return "int"; // fallback for ref types
    }

    private static String defaultValueForId(long typeId) {
        if (typeId == ValType.I32.id()) {
            return "0";
        } else if (typeId == ValType.I64.id()) {
            return "0L";
        } else if (typeId == ValType.F32.id()) {
            return "0.0f";
        } else if (typeId == ValType.F64.id()) {
            return "0.0";
        }
        return "0"; // fallback for ref types
    }

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

        List<ValType> globalTypes = analyzer.globalTypes();

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
                        clazz,
                        funcId,
                        functionType,
                        instructions,
                        body,
                        functionTypes,
                        module.typeSection().types(),
                        globalTypes);
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
            importsCall.setScope(new NameExpr("instance"));
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
            applyCall.addArgument(new NameExpr("instance"));
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
                callMethodExpr.addArgument(new NameExpr("instance"));
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
            List<FunctionType> allFunctionTypes,
            FunctionType[] typeSectionTypes,
            List<ValType> globalTypes) {

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

        // Scope tracking for structured control flow
        Deque<EmitterScope> scopeStack = new ArrayDeque<>();

        // Insertion position for result var declarations (after locals, before code)
        int[] resultVarInsertPos = {block.getStatements().size()};

        // Stack of expressions - represents the Java operand stack
        Deque<com.github.javaparser.ast.expr.Expression> stack = new ArrayDeque<>();

        // Emit instructions using structured control flow
        for (CompilerInstruction ins : instructions) {
            BlockStmt currentBlock = scopeStack.isEmpty() ? block : scopeStack.peek().block;
            CompilerOpCode op = ins.opcode();

            // Skip non-scope instructions if current block is already terminated
            if (isBlockTerminated(currentBlock)
                    && op != CompilerOpCode.SCOPE_EXIT
                    && op != CompilerOpCode.ELSE_ENTER) {
                // Still need to maintain expression stack for scope tracking
                if (op == CompilerOpCode.BLOCK_ENTER
                        || op == CompilerOpCode.LOOP_ENTER
                        || op == CompilerOpCode.IF_ENTER) {
                    // Push a dummy scope so SCOPE_EXIT can pop it
                    scopeStack.push(
                            new EmitterScope(
                                    (int) ins.operand(0), "dead", new BlockStmt(), null, null));
                }
                continue;
            }

            switch (op) {
                case BLOCK_ENTER:
                    {
                        int label = (int) ins.operand(0);
                        int resultCount = ins.operandCount() - 1;
                        String[] resultVars =
                                declareResultVars(
                                        resultCount, label, ins, block, resultVarInsertPos);

                        BlockStmt innerBlock = new BlockStmt();
                        LabeledStmt labeledStmt = new LabeledStmt();
                        labeledStmt.setLabel(new SimpleName("label_" + label));
                        labeledStmt.setStatement(innerBlock);
                        currentBlock.addStatement(labeledStmt);
                        scopeStack.push(
                                new EmitterScope(label, "block", innerBlock, resultVars, null));
                        break;
                    }

                case LOOP_ENTER:
                    {
                        int label = (int) ins.operand(0);
                        BlockStmt loopBody = new BlockStmt();
                        WhileStmt whileStmt = new WhileStmt(new BooleanLiteralExpr(true), loopBody);
                        LabeledStmt labeledStmt = new LabeledStmt();
                        labeledStmt.setLabel(new SimpleName("label_" + label));
                        labeledStmt.setStatement(whileStmt);
                        currentBlock.addStatement(labeledStmt);
                        scopeStack.push(new EmitterScope(label, "loop", loopBody, null, null));
                        break;
                    }

                case IF_ENTER:
                    {
                        int label = (int) ins.operand(0);
                        int resultCount = ins.operandCount() - 1;
                        com.github.javaparser.ast.expr.Expression condition = stack.pop();
                        String[] resultVars =
                                declareResultVars(
                                        resultCount, label, ins, block, resultVarInsertPos);

                        BlockStmt thenBlock = new BlockStmt();
                        BinaryExpr condExpr =
                                new BinaryExpr(
                                        condition,
                                        new IntegerLiteralExpr("0"),
                                        BinaryExpr.Operator.NOT_EQUALS);
                        IfStmt ifStmt = new IfStmt(condExpr, thenBlock, null);
                        currentBlock.addStatement(ifStmt);
                        scopeStack.push(
                                new EmitterScope(label, "if", thenBlock, resultVars, ifStmt));
                        break;
                    }

                case ELSE_ENTER:
                    {
                        EmitterScope ifScope = scopeStack.pop();
                        // Assign then-branch results to result variables
                        assignResults(ifScope.resultVarNames, ifScope.block, stack);
                        // Create else block
                        BlockStmt elseBlock = new BlockStmt();
                        ifScope.ifStmt.setElseStmt(elseBlock);
                        scopeStack.push(
                                new EmitterScope(
                                        ifScope.label,
                                        "else",
                                        elseBlock,
                                        ifScope.resultVarNames,
                                        ifScope.ifStmt));
                        break;
                    }

                case SCOPE_EXIT:
                    {
                        EmitterScope scope = scopeStack.pop();
                        boolean terminated = isBlockTerminated(scope.block);

                        // For loops, add break to exit the while(true) at end of body
                        if (scope.type.equals("loop") && !terminated) {
                            scope.block.addStatement(new BreakStmt());
                        }

                        // Assign results if present (skip if block already terminated)
                        if (!terminated) {
                            assignResults(scope.resultVarNames, scope.block, stack);
                        }

                        // Only push result vars if the scope isn't terminal for the parent.
                        // If terminated by break-to-own-label: not terminal, push results.
                        // If terminated by break-to-outer/throw/return: terminal, skip.
                        boolean terminalForParent =
                                terminated
                                        && !containsBreakTo(
                                                scope.block,
                                                "label_" + scope.label,
                                                scope.type.equals("loop"));
                        if (scope.resultVarNames != null && !terminalForParent) {
                            for (String resultVar : scope.resultVarNames) {
                                stack.push(new NameExpr(resultVar));
                            }
                        }
                        break;
                    }

                case BREAK:
                    {
                        int label = (int) ins.operand(0);
                        EmitterScope targetScope = findScope(scopeStack, label);
                        if (targetScope != null) {
                            assignResults(targetScope.resultVarNames, currentBlock, stack);
                        }
                        BreakStmt breakStmt = new BreakStmt();
                        breakStmt.setLabel(new SimpleName("label_" + label));
                        currentBlock.addStatement(breakStmt);
                        break;
                    }

                case BREAK_IF:
                    {
                        int label = (int) ins.operand(0);
                        com.github.javaparser.ast.expr.Expression condition = stack.pop();
                        EmitterScope targetScope = findScope(scopeStack, label);
                        BlockStmt thenBlock = new BlockStmt();

                        // Handle embedded DROP_KEEP if present (operands: [label, drop, types...])
                        if (ins.operandCount() > 1) {
                            applyDropKeep(ins, 1, stack);
                        }

                        // Materialize & assign results for branch-taken path
                        emitConditionalResultAssign(
                                targetScope, label, thenBlock, currentBlock, stack);

                        BreakStmt breakStmt = new BreakStmt();
                        breakStmt.setLabel(new SimpleName("label_" + label));
                        thenBlock.addStatement(breakStmt);

                        BinaryExpr condExpr =
                                new BinaryExpr(
                                        condition,
                                        new IntegerLiteralExpr("0"),
                                        BinaryExpr.Operator.NOT_EQUALS);
                        currentBlock.addStatement(new IfStmt(condExpr, thenBlock, null));
                        break;
                    }

                case CONTINUE:
                    {
                        int label = (int) ins.operand(0);
                        emitInterruptCheck(currentBlock);
                        ContinueStmt continueStmt = new ContinueStmt();
                        continueStmt.setLabel(new SimpleName("label_" + label));
                        currentBlock.addStatement(continueStmt);
                        break;
                    }

                case CONTINUE_IF:
                    {
                        int label = (int) ins.operand(0);
                        com.github.javaparser.ast.expr.Expression condition = stack.pop();
                        BlockStmt thenBlock = new BlockStmt();

                        // Handle embedded DROP_KEEP if present
                        if (ins.operandCount() > 1) {
                            applyDropKeep(ins, 1, stack);
                        }

                        emitInterruptCheck(thenBlock);
                        ContinueStmt continueStmt = new ContinueStmt();
                        continueStmt.setLabel(new SimpleName("label_" + label));
                        thenBlock.addStatement(continueStmt);

                        BinaryExpr condExpr =
                                new BinaryExpr(
                                        condition,
                                        new IntegerLiteralExpr("0"),
                                        BinaryExpr.Operator.NOT_EQUALS);
                        currentBlock.addStatement(new IfStmt(condExpr, thenBlock, null));
                        break;
                    }

                case TRAP:
                    currentBlock.addStatement(
                            StaticJavaParser.parseStatement(
                                    "throw new"
                                        + " com.dylibso.chicory.wasm.ChicoryException(\"trap\");"));
                    break;

                case RETURN:
                    RETURN(ins, currentBlock, stack, functionType);
                    break;

                case DROP:
                    DROP(ins, stack);
                    break;

                case DROP_KEEP:
                    DROP_KEEP(ins, stack);
                    break;

                case SWITCH:
                    // BR_TABLE - pop selector, throw for now
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }
                    currentBlock.addStatement(
                            StaticJavaParser.parseStatement(
                                    "throw new com.dylibso.chicory.wasm.ChicoryException("
                                            + "\"br_table not yet supported\");"));
                    break;

                default:
                    emitInstruction(
                            ins,
                            currentBlock,
                            stack,
                            localVarNames,
                            functionType,
                            allFunctionTypes,
                            typeSectionTypes,
                            globalTypes);
                    break;
            }
        }
    }

    /** Declare result variables for a block/if scope. Returns null if no results. */
    private static String[] declareResultVars(
            int resultCount, int label, CompilerInstruction ins, BlockStmt block, int[] insertPos) {
        if (resultCount <= 0) {
            return null;
        }
        String[] resultVars = new String[resultCount];
        for (int r = 0; r < resultCount; r++) {
            long typeId = ins.operand(r + 1);
            String varName = "block_" + label + "_result_" + r;
            resultVars[r] = varName;
            block.getStatements()
                    .add(
                            insertPos[0]++,
                            StaticJavaParser.parseStatement(
                                    javaTypeNameForId(typeId)
                                            + " "
                                            + varName
                                            + " = "
                                            + defaultValueForId(typeId)
                                            + ";"));
        }
        return resultVars;
    }

    /** Assign top-of-stack values to result variables (if stack has enough values). */
    private static void assignResults(
            String[] resultVarNames,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        if (resultVarNames == null || resultVarNames.length == 0) {
            return;
        }
        for (int r = resultVarNames.length - 1; r >= 0; r--) {
            if (!stack.isEmpty()) {
                com.github.javaparser.ast.expr.Expression val = stack.pop();
                block.addStatement(
                        new ExpressionStmt(
                                new AssignExpr(
                                        new NameExpr(resultVarNames[r]),
                                        val,
                                        AssignExpr.Operator.ASSIGN)));
            }
        }
    }

    /**
     * For BREAK_IF with results: materialize result candidates to temps,
     * assign inside the if-block, and push temps back for fall-through.
     */
    private static void emitConditionalResultAssign(
            EmitterScope targetScope,
            int label,
            BlockStmt thenBlock,
            BlockStmt currentBlock,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        if (targetScope == null || targetScope.resultVarNames == null) {
            return;
        }
        int resultCount = targetScope.resultVarNames.length;
        // Materialize result candidates to temp vars for both paths
        com.github.javaparser.ast.expr.Expression[] temps =
                new com.github.javaparser.ast.expr.Expression[resultCount];
        for (int r = resultCount - 1; r >= 0; r--) {
            if (!stack.isEmpty()) {
                com.github.javaparser.ast.expr.Expression val = stack.pop();
                String tempName = "brIfTemp_" + label + "_" + r;
                currentBlock.addStatement(
                        StaticJavaParser.parseStatement(
                                "var " + tempName + " = " + val.toString() + ";"));
                temps[r] = new NameExpr(tempName);
            }
        }
        // Push temps back for fall-through path
        for (int r = 0; r < resultCount; r++) {
            if (temps[r] != null) {
                stack.push(temps[r]);
            }
        }
        // Assign temps to result vars inside the if block
        for (int r = 0; r < resultCount; r++) {
            if (temps[r] != null) {
                thenBlock.addStatement(
                        new ExpressionStmt(
                                new AssignExpr(
                                        new NameExpr(targetScope.resultVarNames[r]),
                                        new NameExpr(temps[r].toString()),
                                        AssignExpr.Operator.ASSIGN)));
            }
        }
    }

    /** Check if a block's last statement is terminal (throw/return/break/continue). */
    private static boolean isBlockTerminated(BlockStmt block) {
        var stmts = block.getStatements();
        if (stmts.isEmpty()) {
            return false;
        }
        var last = stmts.get(stmts.size() - 1);
        if (last instanceof ThrowStmt
                || last instanceof ReturnStmt
                || last instanceof BreakStmt
                || last instanceof ContinueStmt) {
            return true;
        }
        if (last instanceof LabeledStmt) {
            return isLabeledBlockTerminal((LabeledStmt) last);
        }
        return false;
    }

    /**
     * A labeled block terminates the enclosing scope if:
     * 1. Its inner block ends in throw/return (recursively), AND
     * 2. No break targets this label (which would exit to the enclosing scope)
     */
    private static boolean isLabeledBlockTerminal(LabeledStmt labeled) {
        var inner = labeled.getStatement();
        BlockStmt body;
        if (inner instanceof BlockStmt) {
            body = (BlockStmt) inner;
        } else if (inner instanceof WhileStmt) {
            var whileBody = ((WhileStmt) inner).getBody();
            if (!(whileBody instanceof BlockStmt)) {
                return false;
            }
            body = (BlockStmt) whileBody;
        } else {
            return false;
        }
        if (!endsWithAbruptCompletion(body)) {
            return false;
        }
        String labelName = labeled.getLabel().asString();
        boolean inLoop = !(labeled.getStatement() instanceof BlockStmt);
        return !containsBreakTo(body, labelName, inLoop);
    }

    private static boolean endsWithAbruptCompletion(BlockStmt block) {
        var stmts = block.getStatements();
        if (stmts.isEmpty()) {
            return false;
        }
        var last = stmts.get(stmts.size() - 1);
        if (last instanceof ThrowStmt
                || last instanceof ReturnStmt
                || last instanceof BreakStmt
                || last instanceof ContinueStmt) {
            return true;
        }
        if (last instanceof LabeledStmt) {
            return isLabeledBlockTerminal((LabeledStmt) last);
        }
        return false;
    }

    private static boolean containsBreakTo(
            com.github.javaparser.ast.Node node, String label, boolean inLoop) {
        if (node instanceof BreakStmt) {
            var breakLabel = ((BreakStmt) node).getLabel();
            if (breakLabel.isPresent() && breakLabel.get().asString().equals(label)) {
                return true;
            }
            // Bare break inside a while exits the loop → flow continues after the label
            if (!breakLabel.isPresent() && inLoop) {
                return true;
            }
        }
        boolean childInLoop = inLoop || (node instanceof WhileStmt);
        for (var child : node.getChildNodes()) {
            if (containsBreakTo(child, label, childInLoop)) {
                return true;
            }
        }
        return false;
    }

    /** Apply DROP_KEEP from embedded operands starting at the given offset. */
    private static void applyDropKeep(
            CompilerInstruction ins,
            int offset,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        int drop = (int) ins.operand(offset);
        int totalTypes = ins.operandCount() - offset - 1;
        int keep = totalTypes - drop;
        if (drop > 0) {
            int available = stack.size();
            int toDropOnStack = Math.min(drop, Math.max(0, available - keep));
            for (int d = 0; d < toDropOnStack; d++) {
                stack.pop();
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
            FunctionType[] typeSectionTypes,
            List<ValType> globalTypes) {
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
            case I32_WRAP_I64:
                I32_WRAP_I64(ins, stack);
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
            case I64_DIV_S:
                I64_DIV_S(ins, stack);
                break;
            case I64_DIV_U:
                I64_DIV_U(ins, stack);
                break;
            case I64_REM_S:
                I64_REM_S(ins, stack);
                break;
            case I64_REM_U:
                I64_REM_U(ins, stack);
                break;
            case I64_AND:
                I64_AND(ins, stack);
                break;
            case I64_OR:
                I64_OR(ins, stack);
                break;
            case I64_XOR:
                I64_XOR(ins, stack);
                break;
            case I64_SHL:
                I64_SHL(ins, stack);
                break;
            case I64_SHR_S:
                I64_SHR_S(ins, stack);
                break;
            case I64_SHR_U:
                I64_SHR_U(ins, stack);
                break;
            case I64_ROTL:
                I64_ROTL(ins, stack);
                break;
            case I64_ROTR:
                I64_ROTR(ins, stack);
                break;
            case I64_CLZ:
                I64_CLZ(ins, stack);
                break;
            case I64_CTZ:
                I64_CTZ(ins, stack);
                break;
            case I64_POPCNT:
                I64_POPCNT(ins, stack);
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
            case I64_EXTEND_8_S:
                I64_EXTEND_8_S(ins, stack);
                break;
            case I64_EXTEND_16_S:
                I64_EXTEND_16_S(ins, stack);
                break;
            case I64_EXTEND_32_S:
                I64_EXTEND_32_S(ins, stack);
                break;
            case I64_EXTEND_I32_S:
                I64_EXTEND_I32_S(ins, stack);
                break;
            case I64_EXTEND_I32_U:
                I64_EXTEND_I32_U(ins, stack);
                break;
            case I64_TRUNC_F32_S:
                I64_TRUNC_F32_S(ins, stack);
                break;
            case I64_TRUNC_F32_U:
                I64_TRUNC_F32_U(ins, stack);
                break;
            case I64_TRUNC_F64_S:
                I64_TRUNC_F64_S(ins, stack);
                break;
            case I64_TRUNC_F64_U:
                I64_TRUNC_F64_U(ins, stack);
                break;
            case I64_TRUNC_SAT_F32_S:
                I64_TRUNC_SAT_F32_S(ins, stack);
                break;
            case I64_TRUNC_SAT_F32_U:
                I64_TRUNC_SAT_F32_U(ins, stack);
                break;
            case I64_TRUNC_SAT_F64_S:
                I64_TRUNC_SAT_F64_S(ins, stack);
                break;
            case I64_TRUNC_SAT_F64_U:
                I64_TRUNC_SAT_F64_U(ins, stack);
                break;
            case I64_REINTERPRET_F64:
                I64_REINTERPRET_F64(ins, stack);
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
            case CALL_INDIRECT:
                CALL_INDIRECT(ins, block, stack, typeSectionTypes);
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
            case F64_ABS:
                F64_ABS(ins, stack);
                break;
            case F64_CEIL:
                F64_CEIL(ins, stack);
                break;
            case F64_FLOOR:
                F64_FLOOR(ins, stack);
                break;
            case F64_NEAREST:
                F64_NEAREST(ins, stack);
                break;
            case F64_CONVERT_I64_U:
                F64_CONVERT_I64_U(ins, stack);
                break;
            case F64_CONVERT_I64_S:
                F64_CONVERT_I64_S(ins, stack);
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
            case F64_TRUNC:
                F64_TRUNC(ins, stack);
                break;
            case F64_EQ:
                F64_EQ(ins, stack);
                break;
            case F64_NE:
                F64_NE(ins, stack);
                break;
            case F64_LT:
                F64_LT(ins, stack);
                break;
            case F64_LE:
                F64_LE(ins, stack);
                break;
            case F64_GT:
                F64_GT(ins, stack);
                break;
            case F64_GE:
                F64_GE(ins, stack);
                break;
            case F64_MAX:
                F64_MAX(ins, stack);
                break;
            case F64_MIN:
                F64_MIN(ins, stack);
                break;
            case F64_COPYSIGN:
                F64_COPYSIGN(ins, stack);
                break;
            case F64_REINTERPRET_I64:
                F64_REINTERPRET_I64(ins, stack);
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
                GLOBAL_GET(ins, block, stack, globalTypes);
                break;
            case GLOBAL_SET:
                GLOBAL_SET(ins, block, stack, globalTypes);
                break;
            case DROP:
                DROP(ins, stack);
                break;
            case DROP_KEEP:
                DROP_KEEP(ins, stack);
                break;
            case SELECT:
                SELECT(ins, block, stack);
                break;
            case MEMORY_GROW:
                MEMORY_GROW(ins, block, stack);
                break;
            case MEMORY_SIZE:
                MEMORY_SIZE(ins, stack);
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
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.PLUS)));
    }

    /**
     * Emit I32_SUB: pop two values, subtract them, push result
     */
    public static void I32_SUB(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.MINUS)));
    }

    /**
     * Emit I32_MUL: pop two values, multiply them, push result
     */
    public static void I32_MUL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.MULTIPLY)));
    }

    public static void I64_ADD(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.PLUS)));
    }

    public static void I64_SUB(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.MINUS)));
    }

    public static void I64_MUL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.MULTIPLY)));
    }

    public static void I32_WRAP_I64(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        stack.push(new CastExpr(PrimitiveType.intType(), value));
    }

    public static void I64_DIV_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_DIV_S", a, b));
    }

    public static void I64_DIV_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_DIV_U", a, b));
    }

    public static void I64_REM_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_REM_S", a, b));
    }

    public static void I64_REM_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_REM_U", a, b));
    }

    public static void I64_AND(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.BINARY_AND)));
    }

    public static void I64_OR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.BINARY_OR)));
    }

    public static void I64_XOR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.XOR)));
    }

    public static void I64_SHL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.LEFT_SHIFT)));
    }

    public static void I64_SHR_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.SIGNED_RIGHT_SHIFT)));
    }

    public static void I64_SHR_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(
                new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT)));
    }

    public static void I64_ROTL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_ROTL", a, b));
    }

    public static void I64_ROTR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_ROTR", a, b));
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
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.BINARY_AND)));
    }

    /**
     * Emit I32_OR: bitwise OR
     */
    public static void I32_OR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.BINARY_OR)));
    }

    /**
     * Emit I32_XOR: bitwise XOR
     */
    public static void I32_XOR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.XOR)));
    }

    /**
     * Emit I32_SHL: left shift
     */
    public static void I32_SHL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.LEFT_SHIFT)));
    }

    /**
     * Emit I32_SHR_S: signed right shift
     */
    public static void I32_SHR_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.SIGNED_RIGHT_SHIFT)));
    }

    /**
     * Emit I32_SHR_U: unsigned right shift
     */
    public static void I32_SHR_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(
                new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT)));
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

    public static void I64_CLZ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_CLZ", a));
    }

    public static void I64_CTZ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_CTZ", a));
    }

    public static void I64_POPCNT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_POPCNT", a));
    }

    public static void I64_EXTEND_8_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_EXTEND_8_S", a));
    }

    public static void I64_EXTEND_16_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_EXTEND_16_S", a));
    }

    public static void I64_EXTEND_32_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_EXTEND_32_S", a));
    }

    public static void I64_EXTEND_I32_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(new CastExpr(PrimitiveType.longType(), a));
    }

    public static void I64_EXTEND_I32_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_EXTEND_I32_U", a));
    }

    public static void I64_TRUNC_F32_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_TRUNC_F32_S", a));
    }

    public static void I64_TRUNC_F32_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_TRUNC_F32_U", a));
    }

    public static void I64_TRUNC_F64_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_TRUNC_F64_S", a));
    }

    public static void I64_TRUNC_F64_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_TRUNC_F64_U", a));
    }

    public static void I64_TRUNC_SAT_F32_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_TRUNC_SAT_F32_S", a));
    }

    public static void I64_TRUNC_SAT_F32_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_TRUNC_SAT_F32_U", a));
    }

    public static void I64_TRUNC_SAT_F64_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_TRUNC_SAT_F64_S", a));
    }

    public static void I64_TRUNC_SAT_F64_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_TRUNC_SAT_F64_U", a));
    }

    public static void I64_REINTERPRET_F64(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("I64_REINTERPRET_F64", a));
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
     * Emit CALL: call function by ID.
     * Boxes args to long[], dispatches via instance.getMachine().call(), unboxes result.
     */
    public static void CALL(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            List<FunctionType> allFunctionTypes) {
        int funcId = (int) ins.operand(0);
        FunctionType calledFunctionType = allFunctionTypes.get(funcId);
        emitCallWithArgs(block, stack, calledFunctionType, String.valueOf(funcId));
    }

    /**
     * Emit CALL_INDIRECT: resolve function from table, type-check, and call.
     * Mirrors ASM compiler's CALL_INDIRECT + compileCallIndirect.
     */
    public static void CALL_INDIRECT(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            FunctionType[] typeSectionTypes) {
        int typeId = (int) ins.operand(0);
        int tableIdx = (int) ins.operand(1);
        FunctionType calledFunctionType = typeSectionTypes[typeId];

        // Pop the table entry index (topmost on the expression stack)
        com.github.javaparser.ast.expr.Expression entryIdx = stack.pop();

        // Resolve function ID from table
        block.addStatement(
                StaticJavaParser.parseStatement(
                        "int ciFuncId = instance.table("
                                + tableIdx
                                + ").requiredRef((int)("
                                + entryIdx
                                + "));"));

        // Type check (mirrors Shaded.callIndirect)
        block.addStatement(
                StaticJavaParser.parseStatement(
                        "if (!instance.type(instance.functionType(ciFuncId))"
                                + ".typesMatch(instance.type("
                                + typeId
                                + "))) { throw new"
                                + " com.dylibso.chicory.wasm.ChicoryException("
                                + "\"indirect call type mismatch\"); }"));

        emitCallWithArgs(block, stack, calledFunctionType, "ciFuncId");
    }

    /**
     * Shared helper: box args from expression stack, call via Machine, unbox result.
     * Used by both CALL and CALL_INDIRECT.
     */
    private static void emitCallWithArgs(
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            FunctionType calledFunctionType,
            String funcIdExpr) {
        int paramCount = calledFunctionType.params().size();

        // Box args from the expression stack
        String argsExpr;
        if (paramCount == 0) {
            argsExpr = "new long[0]";
        } else {
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
            argsExpr = "callArgs";
        }

        // Dispatch via Machine interface (works in static methods)
        String callExpr = "instance.getMachine().call(" + funcIdExpr + ", " + argsExpr + ")";

        // Handle return value
        if (calledFunctionType.returns().isEmpty()) {
            block.addStatement(StaticJavaParser.parseStatement(callExpr + ";"));
        } else if (calledFunctionType.returns().size() == 1) {
            block.addStatement(
                    StaticJavaParser.parseStatement("long[] callResult = " + callExpr + ";"));
            String unboxExpr =
                    SourceCompilerUtil.unboxLongToJvm(
                            "callResult[0]", calledFunctionType.returns().get(0));
            stack.push(StaticJavaParser.parseExpression(unboxExpr));
        } else {
            stack.push(StaticJavaParser.parseExpression(callExpr));
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
        var statements = block.getStatements();
        if (!statements.isEmpty() && statements.get(statements.size() - 1) instanceof ThrowStmt) {
            return;
        }

        Class<?> returnType = SourceCompilerUtil.jvmReturnType(functionType);

        if (returnType == void.class) {
            // Pop and discard any values on stack
            while (!stack.isEmpty()) {
                stack.pop();
            }
            block.addStatement(StaticJavaParser.parseStatement("return;"));
        } else if (returnType == long[].class) {
            if (stack.isEmpty()) {
                return;
            }
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
            if (stack.isEmpty()) {
                return;
            }
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
        String literal;
        if (Float.isNaN(value)) {
            literal = "Float.intBitsToFloat(" + bits + ")";
        } else if (value == Float.POSITIVE_INFINITY) {
            literal = "Float.POSITIVE_INFINITY";
        } else if (value == Float.NEGATIVE_INFINITY) {
            literal = "Float.NEGATIVE_INFINITY";
        } else {
            literal = Float.toString(value) + "f";
        }
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
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.PLUS)));
    }

    public static void F32_SUB(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.MINUS)));
    }

    public static void F32_MUL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.MULTIPLY)));
    }

    public static void F32_DIV(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.DIVIDE)));
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
        String literal;
        if (Double.isNaN(value)) {
            literal = "Double.longBitsToDouble(" + bits + "L)";
        } else if (value == Double.POSITIVE_INFINITY) {
            literal = "Double.POSITIVE_INFINITY";
        } else if (value == Double.NEGATIVE_INFINITY) {
            literal = "Double.NEGATIVE_INFINITY";
        } else {
            literal = Double.toString(value);
        }
        stack.push(StaticJavaParser.parseExpression(literal));
    }

    public static void F64_ABS(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_ABS", a));
    }

    public static void F64_CEIL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_CEIL", a));
    }

    public static void F64_CONVERT_I64_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_CONVERT_I64_U", a));
    }

    public static void F64_CONVERT_I64_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_CONVERT_I64_S", a));
    }

    public static void F64_CONVERT_I32_S(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_CONVERT_I32_S", a));
    }

    public static void F64_CONVERT_I32_U(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_CONVERT_I32_U", a));
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
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.PLUS)));
    }

    public static void F64_SUB(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.MINUS)));
    }

    public static void F64_MUL(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.MULTIPLY)));
    }

    public static void F64_DIV(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression b = stack.pop();
        com.github.javaparser.ast.expr.Expression a = stack.pop();
        stack.push(new EnclosedExpr(new BinaryExpr(a, b, BinaryExpr.Operator.DIVIDE)));
    }

    public static void F64_SQRT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_SQRT", a));
    }

    public static void F64_FLOOR(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_FLOOR", a));
    }

    public static void F64_NEAREST(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_NEAREST", a));
    }

    public static void F64_TRUNC(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_TRUNC", a));
    }

    public static void F64_COPYSIGN(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_COPYSIGN", a, b));
    }

    public static void F64_REINTERPRET_I64(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_REINTERPRET_I64", a));
    }

    public static void F64_EQ(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_EQ", a, b));
    }

    public static void F64_NE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_NE", a, b));
    }

    public static void F64_LT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_LT", a, b));
    }

    public static void F64_LE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_LE", a, b));
    }

    public static void F64_GT(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_GT", a, b));
    }

    public static void F64_GE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_GE", a, b));
    }

    public static void F64_MAX(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_MAX", a, b));
    }

    public static void F64_MIN(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        var b = stack.pop();
        var a = stack.pop();
        stack.push(opcodeImplCall("F64_MIN", a, b));
    }

    public static void GLOBAL_GET(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            List<ValType> globalTypes) {
        int globalIndex = (int) ins.operand(0);
        MethodCallExpr globalCall = new MethodCallExpr();
        globalCall.setScope(new NameExpr("instance"));
        globalCall.setName("global");
        globalCall.addArgument(new IntegerLiteralExpr(String.valueOf(globalIndex)));
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(globalCall);
        call.setName("getValue");
        ValType type = globalTypes.get(globalIndex);
        String unboxed = SourceCompilerUtil.unboxLongToJvm(call.toString(), type);
        stack.push(StaticJavaParser.parseExpression(unboxed));
    }

    public static void GLOBAL_SET(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack,
            List<ValType> globalTypes) {
        com.github.javaparser.ast.expr.Expression value = stack.pop();
        int globalIndex = (int) ins.operand(0);
        ValType type = globalTypes.get(globalIndex);
        String boxed = SourceCompilerUtil.boxJvmToLong(value.toString(), type);
        MethodCallExpr globalCall = new MethodCallExpr();
        globalCall.setScope(new NameExpr("instance"));
        globalCall.setName("global");
        globalCall.addArgument(new IntegerLiteralExpr(String.valueOf(globalIndex)));
        MethodCallExpr call = new MethodCallExpr();
        call.setScope(globalCall);
        call.setName("setValue");
        call.addArgument(StaticJavaParser.parseExpression(boxed));
        block.addStatement(new ExpressionStmt(call));
    }

    public static void DROP(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static void DROP_KEEP(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        int drop = (int) ins.operand(0);
        int keep = ins.operandCount() - 1 - drop;

        if (drop <= 0) {
            return;
        }

        int available = stack.size();
        int toDropOnStack = Math.min(drop, Math.max(0, available - keep));
        for (int i = 0; i < toDropOnStack; i++) {
            stack.pop();
        }
    }

    public static void SELECT(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        // WASM stack: [val1, val2, cond] → cond != 0 ? val1 : val2
        com.github.javaparser.ast.expr.Expression condition = stack.pop();
        com.github.javaparser.ast.expr.Expression val2 = stack.pop();
        com.github.javaparser.ast.expr.Expression val1 = stack.pop();
        BinaryExpr condExpr =
                new BinaryExpr(
                        condition, new IntegerLiteralExpr("0"), BinaryExpr.Operator.NOT_EQUALS);
        com.github.javaparser.ast.expr.ConditionalExpr ternary =
                new com.github.javaparser.ast.expr.ConditionalExpr(condExpr, val1, val2);
        stack.push(ternary);
    }

    /**
     * Emit MEMORY_GROW: grow memory by given number of pages.
     * Mirrors ASM compiler: calls memory.grow(size), returns old page count or -1.
     */
    public static void MEMORY_GROW(
            CompilerInstruction ins,
            BlockStmt block,
            Deque<com.github.javaparser.ast.expr.Expression> stack) {
        com.github.javaparser.ast.expr.Expression size = stack.pop();
        stack.push(StaticJavaParser.parseExpression("memory.grow(" + size + ")"));
    }

    /**
     * Emit MEMORY_SIZE: return current memory size in pages.
     * Mirrors ASM compiler: calls memory.pages().
     */
    public static void MEMORY_SIZE(
            CompilerInstruction ins, Deque<com.github.javaparser.ast.expr.Expression> stack) {
        stack.push(StaticJavaParser.parseExpression("memory.pages()"));
    }
}
