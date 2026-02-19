package com.dylibso.chicory.source.compiler.internal;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Splits too-large generated methods into smaller helper methods to stay within Java's 64KB
 * bytecode limit per method.
 *
 * <p>The approach: 1. Convert local variables to int[]/long[]/float[]/double[] arrays shared
 * between main method and helpers. 2. Extract large labeled blocks into static helper methods. 3.
 * Handle outer break/continue via a status array and labeled do-while wrapper.
 */
final class MethodSplitter {

    private MethodSplitter() {}

    /**
     * Methods with source representation larger than this (in characters) get split. ~80K chars of
     * source is a conservative estimate for ~40-60KB of bytecode.
     */
    private static final int MAX_METHOD_SIZE = 50_000;

    private static final String HELPER_BREAK_LABEL = "_hb";

    /**
     * When extracting a block from a helper that already has _hb: wrapper (nested extraction),
     * parent status codes (_hs[0] = CODE; break _hb;) are offset by this value to avoid
     * collision with the new helper's own return codes.
     */
    private static final int PARENT_STATUS_OFFSET = 10000;

    /**
     * Maximum number of splitting iterations. Safety cap to prevent infinite loops if the
     * heuristics fail to make progress.
     */
    private static final int MAX_ITERATIONS = 100;

    /** Split any too-large static methods in the class. */
    static void splitLargeMethods(ClassOrInterfaceDeclaration clazz) {
        boolean changed = true;
        int iteration = 0;
        while (changed && iteration < MAX_ITERATIONS) {
            changed = false;
            iteration++;
            for (MethodDeclaration method : new ArrayList<>(clazz.getMethods())) {
                if (method.isStatic() && methodSize(method) > MAX_METHOD_SIZE) {
                    if (!hasArrayLocals(method)) {
                        convertLocalsToArrays(method);
                    }
                    if (extractLargestBlock(clazz, method)) {
                        changed = true;
                    } else if (splitLargeSwitch(clazz, method)) {
                        changed = true;
                    }
                }
            }
        }
    }

    private static int methodSize(MethodDeclaration method) {
        return method.getBody().map(b -> b.toString().length()).orElse(0);
    }

    private static boolean hasArrayLocals(MethodDeclaration method) {
        for (Parameter param : method.getParameters()) {
            if (param.getNameAsString().equals("iL") || param.getNameAsString().equals("lL")) {
                return true;
            }
        }
        return method.getBody()
                .map(
                        body ->
                                body.getStatements().stream()
                                        .anyMatch(s -> s.toString().contains("int[] iL")))
                .orElse(false);
    }

    // -----------------------------------------------------------------------
    // Local-to-array conversion
    // -----------------------------------------------------------------------

    private static final class VarInfo {
        final String type; // "int", "long", "float", "double"
        final int arrayIndex;

        VarInfo(String type, int arrayIndex) {
            this.type = type;
            this.arrayIndex = arrayIndex;
        }
    }

    private static void convertLocalsToArrays(MethodDeclaration method) {
        BlockStmt body = method.getBody().orElseThrow();

        // Collect variables: params + local declarations at method body level
        Map<String, VarInfo> varMap = new LinkedHashMap<>();
        Map<String, Integer> typeCounts = new HashMap<>();
        typeCounts.put("int", 0);
        typeCounts.put("long", 0);
        typeCounts.put("float", 0);
        typeCounts.put("double", 0);

        // Map function parameters
        List<String> paramNames = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            String pName = param.getNameAsString();
            if (pName.equals("memory") || pName.equals("instance")) {
                continue;
            }
            String pType = primitiveTypeName(param.getType().toString());
            if (pType == null) {
                continue;
            }
            int idx = typeCounts.getOrDefault(pType, 0);
            varMap.put(pName, new VarInfo(pType, idx));
            typeCounts.put(pType, idx + 1);
            paramNames.add(pName);
        }

        // Collect local variable declarations at method body level
        List<Statement> toRemove = new ArrayList<>();
        for (Statement stmt : body.getStatements()) {
            if (!(stmt instanceof ExpressionStmt)) {
                continue;
            }
            ExpressionStmt exprStmt = (ExpressionStmt) stmt;
            if (!(exprStmt.getExpression()
                    instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr)) {
                continue;
            }
            com.github.javaparser.ast.expr.VariableDeclarationExpr varDecl =
                    (com.github.javaparser.ast.expr.VariableDeclarationExpr)
                            exprStmt.getExpression();
            boolean anyMapped = false;
            for (com.github.javaparser.ast.body.VariableDeclarator declarator :
                    varDecl.getVariables()) {
                String vName = declarator.getNameAsString();
                String vType = primitiveTypeName(declarator.getType().toString());
                if (vType == null) {
                    continue; // skip non-primitive types
                }
                int idx = typeCounts.getOrDefault(vType, 0);
                varMap.put(vName, new VarInfo(vType, idx));
                typeCounts.put(vType, idx + 1);
                anyMapped = true;
            }
            if (anyMapped) {
                toRemove.add(stmt);
            }
        }

        if (varMap.isEmpty()) {
            return;
        }

        // Remove original declarations
        for (Statement stmt : toRemove) {
            body.getStatements().remove(stmt);
        }

        // Replace all NameExpr references BEFORE inserting copy statements,
        // so that copy statements like "iL[0] = arg0;" aren't transformed
        // into "iL[0] = iL[0];"
        replaceVarReferences(body, varMap);

        // Insert array declarations at the top
        int insertIdx = 0;
        boolean hasInt = typeCounts.get("int") > 0;
        boolean hasLong = typeCounts.get("long") > 0;
        boolean hasFloat = typeCounts.get("float") > 0;
        boolean hasDouble = typeCounts.get("double") > 0;

        if (hasInt) {
            body.getStatements()
                    .add(
                            insertIdx++,
                            StaticJavaParser.parseStatement(
                                    "int[] iL = new int[" + typeCounts.get("int") + "];"));
        }
        if (hasLong) {
            body.getStatements()
                    .add(
                            insertIdx++,
                            StaticJavaParser.parseStatement(
                                    "long[] lL = new long[" + typeCounts.get("long") + "];"));
        }
        if (hasFloat) {
            body.getStatements()
                    .add(
                            insertIdx++,
                            StaticJavaParser.parseStatement(
                                    "float[] fL = new float[" + typeCounts.get("float") + "];"));
        }
        if (hasDouble) {
            body.getStatements()
                    .add(
                            insertIdx++,
                            StaticJavaParser.parseStatement(
                                    "double[] dL = new double[" + typeCounts.get("double") + "];"));
        }

        // Copy parameters into arrays (after replacement, so param names are still valid)
        for (String pName : paramNames) {
            VarInfo info = varMap.get(pName);
            body.getStatements()
                    .add(
                            insertIdx++,
                            StaticJavaParser.parseStatement(arrayRef(info) + " = " + pName + ";"));
        }
    }

    private static String primitiveTypeName(String typeStr) {
        switch (typeStr) {
            case "int":
                return "int";
            case "long":
                return "long";
            case "float":
                return "float";
            case "double":
                return "double";
            default:
                return null;
        }
    }

    private static String arrayName(String type) {
        switch (type) {
            case "int":
                return "iL";
            case "long":
                return "lL";
            case "float":
                return "fL";
            case "double":
                return "dL";
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private static String arrayRef(VarInfo info) {
        return arrayName(info.type) + "[" + info.arrayIndex + "]";
    }

    private static void replaceVarReferences(BlockStmt body, Map<String, VarInfo> varMap) {
        body.accept(
                new ModifierVisitor<Void>() {
                    @Override
                    public Visitable visit(NameExpr n, Void arg) {
                        VarInfo info = varMap.get(n.getNameAsString());
                        if (info != null) {
                            return new ArrayAccessExpr(
                                    new NameExpr(arrayName(info.type)),
                                    new IntegerLiteralExpr(String.valueOf(info.arrayIndex)));
                        }
                        return super.visit(n, null);
                    }
                },
                null);
    }

    // -----------------------------------------------------------------------
    // Block extraction
    // -----------------------------------------------------------------------

    /**
     * Find and extract a suitable labeled block into a helper method. Returns true if an
     * extraction was performed.
     *
     * <p>Picks the block closest to half the method size to ensure geometric convergence.
     * Extracting the absolute largest block (which is often nearly the entire method) just moves
     * the problem to the helper without reducing total size, causing an infinite loop.
     */
    private static boolean extractLargestBlock(
            ClassOrInterfaceDeclaration clazz, MethodDeclaration method) {
        BlockStmt body = method.getBody().orElseThrow();
        int mSize = methodSize(method);

        // Find the best block to extract: closest to half method size
        LabeledStmt best = findBestBlockToExtract(body, mSize);
        if (best == null) {
            return false;
        }

        // Only extract if the block is a significant portion of the method
        int blockSize = best.toString().length();
        if (blockSize < MAX_METHOD_SIZE / 4) {
            return false;
        }

        String baseName = baseMethodName(method.getNameAsString());
        int helperIdx = countHelpers(clazz, baseName);
        String helperName = baseName + "__h" + helperIdx;

        doExtractBlock(clazz, method, best, helperName);
        return true;
    }

    /** Extract the base function name (e.g. "func_804" from "func_804__h3"). */
    private static String baseMethodName(String name) {
        int idx = name.indexOf("__h");
        if (idx > 0 && idx < name.length() - 3) {
            // Check if everything after "__h" is digits (it's a helper suffix)
            String suffix = name.substring(idx + 3);
            if (suffix.chars().allMatch(Character::isDigit)) {
                return name.substring(0, idx);
            }
        }
        return name;
    }

    private static int countHelpers(ClassOrInterfaceDeclaration clazz, String baseName) {
        int count = 0;
        for (MethodDeclaration m : clazz.getMethods()) {
            if (m.getNameAsString().startsWith(baseName + "__h")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Find the best labeled block to extract from a method of size {@code methodSize}. Prefers the
     * block whose size is closest to half the method size. This ensures that both the remaining
     * method and the new helper are roughly half the original, giving geometric convergence.
     *
     * <p>Blocks larger than 75% of the method are rejected because extracting them just moves
     * nearly all the code to a helper without reducing it, causing the splitter to loop.
     */
    private static LabeledStmt findBestBlockToExtract(BlockStmt root, int methodSize) {
        int target = methodSize / 2;
        int maxBlockSize = (int) (methodSize * 0.75);

        final LabeledStmt[] best = {null};
        final int[] bestDist = {Integer.MAX_VALUE};

        root.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(LabeledStmt n, Void arg) {
                        // Only extract labeled blocks (not loops directly)
                        if (n.getStatement() instanceof BlockStmt) {
                            int size = n.toString().length();
                            if (size <= maxBlockSize && size >= MAX_METHOD_SIZE / 4) {
                                int dist = Math.abs(size - target);
                                if (dist < bestDist[0]) {
                                    bestDist[0] = dist;
                                    best[0] = n;
                                }
                            }
                        }
                        super.visit(n, null);
                    }
                },
                null);

        return best[0];
    }

    private static void doExtractBlock(
            ClassOrInterfaceDeclaration clazz,
            MethodDeclaration method,
            LabeledStmt labeledStmt,
            String helperName) {

        String blockLabel = labeledStmt.getLabel().getIdentifier();
        BlockStmt blockBody = (BlockStmt) labeledStmt.getStatement();

        // Find which array types are used by scanning the method parameters and body
        boolean hasInt = false;
        boolean hasLong = false;
        boolean hasFloat = false;
        boolean hasDouble = false;
        for (Parameter param : method.getParameters()) {
            switch (param.getNameAsString()) {
                case "iL":
                    hasInt = true;
                    break;
                case "lL":
                    hasLong = true;
                    break;
                case "fL":
                    hasFloat = true;
                    break;
                case "dL":
                    hasDouble = true;
                    break;
                default:
                    break;
            }
        }
        // Also check local declarations (for original func_N methods)
        BlockStmt methodBody = method.getBody().orElseThrow();
        String bodyStr = methodBody.toString();
        if (bodyStr.contains("int[] iL")) {
            hasInt = true;
        }
        if (bodyStr.contains("long[] lL")) {
            hasLong = true;
        }
        if (bodyStr.contains("float[] fL")) {
            hasFloat = true;
        }
        if (bodyStr.contains("double[] dL")) {
            hasDouble = true;
        }

        // Find outer labels: break/continue targets not defined within the block
        Set<String> definedLabels = new HashSet<>();
        collectDefinedLabels(blockBody, definedLabels);

        Map<String, String> outerLabels = new LinkedHashMap<>(); // label -> "break" or "continue"
        collectOuterReferences(blockBody, definedLabels, blockLabel, outerLabels);
        // _hb is always handled by the do-while wrapper in nested helpers,
        // don't treat as outer — the new helper also wraps in _hb: do {} while(false)
        outerLabels.remove(HELPER_BREAK_LABEL);

        // Assign return codes
        Map<String, Integer> returnCodes = new LinkedHashMap<>();
        int code = 1;
        for (String label : outerLabels.keySet()) {
            returnCodes.put(label, code++);
        }

        // Check if any return statements exist in the block (for method-level returns)
        boolean hasReturns = hasReturnStatements(blockBody);
        int returnCode = hasReturns ? code++ : -1;

        // Clone the block content for the helper
        BlockStmt innerBody = blockBody.clone();

        // For nested extractions (extracting from a helper that already has _hb: wrapper),
        // offset parent status codes to avoid collision with the new helper's own codes.
        boolean isNestedExtraction = methodBody.toString().contains("_hb:");
        if (isNestedExtraction) {
            offsetParentStatusCodes(innerBody);
        }

        // Replace outer break/continue and method returns with:
        //   _hs[0] = CODE; break _hb;
        // Self-label breaks become: break _hb; (with _hs[0] staying 0 = normal)
        String returnType = method.getType().toString();
        String returnArray = returnArrayForType(returnType);
        replaceWithBreaks(innerBody, blockLabel, returnCodes, returnCode, returnArray);

        // Find variables used in the block but not declared in it (e.g. callArgs_N,
        // callResult_N). These are long[] temporaries from the caller scope that need
        // to be re-declared inside the helper.
        Set<String> undeclaredVars = findUndeclaredArrayVars(innerBody);

        // Wrap in: int[] _hs = {0}; _hb: do { innerBody } while (false); return _hs[0];
        BlockStmt helperBody = new BlockStmt();
        helperBody.addStatement(StaticJavaParser.parseStatement("int[] _hs = { 0 };"));

        // Add declarations for undeclared array variables
        for (String varName : undeclaredVars) {
            helperBody.addStatement(
                    StaticJavaParser.parseStatement("long[] " + varName + " = null;"));
        }

        // Build the do-while wrapper: _hb: do { ... } while (false);
        BlockStmt doBody = new BlockStmt();
        for (Statement stmt : innerBody.getStatements()) {
            doBody.addStatement(stmt.clone());
        }

        // Create: do { doBody } while (false);
        com.github.javaparser.ast.stmt.DoStmt doStmt =
                new com.github.javaparser.ast.stmt.DoStmt(
                        doBody, StaticJavaParser.parseExpression("false"));

        // Wrap in label: _hb: do { ... } while (false);
        LabeledStmt labeledDoWhile = new LabeledStmt(HELPER_BREAK_LABEL, doStmt);

        helperBody.addStatement(labeledDoWhile);
        helperBody.addStatement(StaticJavaParser.parseStatement("return _hs[0];"));

        // Create the helper method
        MethodDeclaration helper =
                clazz.addMethod(helperName, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC)
                        .setType(PrimitiveType.intType());

        if (hasInt) {
            helper.addParameter(new ArrayType(PrimitiveType.intType()), "iL");
        }
        if (hasLong) {
            helper.addParameter(new ArrayType(PrimitiveType.longType()), "lL");
        }
        if (hasFloat) {
            helper.addParameter(new ArrayType(PrimitiveType.floatType()), "fL");
        }
        if (hasDouble) {
            helper.addParameter(new ArrayType(PrimitiveType.doubleType()), "dL");
        }
        helper.addParameter(
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Memory"),
                "memory");
        helper.addParameter(
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Instance"),
                "instance");

        helper.setBody(helperBody);

        // Replace the block content with dispatch code
        BlockStmt newBlockBody = new BlockStmt();

        // Build helper call with unique dispatch variable name
        // Extract the numeric suffix from the helper name for a unique dispatch var
        String helperSuffix = helperName.substring(helperName.lastIndexOf("__h") + 3);
        String dispatchVar = "_d" + helperSuffix;
        StringBuilder callExpr = new StringBuilder();
        callExpr.append("int ").append(dispatchVar).append(" = ").append(helperName).append("(");
        List<String> callArgs = new ArrayList<>();
        if (hasInt) {
            callArgs.add("iL");
        }
        if (hasLong) {
            callArgs.add("lL");
        }
        if (hasFloat) {
            callArgs.add("fL");
        }
        if (hasDouble) {
            callArgs.add("dL");
        }
        callArgs.add("memory");
        callArgs.add("instance");
        callExpr.append(String.join(", ", callArgs));
        callExpr.append(");");
        newBlockBody.addStatement(StaticJavaParser.parseStatement(callExpr.toString()));

        // Add dispatch for outer labels
        for (Map.Entry<String, Integer> entry : returnCodes.entrySet()) {
            String label = entry.getKey();
            int rc = entry.getValue();
            String branchType = outerLabels.get(label);
            newBlockBody.addStatement(
                    StaticJavaParser.parseStatement(
                            "if ("
                                    + dispatchVar
                                    + " == "
                                    + rc
                                    + ") "
                                    + branchType
                                    + " "
                                    + label
                                    + ";"));
        }

        // Add dispatch for method return
        if (hasReturns) {
            if (returnType.equals("void")) {
                newBlockBody.addStatement(
                        StaticJavaParser.parseStatement(
                                "if (" + dispatchVar + " == " + returnCode + ") return;"));
            } else {
                String arrayForReturn = returnArrayForType(returnType);
                newBlockBody.addStatement(
                        StaticJavaParser.parseStatement(
                                "if ("
                                        + dispatchVar
                                        + " == "
                                        + returnCode
                                        + ") return "
                                        + arrayForReturn
                                        + "[0];"));
            }
        }

        // Propagate offset parent status codes back to the parent's do-while.
        // Parent codes were offset by PARENT_STATUS_OFFSET to avoid collision with
        // the new helper's own return codes. Subtract the offset and propagate.
        if (isNestedExtraction) {
            newBlockBody.addStatement(
                    StaticJavaParser.parseStatement(
                            "if ("
                                    + dispatchVar
                                    + " >= "
                                    + PARENT_STATUS_OFFSET
                                    + ") { _hs[0] = ("
                                    + dispatchVar
                                    + " - "
                                    + PARENT_STATUS_OFFSET
                                    + "); break _hb; }"));
        }

        // Replace the labeled block's body
        labeledStmt.setStatement(newBlockBody);
    }

    // -----------------------------------------------------------------------
    // Switch splitting: split a large switch into multiple helper methods
    // -----------------------------------------------------------------------

    /**
     * Split a large switch statement by delegating groups of cases to helper methods. Returns true
     * if a split was performed.
     */
    private static boolean splitLargeSwitch(
            ClassOrInterfaceDeclaration clazz, MethodDeclaration method) {
        BlockStmt body = method.getBody().orElseThrow();

        // Find the largest SwitchStmt in the method body
        SwitchStmt targetSwitch = null;
        // Search top-level statements, and also inside do-while wrappers
        for (Statement stmt : body.getStatements()) {
            SwitchStmt found = findSwitchStmt(stmt);
            if (found != null) {
                if (targetSwitch == null
                        || found.toString().length() > targetSwitch.toString().length()) {
                    targetSwitch = found;
                }
            }
        }

        if (targetSwitch == null || targetSwitch.getEntries().size() < 4) {
            return false;
        }

        // Determine which array types the method uses
        boolean hasInt = false;
        boolean hasLong = false;
        boolean hasFloat = false;
        boolean hasDouble = false;
        for (Parameter param : method.getParameters()) {
            switch (param.getNameAsString()) {
                case "iL":
                    hasInt = true;
                    break;
                case "lL":
                    hasLong = true;
                    break;
                case "fL":
                    hasFloat = true;
                    break;
                case "dL":
                    hasDouble = true;
                    break;
                default:
                    break;
            }
        }
        String bodyStr = body.toString();
        if (bodyStr.contains("int[] iL")) {
            hasInt = true;
        }
        if (bodyStr.contains("long[] lL")) {
            hasLong = true;
        }
        if (bodyStr.contains("float[] fL")) {
            hasFloat = true;
        }
        if (bodyStr.contains("double[] dL")) {
            hasDouble = true;
        }

        // Split the switch entries in half
        List<SwitchEntry> entries = targetSwitch.getEntries();
        int mid = entries.size() / 2;

        // Find the split point: use the first case label value of the second half
        // to create an if/else dispatch
        List<SwitchEntry> firstHalf = new ArrayList<>(entries.subList(0, mid));
        List<SwitchEntry> secondHalf = new ArrayList<>(entries.subList(mid, entries.size()));

        String methodName = method.getNameAsString();
        int helperCount = countHelpers(clazz, methodName);

        // Build parameter name list for helpers
        List<String> paramNames = new ArrayList<>();
        if (hasInt) {
            paramNames.add("iL");
        }
        if (hasLong) {
            paramNames.add("lL");
        }
        if (hasFloat) {
            paramNames.add("fL");
        }
        if (hasDouble) {
            paramNames.add("dL");
        }
        paramNames.add("memory");
        paramNames.add("instance");

        String selector = targetSwitch.getSelector().toString();

        // Use a parameter name for the selector to avoid referencing local variables
        // in the helpers. The parent will pre-compute the selector value.
        String selectorParam = "_sel";

        // Collect outer label references from ALL switch entries.
        // These are break/continue targets that reference labels outside the switch.
        // Wrap the switch in a temp statement for label analysis.
        Map<String, String> outerLabels = new LinkedHashMap<>();
        Set<String> definedInSwitch = new HashSet<>();
        SwitchStmt tempSwitch = targetSwitch.clone();
        collectDefinedLabels(tempSwitch, definedInSwitch);
        collectOuterReferences(tempSwitch, definedInSwitch, "_hb", outerLabels);
        // _hb is always handled by the do-while wrapper, don't treat as outer
        outerLabels.remove(HELPER_BREAK_LABEL);

        // Assign status codes for outer labels (starting at 1)
        Map<String, Integer> labelCodes = new LinkedHashMap<>();
        int labelCode = 1;
        for (String label : outerLabels.keySet()) {
            labelCodes.put(label, labelCode++);
        }

        // Create helper for first half (uses _sel parameter instead of original selector)
        String helper1Name = methodName + "_helper" + helperCount;
        createSwitchHelper(
                clazz,
                helper1Name,
                selectorParam,
                firstHalf,
                hasInt,
                hasLong,
                hasFloat,
                hasDouble,
                labelCodes);

        // Create helper for second half
        String helper2Name = methodName + "_helper" + (helperCount + 1);
        createSwitchHelper(
                clazz,
                helper2Name,
                selectorParam,
                secondHalf,
                hasInt,
                hasLong,
                hasFloat,
                hasDouble,
                labelCodes);

        // Replace the switch statement with dispatch to the two helpers
        // Find the minimum case value of the second half for the split point
        int splitValue = findMinCaseValue(secondHalf);

        // Add _sel as first arg, then the standard params
        String argsStr = selectorParam + ", " + String.join(", ", paramNames);

        // Pre-compute selector value (unless the method already has _sel as a parameter),
        // then dispatch to the correct helper
        boolean methodHasSel =
                method.getParameters().stream()
                        .anyMatch(p -> p.getNameAsString().equals(selectorParam));
        String selectorDecl = methodHasSel ? "" : "int " + selectorParam + " = " + selector + "; ";

        // Build dispatch code with outer label forwarding
        StringBuilder dispatchBuilder = new StringBuilder();
        dispatchBuilder
                .append("{ ")
                .append(selectorDecl)
                .append("int _sw; if (")
                .append(selectorParam)
                .append(" < ")
                .append(splitValue)
                .append(") { _sw = ")
                .append(helper1Name)
                .append("(")
                .append(argsStr)
                .append("); } else { _sw = ")
                .append(helper2Name)
                .append("(")
                .append(argsStr)
                .append("); } ");

        // Add dispatch for outer labels
        for (Map.Entry<String, Integer> lc : labelCodes.entrySet()) {
            String label = lc.getKey();
            int lcode = lc.getValue();
            String branchType = outerLabels.get(label);
            dispatchBuilder
                    .append("if (_sw == ")
                    .append(lcode)
                    .append(") ")
                    .append(branchType)
                    .append(" ")
                    .append(label)
                    .append("; ");
        }

        // Propagate any remaining non-zero status to the parent helper's do-while.
        // Only if the method already has _hb (it's an extracted helper), otherwise
        // all label codes are explicitly dispatched and this is unreachable.
        boolean hasHelperWrapper = bodyStr.contains("_hb:");
        if (hasHelperWrapper) {
            dispatchBuilder.append("if (_sw != 0) { _hs[0] = _sw; break _hb; } ");
        }
        dispatchBuilder.append("}");

        // Replace the switch with the dispatch
        targetSwitch.replace(StaticJavaParser.parseStatement(dispatchBuilder.toString()));
        return true;
    }

    private static SwitchStmt findSwitchStmt(Statement stmt) {
        if (stmt instanceof SwitchStmt) {
            return (SwitchStmt) stmt;
        }
        if (stmt instanceof LabeledStmt) {
            return findSwitchStmt(((LabeledStmt) stmt).getStatement());
        }
        if (stmt instanceof com.github.javaparser.ast.stmt.DoStmt) {
            return findSwitchStmt(((com.github.javaparser.ast.stmt.DoStmt) stmt).getBody());
        }
        if (stmt instanceof BlockStmt) {
            for (Statement s : ((BlockStmt) stmt).getStatements()) {
                SwitchStmt found = findSwitchStmt(s);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int findMinCaseValue(List<SwitchEntry> entries) {
        for (SwitchEntry entry : entries) {
            if (!entry.getLabels().isEmpty()) {
                String label = entry.getLabels().get(0).toString();
                try {
                    return Integer.parseInt(label);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static void createSwitchHelper(
            ClassOrInterfaceDeclaration clazz,
            String helperName,
            String selector,
            List<SwitchEntry> entries,
            boolean hasInt,
            boolean hasLong,
            boolean hasFloat,
            boolean hasDouble,
            Map<String, Integer> labelCodes) {

        // Create new switch with only these entries, replacing return/break _hb
        // with status-based dispatch, and outer label breaks with status codes
        SwitchStmt newSwitch =
                new SwitchStmt(
                        StaticJavaParser.parseExpression(selector),
                        new com.github.javaparser.ast.NodeList<>());
        for (SwitchEntry entry : entries) {
            SwitchEntry cloned = entry.clone();
            // Replace "return X;" with "{ _hs[0] = X; break _hb; }"
            // and outer label breaks with "{ _hs[0] = CODE; break _hb; }"
            replaceReturnsAndBreaksWithStatus(cloned, labelCodes);
            newSwitch.getEntries().add(cloned);
        }

        // Wrap in do-while pattern: int[] _hs = {0}; _hb: do { switch } while(false); return
        // _hs[0];
        BlockStmt helperBody = new BlockStmt();
        helperBody.addStatement(StaticJavaParser.parseStatement("int[] _hs = { 0 };"));

        BlockStmt doBody = new BlockStmt();
        doBody.addStatement(newSwitch);

        com.github.javaparser.ast.stmt.DoStmt doStmt =
                new com.github.javaparser.ast.stmt.DoStmt(
                        doBody, StaticJavaParser.parseExpression("false"));
        LabeledStmt labeledDoWhile = new LabeledStmt(HELPER_BREAK_LABEL, doStmt);

        helperBody.addStatement(labeledDoWhile);
        helperBody.addStatement(StaticJavaParser.parseStatement("return _hs[0];"));

        MethodDeclaration helper =
                clazz.addMethod(helperName, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC)
                        .setType(PrimitiveType.intType());

        // First parameter is the pre-computed selector value
        helper.addParameter(PrimitiveType.intType(), selector);

        if (hasInt) {
            helper.addParameter(new ArrayType(PrimitiveType.intType()), "iL");
        }
        if (hasLong) {
            helper.addParameter(new ArrayType(PrimitiveType.longType()), "lL");
        }
        if (hasFloat) {
            helper.addParameter(new ArrayType(PrimitiveType.floatType()), "fL");
        }
        if (hasDouble) {
            helper.addParameter(new ArrayType(PrimitiveType.doubleType()), "dL");
        }
        helper.addParameter(
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Memory"),
                "memory");
        helper.addParameter(
                StaticJavaParser.parseClassOrInterfaceType("com.dylibso.chicory.runtime.Instance"),
                "instance");

        helper.setBody(helperBody);
    }

    private static void replaceReturnsAndBreaksWithStatus(
            SwitchEntry entry, Map<String, Integer> labelCodes) {
        // Collect labels defined within this entry
        Set<String> definedInEntry = new HashSet<>();
        collectDefinedLabels(entry, definedInEntry);

        entry.accept(
                new ModifierVisitor<Void>() {
                    @Override
                    public Visitable visit(ReturnStmt n, Void arg) {
                        BlockStmt block = new BlockStmt();
                        if (n.getExpression().isPresent()) {
                            String expr = n.getExpression().get().toString();
                            block.addStatement(
                                    StaticJavaParser.parseStatement("_hs[0] = " + expr + ";"));
                        }
                        block.addStatement(new BreakStmt(HELPER_BREAK_LABEL));
                        return block;
                    }

                    @Override
                    public Visitable visit(BreakStmt n, Void arg) {
                        if (n.getLabel().isPresent()) {
                            String label = n.getLabel().get().getIdentifier();
                            // Skip _hb breaks (they target our own do-while)
                            if (HELPER_BREAK_LABEL.equals(label)) {
                                return super.visit(n, null);
                            }
                            // If this break targets a label defined within the entry, keep it
                            if (definedInEntry.contains(label)) {
                                return super.visit(n, null);
                            }
                            // Outer label break: convert to status code
                            if (labelCodes.containsKey(label)) {
                                BlockStmt block = new BlockStmt();
                                block.addStatement(
                                        StaticJavaParser.parseStatement(
                                                "_hs[0] = " + labelCodes.get(label) + ";"));
                                block.addStatement(new BreakStmt(HELPER_BREAK_LABEL));
                                return block;
                            }
                        }
                        return super.visit(n, null);
                    }
                },
                null);
    }

    private static String returnArrayForType(String returnType) {
        switch (returnType) {
            case "int":
                return "iL";
            case "long":
                return "lL";
            case "float":
                return "fL";
            case "double":
                return "dL";
            default:
                return "iL";
        }
    }

    // -----------------------------------------------------------------------
    // Undeclared variable detection
    // -----------------------------------------------------------------------

    /**
     * Find array variable names used in the block but not declared within it. These are typically
     * long[] temporaries (callArgs_N, callResult_N) declared at the parent method scope.
     */
    private static Set<String> findUndeclaredArrayVars(BlockStmt body) {
        // Find all names used as array access targets (e.g., callResult_1[0])
        // or assigned to directly (e.g., callResult_1 = new long[2])
        Set<String> usedNames = new HashSet<>();
        body.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(ArrayAccessExpr n, Void arg) {
                        if (n.getName() instanceof NameExpr) {
                            usedNames.add(((NameExpr) n.getName()).getNameAsString());
                        }
                        super.visit(n, null);
                    }

                    @Override
                    public void visit(com.github.javaparser.ast.expr.AssignExpr n, Void arg) {
                        if (n.getTarget() instanceof NameExpr) {
                            usedNames.add(((NameExpr) n.getTarget()).getNameAsString());
                        }
                        super.visit(n, null);
                    }
                },
                null);

        // Find all variable names declared within the block
        Set<String> declared = new HashSet<>();
        body.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(
                            com.github.javaparser.ast.expr.VariableDeclarationExpr n, Void arg) {
                        for (com.github.javaparser.ast.body.VariableDeclarator v :
                                n.getVariables()) {
                            declared.add(v.getNameAsString());
                        }
                        super.visit(n, null);
                    }
                },
                null);

        // Known names that are always available as parameters or status arrays
        Set<String> known = new HashSet<>();
        known.add("iL");
        known.add("lL");
        known.add("fL");
        known.add("dL");
        known.add("memory");
        known.add("instance");
        known.add("_hs");

        usedNames.removeAll(declared);
        usedNames.removeAll(known);
        return usedNames;
    }

    // -----------------------------------------------------------------------
    // Label analysis
    // -----------------------------------------------------------------------

    private static void collectDefinedLabels(
            com.github.javaparser.ast.Node stmt, Set<String> labels) {
        stmt.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(LabeledStmt n, Void arg) {
                        labels.add(n.getLabel().getIdentifier());
                        super.visit(n, null);
                    }
                },
                null);
    }

    private static void collectOuterReferences(
            com.github.javaparser.ast.Node stmt,
            Set<String> definedLabels,
            String selfLabel,
            Map<String, String> outerLabels) {
        stmt.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(BreakStmt n, Void arg) {
                        n.getLabel()
                                .ifPresent(
                                        label -> {
                                            String name = label.getIdentifier();
                                            if (!definedLabels.contains(name)) {
                                                // Self-label break means normal completion
                                                if (!name.equals(selfLabel)) {
                                                    outerLabels.putIfAbsent(name, "break");
                                                }
                                            }
                                        });
                        super.visit(n, null);
                    }

                    @Override
                    public void visit(ContinueStmt n, Void arg) {
                        n.getLabel()
                                .ifPresent(
                                        label -> {
                                            String name = label.getIdentifier();
                                            if (!definedLabels.contains(name)) {
                                                outerLabels.putIfAbsent(name, "continue");
                                            }
                                        });
                        super.visit(n, null);
                    }
                },
                null);
    }

    private static boolean hasReturnStatements(Statement stmt) {
        final boolean[] found = {false};
        stmt.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(ReturnStmt n, Void arg) {
                        found[0] = true;
                    }
                },
                null);
        return found[0];
    }

    // -----------------------------------------------------------------------
    // Parent status code offsetting for nested extractions
    // -----------------------------------------------------------------------

    /**
     * When extracting a block from a method that is already a helper (has _hb: wrapper),
     * the extracted block may contain {@code _hs[0] = CODE; break _hb;} patterns from the
     * parent's status system. These codes must be offset to avoid collision with the new
     * helper's own return codes.
     *
     * <p>Also handles bare {@code break _hb;} (parent self-label break = status 0) by
     * inserting {@code _hs[0] = PARENT_STATUS_OFFSET;} before the break.
     */
    private static void offsetParentStatusCodes(BlockStmt body) {
        // Step 1: Offset _hs[0] = CODE in { ...; _hs[0] = CODE; break _hb; } patterns
        body.accept(
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(BlockStmt n, Void arg) {
                        // Visit children first
                        super.visit(n, null);

                        List<Statement> stmts = n.getStatements();
                        for (int i = stmts.size() - 1; i >= 0; i--) {
                            if (isBreakHb(stmts.get(i))
                                    && i > 0
                                    && isHsAssignment(stmts.get(i - 1))) {
                                String valueStr = getHsValueString(stmts.get(i - 1));
                                String offsetExpr;
                                try {
                                    int code = Integer.parseInt(valueStr);
                                    offsetExpr = String.valueOf(code + PARENT_STATUS_OFFSET);
                                } catch (NumberFormatException e) {
                                    // Non-integer expression (e.g., "(_d2 - 10000)" from a
                                    // previous nested extraction dispatch) — wrap with offset
                                    offsetExpr = "(" + valueStr + ") + " + PARENT_STATUS_OFFSET;
                                }
                                stmts.set(
                                        i - 1,
                                        StaticJavaParser.parseStatement(
                                                "_hs[0] = " + offsetExpr + ";"));
                            }
                        }
                    }
                },
                null);

        // Step 2: Handle bare break _hb; (not preceded by _hs[0] = CODE;)
        // by wrapping them with _hs[0] = PARENT_STATUS_OFFSET; break _hb;
        body.accept(
                new ModifierVisitor<Void>() {
                    @Override
                    public Visitable visit(BreakStmt n, Void arg) {
                        if (!isBreakHb(n)) {
                            return super.visit(n, null);
                        }

                        // Check if preceded by _hs[0] assignment (already offset)
                        com.github.javaparser.ast.Node parent = n.getParentNode().orElse(null);
                        if (parent instanceof BlockStmt) {
                            BlockStmt parentBlock = (BlockStmt) parent;
                            int idx = parentBlock.getStatements().indexOf(n);
                            if (idx > 0
                                    && isHsAssignment(parentBlock.getStatements().get(idx - 1))) {
                                // Already handled in step 1
                                return super.visit(n, null);
                            }
                        }

                        // Bare break _hb; → wrap with offset status
                        BlockStmt wrapper = new BlockStmt();
                        wrapper.addStatement(
                                StaticJavaParser.parseStatement(
                                        "_hs[0] = " + PARENT_STATUS_OFFSET + ";"));
                        wrapper.addStatement(new BreakStmt(HELPER_BREAK_LABEL));
                        return wrapper;
                    }
                },
                null);
    }

    private static boolean isBreakHb(Statement stmt) {
        if (stmt instanceof BreakStmt) {
            return ((BreakStmt) stmt)
                    .getLabel()
                    .map(l -> l.getIdentifier().equals(HELPER_BREAK_LABEL))
                    .orElse(false);
        }
        return false;
    }

    private static boolean isHsAssignment(Statement stmt) {
        if (!(stmt instanceof ExpressionStmt)) {
            return false;
        }
        ExpressionStmt exprStmt = (ExpressionStmt) stmt;
        if (!(exprStmt.getExpression() instanceof com.github.javaparser.ast.expr.AssignExpr)) {
            return false;
        }
        com.github.javaparser.ast.expr.AssignExpr assign =
                (com.github.javaparser.ast.expr.AssignExpr) exprStmt.getExpression();
        if (!(assign.getTarget() instanceof ArrayAccessExpr)) {
            return false;
        }
        ArrayAccessExpr target = (ArrayAccessExpr) assign.getTarget();
        return target.getName().toString().equals("_hs")
                && target.getIndex().toString().equals("0");
    }

    private static String getHsValueString(Statement stmt) {
        ExpressionStmt exprStmt = (ExpressionStmt) stmt;
        com.github.javaparser.ast.expr.AssignExpr assign =
                (com.github.javaparser.ast.expr.AssignExpr) exprStmt.getExpression();
        return assign.getValue().toString();
    }

    // -----------------------------------------------------------------------
    // Branch replacement: convert return/break/continue to break _hb with status
    // -----------------------------------------------------------------------

    private static void replaceWithBreaks(
            BlockStmt body,
            String selfLabel,
            Map<String, Integer> returnCodes,
            int methodReturnCode,
            String returnArray) {

        // Collect labels defined within the helper body
        Set<String> definedLabels = new HashSet<>();
        collectDefinedLabels(body, definedLabels);

        body.accept(
                new ModifierVisitor<Void>() {
                    @Override
                    public Visitable visit(BreakStmt n, Void arg) {
                        if (n.getLabel().isPresent()) {
                            String label = n.getLabel().get().getIdentifier();
                            if (label.equals(selfLabel)) {
                                // Self-label break = normal completion, status stays 0
                                return new BreakStmt(HELPER_BREAK_LABEL);
                            }
                            if (!definedLabels.contains(label) && returnCodes.containsKey(label)) {
                                BlockStmt block = new BlockStmt();
                                block.addStatement(
                                        StaticJavaParser.parseStatement(
                                                "_hs[0] = " + returnCodes.get(label) + ";"));
                                block.addStatement(new BreakStmt(HELPER_BREAK_LABEL));
                                return block;
                            }
                        }
                        return super.visit(n, null);
                    }

                    @Override
                    public Visitable visit(ContinueStmt n, Void arg) {
                        if (n.getLabel().isPresent()) {
                            String label = n.getLabel().get().getIdentifier();
                            if (!definedLabels.contains(label) && returnCodes.containsKey(label)) {
                                BlockStmt block = new BlockStmt();
                                block.addStatement(
                                        StaticJavaParser.parseStatement(
                                                "_hs[0] = " + returnCodes.get(label) + ";"));
                                block.addStatement(new BreakStmt(HELPER_BREAK_LABEL));
                                return block;
                            }
                        }
                        return super.visit(n, null);
                    }

                    @Override
                    public Visitable visit(ReturnStmt n, Void arg) {
                        if (methodReturnCode >= 0) {
                            BlockStmt block = new BlockStmt();
                            if (n.getExpression().isPresent()) {
                                String expr = n.getExpression().get().toString();
                                block.addStatement(
                                        StaticJavaParser.parseStatement(
                                                returnArray + "[0] = " + expr + ";"));
                            }
                            block.addStatement(
                                    StaticJavaParser.parseStatement(
                                            "_hs[0] = " + methodReturnCode + ";"));
                            block.addStatement(new BreakStmt(HELPER_BREAK_LABEL));
                            return block;
                        }
                        return super.visit(n, null);
                    }
                },
                null);
    }
}
