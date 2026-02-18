package com.dylibso.chicory.wasm;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.ActiveElement;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.ArrayType;
import com.dylibso.chicory.wasm.types.CatchOpCode;
import com.dylibso.chicory.wasm.types.CompType;
import com.dylibso.chicory.wasm.types.DeclarativeElement;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FieldType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalImport;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.StorageType;
import com.dylibso.chicory.wasm.types.StructType;
import com.dylibso.chicory.wasm.types.SubType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableImport;
import com.dylibso.chicory.wasm.types.TagImport;
import com.dylibso.chicory.wasm.types.TagSection;
import com.dylibso.chicory.wasm.types.TagType;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

// Heavily inspired by wazero
// https://github.com/tetratelabs/wazero/blob/5a8a053bff0ae795b264de9672016745cb842070/internal/wasm/func_validation.go
// control flow implementation follows:
// https://webassembly.github.io/spec/core/appendix/algorithm.html
final class Validator {

    private static boolean isNum(ValType t) {
        return t.isNumeric() || t.equals(ValType.BOT);
    }

    private static boolean isRef(ValType t) {
        return t.isReference() || t.equals(ValType.BOT);
    }

    @SuppressWarnings("PublicField")
    private static class CtrlFrame {
        // OpCode of the current Control Flow instruction
        final OpCode opCode;
        // params or inputs
        final List<ValType> startTypes;
        // returns or outputs
        final List<ValType> endTypes;
        // the height of the value stack before entering the current Control Flow instruction
        final int height;
        // the height of the init stack before entering the current Control Flow
        final int initHeight;
        // set after unconditional jumps
        boolean unreachable;
        // if there is no else, we explicit check that the enclosing IF is not returning values
        boolean hasElse;

        CtrlFrame(
                OpCode opCode,
                List<ValType> startTypes,
                List<ValType> endTypes,
                int height,
                int initHeight,
                boolean unreachable,
                boolean hasElse) {
            this.opCode = opCode;
            this.startTypes = startTypes;
            this.endTypes = endTypes;
            this.initHeight = initHeight;
            this.height = height;
            this.unreachable = unreachable;
            this.hasElse = hasElse;
        }
    }

    private final List<ValType> valTypeStack = new ArrayList<>();
    private final List<CtrlFrame> ctrlFrameStack = new ArrayList<>();
    private final List<Integer> initStack = new ArrayList<>();

    private final List<InvalidException> errors = new ArrayList<>();

    private final WasmModule module;
    private final List<ValType> locals;
    private final List<Boolean> localsInitialized;
    private final List<Global> globalImports;
    private final List<Integer> functionImports;
    private final List<ValType> tableImports;
    private final List<TagType> tagImports;
    private final int memoryImports;
    private final Set<Integer> declaredFunctions;

    Validator(WasmModule module) {
        this.module = requireNonNull(module);

        this.locals = new ArrayList<>();
        this.localsInitialized = new ArrayList<>();

        this.globalImports =
                module.importSection().stream()
                        .filter(GlobalImport.class::isInstance)
                        .map(GlobalImport.class::cast)
                        .map(i -> new Global(i.type(), i.mutabilityType(), List.of()))
                        .collect(toList());

        this.functionImports =
                module.importSection().stream()
                        .filter(FunctionImport.class::isInstance)
                        .map(FunctionImport.class::cast)
                        .map(FunctionImport::typeIndex)
                        .collect(toList());

        this.tableImports =
                module.importSection().stream()
                        .filter(TableImport.class::isInstance)
                        .map(TableImport.class::cast)
                        .map(TableImport::entryType)
                        .collect(toList());

        this.memoryImports = module.importSection().count(ExternalType.MEMORY);

        this.declaredFunctions =
                module.elementSection().stream()
                        .filter(DeclarativeElement.class::isInstance)
                        .flatMap(element -> element.initializers().stream())
                        .flatMap(this::declaredFunctions)
                        .collect(toSet());

        this.tagImports =
                module.importSection().stream()
                        .filter(TagImport.class::isInstance)
                        .map(TagImport.class::cast)
                        .map(TagImport::tagType)
                        .collect(toList());
    }

    private Stream<Integer> declaredFunctions(List<Instruction> init) {
        if (!init.isEmpty() && init.get(0).opcode() == OpCode.REF_FUNC) {
            int idx = (int) init.get(0).operand(0);
            getFunctionType(idx);
            if (idx >= functionImports.size()) {
                return Stream.of(idx);
            }
        }
        return Stream.empty();
    }

    private TypeSection typeSection() {
        return module.typeSection();
    }

    private boolean typeMatches(ValType t1, ValType t2) {
        return ValType.matches(t1, t2, typeSection());
    }

    private void pushVal(ValType valType) {
        valTypeStack.add(valType);
    }

    private ValType popVal() {
        var frame = peekCtrl();
        if (valTypeStack.size() == frame.height && frame.unreachable) {
            return ValType.BOT;
        }
        if (valTypeStack.size() == frame.height) {
            errors.add(
                    new InvalidException(
                            "type mismatch: instruction requires [i32] but stack has []"));
            return ValType.BOT;
        }
        return valTypeStack.remove(valTypeStack.size() - 1);
    }

    private ValType popVal(ValType expected) {
        var actual = popVal();
        if (!typeMatches(actual, expected)
                && !actual.equals(ValType.BOT)
                && !expected.equals(ValType.BOT)) {
            errors.add(
                    new InvalidException(
                            "type mismatch: instruction requires ["
                                    + expected.toString().toLowerCase(Locale.ROOT)
                                    + "] but stack has ["
                                    + actual.toString().toLowerCase(Locale.ROOT)
                                    + "]"));
        }
        return actual;
    }

    private ValType popRef() {
        var actual = popVal();
        if (!isRef(actual)) {
            errors.add(
                    new InvalidException(
                            "type mismatch, popRef(), expected reference type"
                                    + " but got: "
                                    + actual));
        }
        if (actual.equals(ValType.BOT)) {
            return ValType.RefBot;
        }

        return actual;
    }

    private void pushVals(List<ValType> valTypes) {
        for (var t : valTypes) {
            pushVal(t);
        }
    }

    private List<ValType> popVals(List<ValType> valTypes) {
        var popped = new ValType[valTypes.size()];
        for (int i = valTypes.size() - 1; i >= 0; i--) {
            popped[i] = popVal(valTypes.get(i));
        }
        return Arrays.asList(popped);
    }

    private ValType getLocal(int idx) {
        if (idx >= locals.size()) {
            throw new InvalidException("unknown local " + idx);
        }
        if (!localsInitialized.get(idx)) {
            errors.add(new InvalidException("uninitialized local: index " + idx));
        }
        return getLocalType(idx);
    }

    private void setLocal(int idx) {
        if (idx >= locals.size()) {
            throw new InvalidException("unknown local " + idx);
        }
        if (!localsInitialized.get(idx)) {
            initStack.add(idx);
            localsInitialized.set(idx, true);
        }
    }

    private void resetLocals(int height) {
        while (initStack.size() > height) {
            localsInitialized.set(initStack.remove(initStack.size() - 1), false);
        }
    }

    private void pushCtrl(OpCode opCode, List<ValType> in, List<ValType> out) {
        var frame =
                new CtrlFrame(opCode, in, out, valTypeStack.size(), initStack.size(), false, false);
        pushCtrl(frame);
        pushVals(in);
    }

    private void pushCtrl(CtrlFrame frame) {
        ctrlFrameStack.add(frame);
    }

    private CtrlFrame popCtrl() {
        if (ctrlFrameStack.isEmpty()) {
            errors.add(new InvalidException("type mismatch, control frame stack empty"));
        }
        var frame = peekCtrl();
        popVals(frame.endTypes);
        if (valTypeStack.size() != frame.height) {
            errors.add(new InvalidException("type mismatch, mismatching stack height"));
        }
        resetLocals(frame.initHeight);
        ctrlFrameStack.remove(ctrlFrameStack.size() - 1);
        return frame;
    }

    private CtrlFrame peekCtrl() {
        return ctrlFrameStack.get(ctrlFrameStack.size() - 1);
    }

    private CtrlFrame getCtrl(int n) {
        return ctrlFrameStack.get(ctrlFrameStack.size() - 1 - n);
    }

    private static List<ValType> labelTypes(CtrlFrame frame) {
        return (frame.opCode == OpCode.LOOP) ? frame.startTypes : frame.endTypes;
    }

    private void resetAtStackLimit() {
        var frame = peekCtrl();
        while (valTypeStack.size() > frame.height) {
            valTypeStack.remove(valTypeStack.size() - 1);
        }
    }

    private void unreachable() {
        var frame = peekCtrl();
        resetAtStackLimit();
        frame.unreachable = true;
    }

    private void validateMemory(int id) {
        if ((module.memorySection().isEmpty() && memoryImports == 0) || id != 0) {
            throw new InvalidException("unknown memory " + id);
        }
    }

    private void validateMemAlign(long current, long expected) {
        if (current != expected) {
            throw new InvalidException(
                    "invalid memory alignement, current: " + current + ", expected: " + expected);
        }
    }

    private void validateLane(int id, int max) {
        if (id < 0 || id >= max) {
            throw new InvalidException("invalid lane index " + id + " max is " + max);
        }
    }

    private void validateDataSegment(int idx) {
        if (idx < 0 || idx >= module.dataSection().dataSegmentCount()) {
            throw new InvalidException("unknown data segment " + idx);
        }
    }

    private ValType valType(long id) {
        return ValType.builder().fromId(id).build().resolve(module.typeSection());
    }

    private ValType valType(int opcode, int typeIdx) {
        return ValType.builder()
                .withOpcode(opcode)
                .withTypeIdx(typeIdx)
                .build()
                .resolve(module.typeSection());
    }

    private List<ValType> getReturns(AnnotatedInstruction op) {
        var typeId = op.operand(0);
        if (typeId == 0x40) { // epsilon
            return List.of();
        }
        if (ValType.isValid(typeId)) {
            return List.of(valType(typeId));
        }
        return getType((int) typeId).returns();
    }

    private List<ValType> getParams(AnnotatedInstruction op) {
        var typeId = op.operand(0);
        if (typeId == 0x40) { // epsilon
            return List.of();
        }
        if (ValType.isValid(typeId)) {
            return List.of();
        }
        if (typeId >= module.typeSection().subTypeCount()) {
            throw new MalformedException("unexpected end");
        }
        return getType((int) typeId).params();
    }

    private ValType getLocalType(int idx) {
        if (idx >= locals.size()) {
            throw new InvalidException("unknown local " + idx);
        }
        return locals.get(idx);
    }

    private FunctionType getType(int idx) {
        if (idx < 0 || idx >= module.typeSection().subTypeCount()) {
            throw new InvalidException("unknown type " + idx);
        }
        return module.typeSection().getType(idx);
    }

    private Global getGlobal(int idx) {
        if (idx < 0 || idx >= globalImports.size() + module.globalSection().globalCount()) {
            throw new InvalidException("unknown global " + idx);
        }
        if (idx < globalImports.size()) {
            return globalImports.get(idx);
        }
        return module.globalSection().getGlobal(idx - globalImports.size());
    }

    private int getFunctionType(int idx) {
        if (idx < 0 || idx >= functionImports.size() + module.functionSection().functionCount()) {
            throw new InvalidException("unknown function " + idx);
        }
        if (idx < functionImports.size()) {
            return functionImports.get(idx);
        }
        return module.functionSection().getFunctionType(idx - functionImports.size());
    }

    private ValType getTableType(int idx) {
        if (idx < 0 || idx >= tableImports.size() + module.tableSection().tableCount()) {
            throw new InvalidException("unknown table " + idx);
        }
        if (idx < tableImports.size()) {
            return tableImports.get(idx);
        }
        return module.tableSection().getTable(idx - tableImports.size()).elementType();
    }

    private TagType getTagType(int idx) {
        var tagCount = module.tagSection().map(ts -> ts.tagCount()).orElse(0);
        if (idx < 0 || idx >= tagImports.size() + tagCount) {
            throw new InvalidException("unknown tag " + idx);
        }
        if (idx < tagImports.size()) {
            return tagImports.get(idx);
        }
        return module.tagSection().get().getTag(idx - tagImports.size());
    }

    private Element getElement(int idx) {
        if (idx < 0 || idx >= module.elementSection().elementCount()) {
            throw new InvalidException("unknown elem segment " + idx);
        }
        return module.elementSection().getElement(idx);
    }

    // GC helpers

    private SubType getSubType(int idx) {
        if (idx < 0 || idx >= module.typeSection().subTypeCount()) {
            throw new InvalidException("unknown type " + idx);
        }
        return module.typeSection().getSubType(idx);
    }

    private StructType getStructType(int idx) {
        var st = getSubType(idx);
        if (st.compType().structType() == null) {
            throw new InvalidException("expected struct type at index " + idx);
        }
        return st.compType().structType();
    }

    private ArrayType getArrayType(int idx) {
        var st = getSubType(idx);
        if (st.compType().arrayType() == null) {
            throw new InvalidException("expected array type at index " + idx);
        }
        return st.compType().arrayType();
    }

    private ValType unpackStorageType(StorageType st) {
        if (st.packedType() != null) {
            return ValType.I32;
        }
        return st.valType();
    }

    private ValType unpackFieldType(FieldType ft) {
        return unpackStorageType(ft.storageType());
    }

    /**
     * Returns the "top" abstract heap type for a given heap type index.
     * Concrete struct/array → any; concrete func → func; abstract types → their top.
     */
    private int topOfHeapType(int heapTypeIdx) {
        if (heapTypeIdx >= 0) {
            var st = getSubType(heapTypeIdx);
            if (st.compType().funcType() != null) {
                return ValType.TypeIdxCode.FUNC.code();
            }
            return ValType.TypeIdxCode.ANY.code();
        }
        switch (heapTypeIdx) {
            case -18: // ANY
            case -15: // NONE
            case -19: // EQ
            case -21: // STRUCT
            case -22: // ARRAY
            case -20: // I31
                return ValType.TypeIdxCode.ANY.code();
            case -16: // FUNC
            case -13: // NOFUNC
                return ValType.TypeIdxCode.FUNC.code();
            case -17: // EXTERN
            case -14: // NOEXTERN
                return ValType.TypeIdxCode.EXTERN.code();
            case -23: // EXN
                return ValType.TypeIdxCode.EXN.code();
            default:
                return heapTypeIdx;
        }
    }

    void validateModule() {
        if (module.functionSection().functionCount() != module.codeSection().functionBodyCount()) {
            throw new MalformedException("function and code section have inconsistent lengths");
        }

        if (module.dataCountSection()
                .map(dcs -> dcs.dataCount() != module.dataSection().dataSegmentCount())
                .orElse(false)) {
            throw new MalformedException("data count and data section have inconsistent lengths");
        }

        if (module.startSection().isPresent()) {
            long index = module.startSection().get().startIndex();
            if (index < 0 || index > Integer.MAX_VALUE) {
                throw new InvalidException("unknown function " + index);
            }
            var type = getType(getFunctionType((int) index));
            if (!type.params().isEmpty() || !type.returns().isEmpty()) {
                throw new InvalidException(
                        "invalid start function, must have empty signature " + type);
            }
        }
    }

    void validateData() {
        // Validate offsets.
        int allGlobals = globalImports.size() + module.globalSection().globalCount();
        for (var ds : module.dataSection().dataSegments()) {
            if (ds instanceof ActiveDataSegment) {
                var ads = (ActiveDataSegment) ds;
                if (ads.index() != 0) {
                    throw new InvalidException("unknown memory " + ads.index());
                }
                validateConstantExpression(ads.offsetInstructions(), ValType.I32, allGlobals);
            }
        }
    }

    void validateTypes() {
        int subTypeBase = 0;
        for (var i = 0; i < module.typeSection().typeCount(); i++) {
            var t = module.typeSection().getRecType(i);
            int groupSize = t.subTypes().length;
            // The valid range is [0, subTypeBase + groupSize) - within the current
            // recursion group forward refs are allowed, outside they are not
            int validUpperBound = subTypeBase + groupSize;
            int flatIdx = subTypeBase;
            for (var st : t.subTypes()) {
                var comp = st.compType();
                if (comp.funcType() != null) {
                    validateTypeRefs(comp.funcType().params(), validUpperBound);
                    validateTypeRefs(comp.funcType().returns(), validUpperBound);
                }
                if (comp.structType() != null) {
                    for (var ft : comp.structType().fieldTypes()) {
                        validateStorageTypeRefs(ft.storageType(), validUpperBound);
                    }
                }
                if (comp.arrayType() != null) {
                    validateStorageTypeRefs(
                            comp.arrayType().fieldType().storageType(), validUpperBound);
                }
                // Validate supertype references and subtype validity
                for (int sup : st.typeIdx()) {
                    if (sup < 0 || sup >= validUpperBound) {
                        throw new InvalidException("unknown type " + sup);
                    }
                    var superSt = module.typeSection().getSubType(sup);
                    if (superSt.isFinal()) {
                        throw new InvalidException(
                                "sub type " + flatIdx + " does not match super type");
                    }
                    validateSubtypeMatch(flatIdx, st.compType(), superSt.compType());
                }
                flatIdx++;
            }
            subTypeBase += groupSize;
        }
    }

    private void validateStorageTypeRefs(StorageType st, int validUpperBound) {
        if (st.valType() != null) {
            validateTypeRefs(java.util.List.of(st.valType()), validUpperBound);
        }
    }

    private void validateSubtypeMatch(int flatIdx, CompType sub, CompType sup) {
        // Both must be the same kind (func/struct/array)
        if (sub.funcType() != null && sup.funcType() != null) {
            validateFuncSubtype(flatIdx, sub.funcType(), sup.funcType());
        } else if (sub.structType() != null && sup.structType() != null) {
            validateStructSubtype(flatIdx, sub.structType(), sup.structType());
        } else if (sub.arrayType() != null && sup.arrayType() != null) {
            validateFieldSubtype(flatIdx, sub.arrayType().fieldType(), sup.arrayType().fieldType());
        } else {
            throw new InvalidException("sub type " + flatIdx + " does not match super type");
        }
    }

    private void validateFuncSubtype(int flatIdx, FunctionType sub, FunctionType sup) {
        // Contravariant params, covariant returns
        if (sub.params().size() != sup.params().size()
                || sub.returns().size() != sup.returns().size()) {
            throw new InvalidException("sub type " + flatIdx + " does not match super type");
        }
        for (int i = 0; i < sub.params().size(); i++) {
            if (!typeMatches(sup.params().get(i), sub.params().get(i))) {
                throw new InvalidException("sub type " + flatIdx + " does not match super type");
            }
        }
        for (int i = 0; i < sub.returns().size(); i++) {
            if (!typeMatches(sub.returns().get(i), sup.returns().get(i))) {
                throw new InvalidException("sub type " + flatIdx + " does not match super type");
            }
        }
    }

    private void validateStructSubtype(int flatIdx, StructType sub, StructType sup) {
        // Subtype must have at least as many fields
        if (sub.fieldTypes().length < sup.fieldTypes().length) {
            throw new InvalidException("sub type " + flatIdx + " does not match super type");
        }
        // First N fields must match
        for (int i = 0; i < sup.fieldTypes().length; i++) {
            validateFieldSubtype(flatIdx, sub.fieldTypes()[i], sup.fieldTypes()[i]);
        }
    }

    private void validateFieldSubtype(int flatIdx, FieldType sub, FieldType sup) {
        if (sub.mut() != sup.mut()) {
            throw new InvalidException("sub type " + flatIdx + " does not match super type");
        }
        if (sub.mut() == MutabilityType.Var) {
            // Mutable fields must be invariant (both directions)
            if (!storageTypeMatches(sub.storageType(), sup.storageType())
                    || !storageTypeMatches(sup.storageType(), sub.storageType())) {
                throw new InvalidException("sub type " + flatIdx + " does not match super type");
            }
        } else {
            // Immutable fields are covariant
            if (!storageTypeMatches(sub.storageType(), sup.storageType())) {
                throw new InvalidException("sub type " + flatIdx + " does not match super type");
            }
        }
    }

    private boolean storageTypeMatches(StorageType sub, StorageType sup) {
        if (sub.packedType() != null || sup.packedType() != null) {
            return java.util.Objects.equals(sub.packedType(), sup.packedType());
        }
        return typeMatches(sub.valType(), sup.valType());
    }

    private void validateTypeRefs(java.util.List<ValType> types, int validUpperBound) {
        for (var v : types) {
            if (v.isReference() && v.typeIdx() >= 0) {
                if (v.typeIdx() >= validUpperBound) {
                    throw new InvalidException("unknown type " + v.typeIdx());
                }
            }
        }
    }

    void validateTags() {
        for (var tagType : module.tagSection().map(ts -> ts.types()).orElse(new TagType[0])) {
            var type = module.typeSection().getType(tagType.typeIdx());
            if (type.returns().size() > 0) {
                throw new InvalidException("non-empty tag result type index: " + tagType.typeIdx());
            }
        }
    }

    void validateTables() {
        int allGlobals = globalImports.size() + module.globalSection().globalCount();
        for (int i = 0; i < module.tableSection().tableCount(); i++) {
            Table t = module.tableSection().getTable(i);
            validateConstantExpression(t.initialize(), t.elementType(), allGlobals);
        }
    }

    void validateElements() {
        // Validate offsets.
        var totalFunctions =
                module.functionSection().functionCount()
                        + module.importSection().stream()
                                .filter(i -> i.importType() == ExternalType.FUNCTION)
                                .count();
        int allGlobals = globalImports.size() + module.globalSection().globalCount();
        for (Element el : module.elementSection().elements()) {
            validateValueType(el.type());
            if (el instanceof ActiveElement) {
                var ae = (ActiveElement) el;
                if (!typeMatches(ae.type(), getTableType(ae.tableIndex()))) {
                    throw new InvalidException(
                            "type mismatch, active element doesn't match table type");
                }
                validateConstantExpression(ae.offset(), ValType.I32, allGlobals);
                for (int i = 0; i < ae.initializers().size(); i++) {
                    var initializers = ae.initializers().get(i);
                    for (var init : initializers) {
                        if (init.opcode() == OpCode.REF_FUNC) {
                            var idx = init.operands()[0];
                            if (idx < 0 || idx >= totalFunctions) {
                                throw new InvalidException("unknown function " + idx);
                            }
                        }
                    }
                    validateConstantExpression(
                            ae.initializers().get(i), getTableType(ae.tableIndex()), allGlobals);
                }
            }
        }
    }

    void validateGlobals() {
        var globalSection = module.globalSection();
        for (int i = 0; i < globalSection.globalCount(); i++) {
            Global g = globalSection.getGlobal(i);
            // Pass the absolute index (imports + current) as the limit for global.get
            validateConstantExpression(
                    g.initInstructions(), g.valueType(), globalImports.size() + i);
        }
    }

    private void validateConstantExpression(
            List<? extends Instruction> expr, ValType expectedType, int maxGlobalIdx) {
        validateValueType(expectedType);
        int allFuncCount = this.functionImports.size() + module.functionSection().functionCount();
        var valTypeStack = new ArrayDeque<ValType>();
        for (var instruction : expr) {
            switch (instruction.opcode()) {
                case I32_CONST:
                    valTypeStack.push(ValType.I32);
                    break;
                case I32_ADD:
                case I32_SUB:
                case I32_MUL:
                    valTypeStack.pop();
                    valTypeStack.pop();
                    valTypeStack.push(ValType.I32);
                    break;
                case I64_CONST:
                    valTypeStack.push(ValType.I64);
                    break;
                case I64_ADD:
                case I64_SUB:
                case I64_MUL:
                    valTypeStack.pop();
                    valTypeStack.pop();
                    valTypeStack.push(ValType.I64);
                    break;
                case F32_CONST:
                    valTypeStack.push(ValType.F32);
                    break;
                case F64_CONST:
                    valTypeStack.push(ValType.F64);
                    break;
                case V128_CONST:
                    valTypeStack.push(ValType.V128);
                    break;
                case REF_NULL:
                    {
                        int operand = (int) instruction.operand(0);
                        valTypeStack.push(valType(ValType.ID.RefNull, operand));
                        break;
                    }
                case REF_FUNC:
                    {
                        int idx = (int) instruction.operand(0);
                        valTypeStack.push(valType(ValType.ID.Ref, getFunctionType(idx)));
                        if (idx < 0 || idx > allFuncCount) {
                            throw new InvalidException("unknown function " + idx);
                        }
                        break;
                    }
                case GLOBAL_GET:
                    {
                        var idx = (int) instruction.operand(0);
                        if (idx < 0 || idx >= maxGlobalIdx) {
                            throw new InvalidException(
                                    "unknown global "
                                            + idx
                                            + ", initializer expression can only reference"
                                            + " an imported or preceding global");
                        }
                        if (idx < globalImports.size()) {
                            var global = globalImports.get(idx);
                            if (global.mutabilityType() != MutabilityType.Const) {
                                throw new InvalidException(
                                        "constant expression required, initializer expression"
                                                + " cannot reference a mutable global");
                            }
                            valTypeStack.push(global.valueType());
                        } else {
                            // Reference to a preceding module global
                            int moduleGlobalIdx = idx - globalImports.size();
                            var global = module.globalSection().getGlobal(moduleGlobalIdx);
                            if (global.mutabilityType() != MutabilityType.Const) {
                                throw new InvalidException(
                                        "constant expression required, initializer expression"
                                                + " cannot reference a mutable global");
                            }
                            valTypeStack.push(global.valueType());
                        }
                        break;
                    }
                case REF_I31:
                    valTypeStack.pop(); // I32
                    valTypeStack.push(
                            ValType.builder()
                                    .withOpcode(ValType.ID.Ref)
                                    .withTypeIdx(ValType.TypeIdxCode.I31.code())
                                    .build());
                    break;
                case STRUCT_NEW:
                    {
                        int typeIdx = (int) instruction.operand(0);
                        var st = getStructType(typeIdx);
                        for (int f = 0; f < st.fieldTypes().length; f++) {
                            valTypeStack.pop();
                        }
                        valTypeStack.push(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case STRUCT_NEW_DEFAULT:
                    {
                        int typeIdx = (int) instruction.operand(0);
                        getStructType(typeIdx);
                        valTypeStack.push(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ARRAY_NEW:
                    {
                        int typeIdx = (int) instruction.operand(0);
                        getArrayType(typeIdx);
                        valTypeStack.pop(); // length
                        valTypeStack.pop(); // fill value
                        valTypeStack.push(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ARRAY_NEW_DEFAULT:
                    {
                        int typeIdx = (int) instruction.operand(0);
                        getArrayType(typeIdx);
                        valTypeStack.pop(); // length
                        valTypeStack.push(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ARRAY_NEW_FIXED:
                    {
                        int typeIdx = (int) instruction.operand(0);
                        int count = (int) instruction.operand(1);
                        getArrayType(typeIdx);
                        for (int e = 0; e < count; e++) {
                            valTypeStack.pop();
                        }
                        valTypeStack.push(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ANY_CONVERT_EXTERN:
                    {
                        valTypeStack.pop();
                        valTypeStack.push(ValType.AnyRef);
                        break;
                    }
                case EXTERN_CONVERT_ANY:
                    {
                        valTypeStack.pop();
                        valTypeStack.push(ValType.ExternRef);
                        break;
                    }
                case END:
                    break;
                default:
                    throw new InvalidException(
                            "constant expression required, but non-constant instruction"
                                    + " encountered: "
                                    + instruction);
            }
        }

        if (valTypeStack.size() < 1) {
            throw new InvalidException("type mismatch, no constant expressions found");
        }
        if (valTypeStack.size() != 1) {
            throw new InvalidException(
                    "type mismatch, values remaining on the stack after evaluation");
        } else {
            var exprType = valTypeStack.pop();
            if (exprType != null && !typeMatches(exprType, expectedType)) {
                throw new InvalidException("type mismatch");
            }
        }
    }

    void validateFunctions() {
        for (var i = 0; i < module.codeSection().functionBodyCount(); i++) {
            var body = module.codeSection().getFunctionBody(i);
            var idx = functionImports.size() + i;
            var type = getType(getFunctionType(idx));
            validateFunction(idx, body, type);
        }
    }

    private static int[] typesWithDefaultValue =
            new int[] {
                ValType.ID.F64,
                ValType.ID.F64,
                ValType.ID.F32,
                ValType.ID.I64,
                ValType.ID.I32,
                ValType.ID.V128,
                ValType.ID.RefNull
            };

    private static boolean hasDefaultValue(ValType t) {
        for (var t2 : typesWithDefaultValue) {
            if (t.opcode() == t2) {
                return true;
            }
        }

        return false;
    }

    private void validateValueType(ValType valueType) {
        if (valueType.isReference() && valueType.typeIdx() >= 0) {
            int idx = valueType.typeIdx();
            if (idx >= module.typeSection().subTypeCount()) {
                throw new InvalidException("unknown type " + idx);
            }
        }
    }

    @SuppressWarnings("UnnecessaryCodeBlock")
    void validateFunction(int funcIdx, FunctionBody body, FunctionType functionType) {
        valTypeStack.clear();
        locals.clear();
        localsInitialized.clear();

        functionType
                .params()
                .forEach(
                        t -> {
                            validateValueType(t);
                            locals.add(t);
                            localsInitialized.add(true);
                        });

        body.localTypes()
                .forEach(
                        t -> {
                            validateValueType(t);
                            locals.add(t);
                            localsInitialized.add(hasDefaultValue(t));
                        });

        functionType.returns().forEach(this::validateValueType);

        pushCtrl(null, new ArrayList<>(), functionType.returns());

        for (var i = 0; i < body.instructions().size(); i++) {
            var op = body.instructions().get(i);

            // control flow instructions handling
            switch (op.opcode()) {
                case UNREACHABLE:
                    unreachable();
                    break;
                case TRY_TABLE:
                    {
                        var t1 = getParams(op);
                        var t2 = getReturns(op);
                        popVals(t1);
                        // and now the catches
                        var catches = CatchOpCode.decode(op.operands());

                        for (int idx = 0; idx < catches.size(); idx++) {
                            var currentCatch = catches.get(idx);
                            if (ctrlFrameStack.size() < currentCatch.label()) {
                                throw new InvalidException("something something");
                            }
                            // push_ctrl(catch, [], label_types(ctrls[handler.label]))
                            // using THROW instead of CATCH ... doesn't matter as it's removed right
                            // after
                            pushCtrl(
                                    OpCode.THROW,
                                    List.of(),
                                    labelTypes(getCtrl(currentCatch.label())));
                            switch (currentCatch.opcode()) {
                                case CATCH:
                                    {
                                        var tagType =
                                                module.typeSection()
                                                        .getType(
                                                                getTagType(currentCatch.tag())
                                                                        .typeIdx());
                                        pushVals(tagType.params());
                                        break;
                                    }
                                case CATCH_REF:
                                    {
                                        var tagType =
                                                module.typeSection()
                                                        .getType(
                                                                getTagType(currentCatch.tag())
                                                                        .typeIdx());
                                        pushVals(tagType.params());
                                        pushVal(ValType.ExnRef);
                                        break;
                                    }
                                case CATCH_ALL:
                                    break;
                                case CATCH_ALL_REF:
                                    pushVal(ValType.ExnRef);
                                    break;
                            }
                            popCtrl();
                        }
                        pushCtrl(op.opcode(), t1, t2);
                        break;
                    }
                case THROW:
                    {
                        var tagNumber = (int) op.operand(0);
                        if ((tagImports.size()
                                        + module.tagSection().map(TagSection::tagCount).orElse(0))
                                <= tagNumber) {
                            throw new InvalidException("unknown tag " + tagNumber);
                        }
                        var type = module.typeSection().getType(getTagType(tagNumber).typeIdx());
                        popVals(type.params());
                        assert (type.returns().size() == 0);
                        unreachable();
                        break;
                    }
                case THROW_REF:
                    {
                        popVal(ValType.ExnRef);
                        unreachable();
                        break;
                    }
                case IF:
                    popVal(ValType.I32);
                // fallthrough
                case LOOP:
                // t1* -> t2*
                // fallthrough
                case BLOCK:
                    {
                        var t1 = getParams(op);
                        var t2 = getReturns(op);
                        t2.forEach(this::validateValueType);
                        popVals(t1);
                        pushCtrl(op.opcode(), t1, t2);
                        break;
                    }
                case END:
                    {
                        var frame = popCtrl();
                        if (frame.opCode == OpCode.IF
                                && !frame.hasElse
                                && frame.startTypes.size() != frame.endTypes.size()) {
                            throw new InvalidException("type mismatch, unbalanced if branches");
                        }
                        pushVals(frame.endTypes);
                        break;
                    }
                case ELSE:
                    {
                        var frame = popCtrl();
                        if (frame.opCode != OpCode.IF) {
                            throw new InvalidException("else doesn't belong to if");
                        }
                        pushCtrl(op.opcode(), frame.startTypes, frame.endTypes);
                        peekCtrl().hasElse = true;
                        break;
                    }
                case BR:
                    {
                        var n = (int) op.operand(0);
                        popVals(labelTypes(getCtrl(n)));
                        unreachable();
                        break;
                    }
                case BR_IF:
                    {
                        popVal(ValType.I32);
                        var n = (int) op.operand(0);
                        var labelTypes = labelTypes(getCtrl(n));
                        popVals(labelTypes);
                        pushVals(labelTypes);
                        break;
                    }
                case BR_TABLE:
                    {
                        popVal(ValType.I32);
                        var m = (int) op.operand(op.operandCount() - 1);
                        if ((ctrlFrameStack.size() - 1 - m) < 0) {
                            throw new InvalidException("unknown label " + m);
                        }
                        var defaultBranchLabelTypes = labelTypes(getCtrl(m));
                        var arity = defaultBranchLabelTypes.size();
                        for (var idx = 0; idx < op.operandCount() - 1; idx++) {
                            var n = (int) op.operand(idx);
                            CtrlFrame ctrlFrame;
                            try {
                                ctrlFrame = getCtrl(n);
                            } catch (IndexOutOfBoundsException e) {
                                throw new InvalidException("unknown label", e);
                            }
                            var labelTypes = labelTypes(ctrlFrame);
                            if (labelTypes.size() != arity) {
                                throw new InvalidException(
                                        "type mismatch, mismatched arity in BR_TABLE for label "
                                                + n);
                            }
                            pushVals(popVals(labelTypes));
                        }
                        popVals(defaultBranchLabelTypes);
                        unreachable();
                        break;
                    }
                case BR_ON_NULL:
                    {
                        var n = (int) op.operand(0);
                        var rt = popRef();
                        var labelTypes = labelTypes(getCtrl(n));
                        popVals(labelTypes);
                        pushVals(labelTypes);
                        pushVal(valType(ValType.ID.Ref, rt.typeIdx()));
                        break;
                    }
                case BR_ON_NON_NULL:
                    {
                        var n = (int) op.operand(0);
                        var rt = popRef();
                        pushVal(valType(ValType.ID.Ref, rt.typeIdx()));
                        var labelTypes = labelTypes(getCtrl(n));
                        popVals(labelTypes);
                        pushVals(labelTypes);
                        popVal();
                        break;
                    }
                case RETURN:
                    VALIDATE_RETURN();
                    break;
                case RETURN_CALL:
                    VALIDATE_CALL((int) op.operand(0), true);
                    VALIDATE_RETURN();
                    break;
                case RETURN_CALL_INDIRECT:
                    VALIDATE_CALL_INDIRECT(op.operand(0), (int) op.operand(1), true);
                    VALIDATE_RETURN();
                    break;
                case RETURN_CALL_REF:
                    VALIDATE_CALL_REF((int) op.operand(0), true);
                    VALIDATE_RETURN();
                    break;
                default:
                    break;
            }

            switch (op.opcode()) {
                case MEMORY_COPY:
                    validateMemory((int) op.operand(0));
                    validateMemory((int) op.operand(1));
                    break;
                case MEMORY_FILL:
                    validateMemory((int) op.operand(0));
                    break;
                case MEMORY_INIT:
                    validateMemory((int) op.operand(1));
                    validateDataSegment((int) op.operand(0));
                    break;
                case MEMORY_SIZE:
                case MEMORY_GROW:
                case I32_LOAD:
                case I32_LOAD8_U:
                case I32_ATOMIC_LOAD8_U:
                case I32_LOAD8_S:
                case I32_LOAD16_U:
                case I32_ATOMIC_LOAD16_U:
                case I32_LOAD16_S:
                case I64_LOAD:
                case I64_LOAD8_S:
                case I64_LOAD8_U:
                case I64_ATOMIC_LOAD8_U:
                case I64_ATOMIC_LOAD16_U:
                case I64_LOAD16_S:
                case I64_LOAD16_U:
                case I64_LOAD32_S:
                case I64_LOAD32_U:
                case I64_ATOMIC_LOAD32_U:
                case F32_LOAD:
                case F64_LOAD:
                case I32_STORE:
                case I32_ATOMIC_STORE:
                case I32_STORE8:
                case I32_ATOMIC_STORE8:
                case I32_STORE16:
                case I32_ATOMIC_STORE16:
                case I32_ATOMIC_RMW_ADD:
                case I32_ATOMIC_RMW_CMPXCHG:
                case I32_ATOMIC_RMW8_CMPXCHG_U:
                case I32_ATOMIC_RMW16_CMPXCHG_U:
                case I64_ATOMIC_RMW_CMPXCHG:
                case I64_ATOMIC_RMW8_CMPXCHG_U:
                case I64_ATOMIC_RMW16_CMPXCHG_U:
                case I64_ATOMIC_RMW32_CMPXCHG_U:
                case I32_ATOMIC_RMW_XCHG:
                case I32_ATOMIC_RMW_OR:
                case I32_ATOMIC_RMW_XOR:
                case I32_ATOMIC_RMW_SUB:
                case I32_ATOMIC_RMW_AND:
                case I32_ATOMIC_RMW8_ADD_U:
                case I32_ATOMIC_RMW8_XCHG_U:
                case I32_ATOMIC_RMW8_OR_U:
                case I32_ATOMIC_RMW8_XOR_U:
                case I32_ATOMIC_RMW8_AND_U:
                case I32_ATOMIC_RMW8_SUB_U:
                case I32_ATOMIC_RMW16_ADD_U:
                case I32_ATOMIC_RMW16_XCHG_U:
                case I32_ATOMIC_RMW16_OR_U:
                case I32_ATOMIC_RMW16_XOR_U:
                case I32_ATOMIC_RMW16_AND_U:
                case I32_ATOMIC_RMW16_SUB_U:
                case I64_STORE:
                case I64_ATOMIC_STORE:
                case I64_STORE8:
                case I64_ATOMIC_STORE8:
                case I64_STORE16:
                case I64_ATOMIC_STORE16:
                case I64_STORE32:
                case I64_ATOMIC_STORE32:
                case F32_STORE:
                case F64_STORE:
                case I64_ATOMIC_RMW_ADD:
                case I64_ATOMIC_RMW_XCHG:
                case I64_ATOMIC_RMW_OR:
                case I64_ATOMIC_RMW_XOR:
                case I64_ATOMIC_RMW_SUB:
                case I64_ATOMIC_RMW_AND:
                case I64_ATOMIC_RMW8_ADD_U:
                case I64_ATOMIC_RMW8_XCHG_U:
                case I64_ATOMIC_RMW8_OR_U:
                case I64_ATOMIC_RMW8_XOR_U:
                case I64_ATOMIC_RMW8_AND_U:
                case I64_ATOMIC_RMW8_SUB_U:
                case I64_ATOMIC_RMW16_ADD_U:
                case I64_ATOMIC_RMW16_XCHG_U:
                case I64_ATOMIC_RMW16_OR_U:
                case I64_ATOMIC_RMW16_XOR_U:
                case I64_ATOMIC_RMW16_AND_U:
                case I64_ATOMIC_RMW16_SUB_U:
                case I64_ATOMIC_RMW32_ADD_U:
                case I64_ATOMIC_RMW32_XCHG_U:
                case I64_ATOMIC_RMW32_OR_U:
                case I64_ATOMIC_RMW32_XOR_U:
                case I64_ATOMIC_RMW32_AND_U:
                case I64_ATOMIC_RMW32_SUB_U:
                case V128_STORE:
                case MEM_ATOMIC_NOTIFY:
                case MEM_ATOMIC_WAIT32:
                case MEM_ATOMIC_WAIT64:
                case I32_ATOMIC_LOAD:
                case I64_ATOMIC_LOAD:
                    validateMemory(0);
                    break;
                default:
                    break;
            }

            switch (op.opcode()) {
                case ATOMIC_FENCE:
                case I32_ATOMIC_STORE8:
                case I64_ATOMIC_STORE8:
                case I32_ATOMIC_LOAD8_U:
                case I64_ATOMIC_LOAD8_U:
                case I32_ATOMIC_RMW8_ADD_U:
                case I32_ATOMIC_RMW8_XCHG_U:
                case I32_ATOMIC_RMW8_OR_U:
                case I32_ATOMIC_RMW8_XOR_U:
                case I32_ATOMIC_RMW8_AND_U:
                case I32_ATOMIC_RMW8_SUB_U:
                case I64_ATOMIC_RMW8_ADD_U:
                case I64_ATOMIC_RMW8_XCHG_U:
                case I64_ATOMIC_RMW8_OR_U:
                case I64_ATOMIC_RMW8_XOR_U:
                case I64_ATOMIC_RMW8_AND_U:
                case I64_ATOMIC_RMW8_SUB_U:
                case I32_ATOMIC_RMW8_CMPXCHG_U:
                case I64_ATOMIC_RMW8_CMPXCHG_U:
                    validateMemAlign(op.operand(0), 0x00);
                    break;
                case I32_ATOMIC_STORE16:
                case I64_ATOMIC_STORE16:
                case I32_ATOMIC_LOAD16_U:
                case I64_ATOMIC_LOAD16_U:
                case I32_ATOMIC_RMW16_ADD_U:
                case I32_ATOMIC_RMW16_XCHG_U:
                case I32_ATOMIC_RMW16_OR_U:
                case I32_ATOMIC_RMW16_XOR_U:
                case I32_ATOMIC_RMW16_AND_U:
                case I32_ATOMIC_RMW16_SUB_U:
                case I64_ATOMIC_RMW16_ADD_U:
                case I64_ATOMIC_RMW16_XCHG_U:
                case I64_ATOMIC_RMW16_OR_U:
                case I64_ATOMIC_RMW16_XOR_U:
                case I64_ATOMIC_RMW16_AND_U:
                case I64_ATOMIC_RMW16_SUB_U:
                case I32_ATOMIC_RMW16_CMPXCHG_U:
                case I64_ATOMIC_RMW16_CMPXCHG_U:
                    validateMemAlign(op.operand(0), 0x01);
                    break;
                case I32_ATOMIC_STORE:
                case I64_ATOMIC_STORE32:
                case I32_ATOMIC_LOAD:
                case I64_ATOMIC_LOAD32_U:
                case I32_ATOMIC_RMW_ADD:
                case I32_ATOMIC_RMW_XCHG:
                case I32_ATOMIC_RMW_OR:
                case I32_ATOMIC_RMW_XOR:
                case I32_ATOMIC_RMW_SUB:
                case I32_ATOMIC_RMW_AND:
                case I64_ATOMIC_RMW32_ADD_U:
                case I64_ATOMIC_RMW32_XCHG_U:
                case I64_ATOMIC_RMW32_OR_U:
                case I64_ATOMIC_RMW32_XOR_U:
                case I64_ATOMIC_RMW32_AND_U:
                case I64_ATOMIC_RMW32_SUB_U:
                case I32_ATOMIC_RMW_CMPXCHG:
                case I64_ATOMIC_RMW32_CMPXCHG_U:
                case MEM_ATOMIC_NOTIFY:
                case MEM_ATOMIC_WAIT32:
                    validateMemAlign(op.operand(0), 0x02);
                    break;
                case I64_ATOMIC_STORE:
                case I64_ATOMIC_LOAD:
                case I64_ATOMIC_RMW_ADD:
                case I64_ATOMIC_RMW_XCHG:
                case I64_ATOMIC_RMW_OR:
                case I64_ATOMIC_RMW_XOR:
                case I64_ATOMIC_RMW_SUB:
                case I64_ATOMIC_RMW_AND:
                case I64_ATOMIC_RMW_CMPXCHG:
                case MEM_ATOMIC_WAIT64:
                    validateMemAlign(op.operand(0), 0x03);
                    break;
            }

            switch (op.opcode()) {
                case V128_LOAD8_LANE:
                case V128_STORE8_LANE:
                    validateLane((int) op.operand(2), 16);
                    break;
                case V128_LOAD16_LANE:
                case V128_STORE16_LANE:
                    validateLane((int) op.operand(2), 8);
                    break;
                case V128_LOAD32_LANE:
                case V128_STORE32_LANE:
                    validateLane((int) op.operand(2), 4);
                    break;
                case V128_LOAD64_LANE:
                case V128_STORE64_LANE:
                    validateLane((int) op.operand(2), 2);
                    break;
                case I8x16_REPLACE_LANE:
                case I8x16_EXTRACT_LANE_S:
                case I8x16_EXTRACT_LANE_U:
                    validateLane((int) op.operand(0), 16);
                    break;
                case I16x8_REPLACE_LANE:
                case I16x8_EXTRACT_LANE_S:
                case I16x8_EXTRACT_LANE_U:
                    validateLane((int) op.operand(0), 8);
                    break;
                case I32x4_REPLACE_LANE:
                case F32x4_REPLACE_LANE:
                case I32x4_EXTRACT_LANE:
                case F32x4_EXTRACT_LANE:
                    validateLane((int) op.operand(0), 4);
                    break;
                case I64x2_REPLACE_LANE:
                case F64x2_REPLACE_LANE:
                case I64x2_EXTRACT_LANE:
                case F64x2_EXTRACT_LANE:
                    validateLane((int) op.operand(0), 2);
                    break;
                case I8x16_SHUFFLE:
                    var operands = Value.vecTo8(new long[] {op.operand(0), op.operand(1)});
                    for (int j = 0; j < 16; j++) {
                        validateLane(operands[j], 32);
                    }
                    break;
                default:
                    break;
            }

            switch (op.opcode()) {
                case NOP:
                case UNREACHABLE:
                case THROW:
                case THROW_REF:
                case TRY_TABLE:
                case LOOP:
                case BLOCK:
                case IF:
                case ELSE:
                case RETURN:
                case RETURN_CALL:
                case RETURN_CALL_INDIRECT:
                case RETURN_CALL_REF:
                case BR_IF:
                case BR_TABLE:
                case BR:
                case BR_ON_NULL:
                case BR_ON_NON_NULL:
                case END:
                case ATOMIC_FENCE:
                    break;
                case DATA_DROP:
                    {
                        validateDataSegment((int) op.operand(0));
                        break;
                    }
                case DROP:
                    {
                        var t = popVal();

                        // setting the type hint
                        if (t.opcode() == ValType.ID.V128) {
                            op.setOperand(0, ValType.ID.V128);
                        }

                        break;
                    }
                case MEM_ATOMIC_NOTIFY:
                    {
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        pushVal(ValType.I32);
                        break;
                    }
                case MEM_ATOMIC_WAIT32:
                    {
                        popVal(ValType.I64);
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        pushVal(ValType.I32);
                        break;
                    }
                case MEM_ATOMIC_WAIT64:
                    {
                        popVal(ValType.I64);
                        popVal(ValType.I64);
                        popVal(ValType.I32);
                        pushVal(ValType.I32);
                        break;
                    }
                case I32_STORE:
                case I32_ATOMIC_STORE:
                case I32_STORE8:
                case I32_ATOMIC_STORE8:
                case I32_STORE16:
                case I32_ATOMIC_STORE16:
                    {
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        break;
                    }
                case I32_LOAD:
                case I32_LOAD8_U:
                case I32_ATOMIC_LOAD8_U:
                case I32_LOAD8_S:
                case I32_LOAD16_U:
                case I32_ATOMIC_LOAD16_U:
                case I32_LOAD16_S:
                case I32_CLZ:
                case I32_CTZ:
                case I32_POPCNT:
                case I32_EXTEND_8_S:
                case I32_EXTEND_16_S:
                case I32_EQZ:
                case MEMORY_GROW:
                case I32_ATOMIC_LOAD:
                    {
                        popVal(ValType.I32);
                        pushVal(ValType.I32);
                        break;
                    }
                case TABLE_SIZE:
                case I32_CONST:
                case MEMORY_SIZE:
                    {
                        pushVal(ValType.I32);
                        break;
                    }
                case I32_ATOMIC_RMW_CMPXCHG:
                case I32_ATOMIC_RMW8_CMPXCHG_U:
                case I32_ATOMIC_RMW16_CMPXCHG_U:
                    {
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        pushVal(ValType.I32);
                        break;
                    }
                case I64_ATOMIC_RMW_CMPXCHG:
                case I64_ATOMIC_RMW8_CMPXCHG_U:
                case I64_ATOMIC_RMW16_CMPXCHG_U:
                case I64_ATOMIC_RMW32_CMPXCHG_U:
                    {
                        popVal(ValType.I64);
                        popVal(ValType.I64);
                        popVal(ValType.I32);
                        pushVal(ValType.I64);
                        break;
                    }
                case I32_ADD:
                case I32_ATOMIC_RMW_ADD:
                case I32_ATOMIC_RMW_XCHG:
                case I32_ATOMIC_RMW_OR:
                case I32_ATOMIC_RMW_XOR:
                case I32_ATOMIC_RMW_SUB:
                case I32_ATOMIC_RMW_AND:
                case I32_SUB:
                case I32_MUL:
                case I32_DIV_S:
                case I32_DIV_U:
                case I32_REM_S:
                case I32_REM_U:
                case I32_AND:
                case I32_OR:
                case I32_XOR:
                case I32_EQ:
                case I32_NE:
                case I32_LT_S:
                case I32_LT_U:
                case I32_LE_S:
                case I32_LE_U:
                case I32_GT_S:
                case I32_GT_U:
                case I32_GE_S:
                case I32_GE_U:
                case I32_SHL:
                case I32_SHR_U:
                case I32_SHR_S:
                case I32_ROTL:
                case I32_ROTR:
                case I32_ATOMIC_RMW8_ADD_U:
                case I32_ATOMIC_RMW8_XCHG_U:
                case I32_ATOMIC_RMW8_OR_U:
                case I32_ATOMIC_RMW8_XOR_U:
                case I32_ATOMIC_RMW8_AND_U:
                case I32_ATOMIC_RMW8_SUB_U:
                case I32_ATOMIC_RMW16_ADD_U:
                case I32_ATOMIC_RMW16_XCHG_U:
                case I32_ATOMIC_RMW16_OR_U:
                case I32_ATOMIC_RMW16_XOR_U:
                case I32_ATOMIC_RMW16_AND_U:
                case I32_ATOMIC_RMW16_SUB_U:
                    {
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        pushVal(ValType.I32);
                        break;
                    }
                case I32_WRAP_I64:
                case I64_EQZ:
                    {
                        popVal(ValType.I64);
                        pushVal(ValType.I32);
                        break;
                    }
                case I32_TRUNC_F32_S:
                case I32_TRUNC_F32_U:
                case I32_TRUNC_SAT_F32_S:
                case I32_TRUNC_SAT_F32_U:
                case I32_REINTERPRET_F32:
                    {
                        popVal(ValType.F32);
                        pushVal(ValType.I32);
                        break;
                    }
                case I32_TRUNC_F64_S:
                case I32_TRUNC_F64_U:
                case I32_TRUNC_SAT_F64_S:
                case I32_TRUNC_SAT_F64_U:
                    {
                        popVal(ValType.F64);
                        pushVal(ValType.I32);
                        break;
                    }
                case I64_LOAD:
                case I64_LOAD8_S:
                case I64_LOAD8_U:
                case I64_ATOMIC_LOAD8_U:
                case I64_LOAD16_S:
                case I64_LOAD16_U:
                case I64_ATOMIC_LOAD16_U:
                case I64_LOAD32_S:
                case I64_LOAD32_U:
                case I64_ATOMIC_LOAD32_U:
                case I64_EXTEND_I32_U:
                case I64_EXTEND_I32_S:
                case I64_ATOMIC_LOAD:
                    {
                        popVal(ValType.I32);
                        pushVal(ValType.I64);
                        break;
                    }
                case I64_ATOMIC_RMW_ADD:
                case I64_ATOMIC_RMW_XCHG:
                case I64_ATOMIC_RMW_OR:
                case I64_ATOMIC_RMW_XOR:
                case I64_ATOMIC_RMW_SUB:
                case I64_ATOMIC_RMW_AND:
                case I64_ATOMIC_RMW8_ADD_U:
                case I64_ATOMIC_RMW8_XCHG_U:
                case I64_ATOMIC_RMW8_OR_U:
                case I64_ATOMIC_RMW8_XOR_U:
                case I64_ATOMIC_RMW8_AND_U:
                case I64_ATOMIC_RMW8_SUB_U:
                case I64_ATOMIC_RMW16_ADD_U:
                case I64_ATOMIC_RMW16_XCHG_U:
                case I64_ATOMIC_RMW16_OR_U:
                case I64_ATOMIC_RMW16_XOR_U:
                case I64_ATOMIC_RMW16_AND_U:
                case I64_ATOMIC_RMW16_SUB_U:
                case I64_ATOMIC_RMW32_ADD_U:
                case I64_ATOMIC_RMW32_XCHG_U:
                case I64_ATOMIC_RMW32_OR_U:
                case I64_ATOMIC_RMW32_XOR_U:
                case I64_ATOMIC_RMW32_AND_U:
                case I64_ATOMIC_RMW32_SUB_U:
                    {
                        popVal(ValType.I64);
                        popVal(ValType.I32);
                        pushVal(ValType.I64);
                        break;
                    }
                case I64_CONST:
                    {
                        pushVal(ValType.I64);
                        break;
                    }
                case I64_STORE:
                case I64_ATOMIC_STORE:
                case I64_STORE8:
                case I64_ATOMIC_STORE8:
                case I64_STORE16:
                case I64_ATOMIC_STORE16:
                case I64_STORE32:
                case I64_ATOMIC_STORE32:
                    {
                        popVal(ValType.I64);
                        popVal(ValType.I32);
                        break;
                    }
                case I64_ADD:
                case I64_SUB:
                case I64_MUL:
                case I64_DIV_S:
                case I64_DIV_U:
                case I64_REM_S:
                case I64_REM_U:
                case I64_AND:
                case I64_OR:
                case I64_XOR:
                case I64_SHL:
                case I64_SHR_U:
                case I64_SHR_S:
                case I64_ROTL:
                case I64_ROTR:
                    {
                        popVal(ValType.I64);
                        popVal(ValType.I64);
                        pushVal(ValType.I64);
                        break;
                    }
                case I64_EQ:
                case I64_NE:
                case I64_LT_S:
                case I64_LT_U:
                case I64_LE_S:
                case I64_LE_U:
                case I64_GT_S:
                case I64_GT_U:
                case I64_GE_S:
                case I64_GE_U:
                    {
                        popVal(ValType.I64);
                        popVal(ValType.I64);
                        pushVal(ValType.I32);
                        break;
                    }
                case I64_CLZ:
                case I64_CTZ:
                case I64_POPCNT:
                case I64_EXTEND_8_S:
                case I64_EXTEND_16_S:
                case I64_EXTEND_32_S:
                    {
                        popVal(ValType.I64);
                        pushVal(ValType.I64);
                        break;
                    }
                case I64_REINTERPRET_F64:
                case I64_TRUNC_F64_S:
                case I64_TRUNC_F64_U:
                case I64_TRUNC_SAT_F64_S:
                case I64_TRUNC_SAT_F64_U:
                    {
                        popVal(ValType.F64);
                        pushVal(ValType.I64);
                        break;
                    }
                case I64_TRUNC_F32_S:
                case I64_TRUNC_F32_U:
                case I64_TRUNC_SAT_F32_S:
                case I64_TRUNC_SAT_F32_U:
                    {
                        popVal(ValType.F32);
                        pushVal(ValType.I64);
                        break;
                    }
                case F32_STORE:
                    {
                        popVal(ValType.F32);
                        popVal(ValType.I32);
                        break;
                    }
                case F32_CONST:
                    {
                        pushVal(ValType.F32);
                        break;
                    }
                case F32_LOAD:
                case F32_CONVERT_I32_S:
                case F32_CONVERT_I32_U:
                case F32_REINTERPRET_I32:
                    {
                        popVal(ValType.I32);
                        pushVal(ValType.F32);
                        break;
                    }
                case F32_CONVERT_I64_S:
                case F32_CONVERT_I64_U:
                    {
                        popVal(ValType.I64);
                        pushVal(ValType.F32);
                        break;
                    }
                case F64_LOAD:
                case F64_CONVERT_I32_S:
                case F64_CONVERT_I32_U:
                    {
                        popVal(ValType.I32);
                        pushVal(ValType.F64);
                        break;
                    }
                case F64_CONVERT_I64_S:
                case F64_CONVERT_I64_U:
                case F64_REINTERPRET_I64:
                    {
                        popVal(ValType.I64);
                        pushVal(ValType.F64);
                        break;
                    }
                case F64_PROMOTE_F32:
                    {
                        popVal(ValType.F32);
                        pushVal(ValType.F64);
                        break;
                    }
                case F32_DEMOTE_F64:
                    {
                        popVal(ValType.F64);
                        pushVal(ValType.F32);
                        break;
                    }
                case F32_SQRT:
                case F32_ABS:
                case F32_NEG:
                case F32_CEIL:
                case F32_FLOOR:
                case F32_TRUNC:
                case F32_NEAREST:
                    {
                        popVal(ValType.F32);
                        pushVal(ValType.F32);
                        break;
                    }
                case F32_ADD:
                case F32_SUB:
                case F32_MUL:
                case F32_DIV:
                case F32_MIN:
                case F32_MAX:
                case F32_COPYSIGN:
                    {
                        popVal(ValType.F32);
                        popVal(ValType.F32);
                        pushVal(ValType.F32);
                        break;
                    }
                case F32_EQ:
                case F32_NE:
                case F32_LT:
                case F32_LE:
                case F32_GT:
                case F32_GE:
                    {
                        popVal(ValType.F32);
                        popVal(ValType.F32);
                        pushVal(ValType.I32);
                        break;
                    }
                case F64_STORE:
                    {
                        popVal(ValType.F64);
                        popVal(ValType.I32);
                        break;
                    }
                case F64_CONST:
                    {
                        pushVal(ValType.F64);
                        break;
                    }
                case F64_SQRT:
                case F64_ABS:
                case F64_NEG:
                case F64_CEIL:
                case F64_FLOOR:
                case F64_TRUNC:
                case F64_NEAREST:
                    {
                        popVal(ValType.F64);
                        pushVal(ValType.F64);
                        break;
                    }
                case F64_ADD:
                case F64_SUB:
                case F64_MUL:
                case F64_DIV:
                case F64_MIN:
                case F64_MAX:
                case F64_COPYSIGN:
                    {
                        popVal(ValType.F64);
                        popVal(ValType.F64);
                        pushVal(ValType.F64);
                        break;
                    }
                case F64_EQ:
                case F64_NE:
                case F64_LT:
                case F64_LE:
                case F64_GT:
                case F64_GE:
                    {
                        popVal(ValType.F64);
                        popVal(ValType.F64);
                        pushVal(ValType.I32);
                        break;
                    }
                case LOCAL_SET:
                    {
                        var index = (int) op.operand(0);
                        popVal(getLocalType(index));
                        setLocal(index);
                        break;
                    }
                case LOCAL_GET:
                    {
                        var index = (int) op.operand(0);
                        getLocal(index);
                        pushVal(getLocalType(index));
                        break;
                    }
                case LOCAL_TEE:
                    {
                        var index = (int) op.operand(0);
                        ValType actualType = popVal();
                        setLocal(index);
                        ValType localType = getLocalType(index);
                        if (!typeMatches(actualType, localType)) {
                            throw new InvalidException(
                                    "type mismatch: local_tee: " + actualType + " " + localType);
                        }
                        pushVal(localType);
                        break;
                    }
                case GLOBAL_GET:
                    {
                        var global = getGlobal((int) op.operand(0));
                        pushVal(global.valueType());
                        break;
                    }
                case GLOBAL_SET:
                    {
                        var global = getGlobal((int) op.operand(0));
                        if (global.mutabilityType() == MutabilityType.Const) {
                            // global.wast in the origin spec and function references
                            // have exact same test that exact two different errors
                            // TOOD: figure out which one
                            throw new InvalidException("global is immutable, immutable global");
                        }
                        popVal(global.valueType());
                        break;
                    }
                case CALL:
                    VALIDATE_CALL((int) op.operand(0), false);
                    break;
                case CALL_INDIRECT:
                    VALIDATE_CALL_INDIRECT(op.operand(0), (int) op.operand(1), false);
                    break;
                case CALL_REF:
                    VALIDATE_CALL_REF((int) op.operand(0), false);
                    break;
                case REF_NULL:
                    {
                        int operand = (int) op.operand(0);
                        ValType type = valType(ValType.ID.RefNull, operand);
                        pushVal(type);
                        break;
                    }
                case REF_IS_NULL:
                    {
                        popRef();
                        pushVal(ValType.I32);
                        break;
                    }
                case REF_AS_NON_NULL:
                    {
                        var rt = popRef();
                        pushVal(valType(ValType.ID.Ref, rt.typeIdx()));
                        break;
                    }
                case REF_FUNC:
                    {
                        var idx = (int) op.operand(0);
                        if (idx == funcIdx // reference to self
                                && !declaredFunctions.contains(idx)) {
                            throw new InvalidException("undeclared function reference");
                        }
                        pushVal(valType(ValType.ID.Ref, getFunctionType(idx)));
                        break;
                    }
                case SELECT:
                    {
                        popVal(ValType.I32);
                        var t1 = popVal();
                        var t2 = popVal();

                        // setting the type hint
                        if ((t1.opcode() == ValType.ID.V128 && t2.opcode() == ValType.ID.V128)
                                || (t1.opcode() == ValType.ID.V128 && t2.opcode() == ValType.ID.BOT)
                                || (t1.opcode() == ValType.ID.BOT
                                        && t2.opcode() == ValType.ID.V128)) {
                            op.setOperand(0, ValType.ID.V128);
                            pushVal(ValType.V128);
                            break;
                        }

                        if (!(isNum(t1) && isNum(t2))) {
                            throw new InvalidException(
                                    "type mismatch: select should have numeric arguments but they"
                                            + " are "
                                            + t1
                                            + " "
                                            + t2);
                        }
                        if (!t1.equals(t2) && !t1.equals(ValType.BOT) && !t2.equals(ValType.BOT)) {
                            throw new InvalidException(
                                    "type mismatch, in SELECT t1: " + t1 + ", t2: " + t2);
                        }
                        if (t1.equals(ValType.BOT)) {
                            pushVal(t2);
                        } else {
                            pushVal(t1);
                        }
                        break;
                    }
                case SELECT_T:
                    {
                        popVal(ValType.I32);
                        if (op.operands().length <= 0 || op.operands().length > 1) {
                            throw new InvalidException("invalid result arity");
                        }
                        var t = valType(op.operand(0));
                        validateValueType(t);
                        popVal(t);
                        popVal(t);
                        pushVal(t);
                        break;
                    }
                case TABLE_COPY:
                    {
                        var table1 = getTableType((int) op.operand(1));
                        var table2 = getTableType((int) op.operand(0));

                        if (!typeMatches(table1, table2)) {
                            throw new InvalidException(
                                    "type mismatch, table 1 type: "
                                            + table1
                                            + ", table 2 type: "
                                            + table2);
                        }

                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        break;
                    }
                case TABLE_INIT:
                    {
                        var table = getTableType((int) op.operand(1));
                        var elemIdx = (int) op.operand(0);
                        var elem = getElement(elemIdx);

                        if (!typeMatches(elem.type(), table)) {
                            throw new InvalidException(
                                    "type mismatch, table type: "
                                            + table
                                            + ", elem type: "
                                            + elem.type());
                        }

                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        break;
                    }
                case MEMORY_COPY:
                case MEMORY_FILL:
                case MEMORY_INIT:
                    {
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        break;
                    }
                case TABLE_FILL:
                    {
                        popVal(ValType.I32);
                        popVal(getTableType((int) op.operand(0)));
                        popVal(ValType.I32);
                        break;
                    }
                case TABLE_GET:
                    {
                        popVal(ValType.I32);
                        pushVal(getTableType((int) op.operand(0)));
                        break;
                    }
                case TABLE_SET:
                    {
                        popVal(getTableType((int) op.operand(0)));
                        popVal(ValType.I32);
                        break;
                    }
                case TABLE_GROW:
                    {
                        popVal(ValType.I32);
                        popVal(getTableType((int) op.operand(0)));
                        pushVal(ValType.I32);
                        break;
                    }
                case ELEM_DROP:
                    {
                        var index = (int) op.operand(0);
                        getElement(index);
                        break;
                    }
                case V128_LOAD:
                case V128_LOAD8x8_S:
                case V128_LOAD8x8_U:
                case V128_LOAD16x4_S:
                case V128_LOAD16x4_U:
                case V128_LOAD32x2_S:
                case V128_LOAD32x2_U:
                case V128_LOAD8_SPLAT:
                case V128_LOAD16_SPLAT:
                case V128_LOAD32_SPLAT:
                case V128_LOAD64_SPLAT:
                case V128_LOAD32_ZERO:
                case V128_LOAD64_ZERO:
                case I8x16_SPLAT:
                case I16x8_SPLAT:
                case I32x4_SPLAT:
                    {
                        popVal(ValType.I32);
                        pushVal(ValType.V128);
                        break;
                    }
                case F32x4_SPLAT:
                    {
                        popVal(ValType.F32);
                        pushVal(ValType.V128);
                        break;
                    }
                case I64x2_SPLAT:
                    {
                        popVal(ValType.I64);
                        pushVal(ValType.V128);
                        break;
                    }
                case F64x2_SPLAT:
                    {
                        popVal(ValType.F64);
                        pushVal(ValType.V128);
                        break;
                    }
                case V128_CONST:
                    {
                        pushVal(ValType.V128);
                        break;
                    }
                case I8x16_REPLACE_LANE:
                case I16x8_REPLACE_LANE:
                case I32x4_REPLACE_LANE:
                case I8x16_SHL:
                case I8x16_SHR_S:
                case I8x16_SHR_U:
                case I16x8_SHL:
                case I16x8_SHR_S:
                case I16x8_SHR_U:
                case I32x4_SHL:
                case I32x4_SHR_S:
                case I32x4_SHR_U:
                case I64x2_SHL:
                case I64x2_SHR_S:
                case I64x2_SHR_U:
                    {
                        popVal(ValType.I32);
                        popVal(ValType.V128);
                        pushVal(ValType.V128);
                        break;
                    }
                case I64x2_REPLACE_LANE:
                    {
                        popVal(ValType.I64);
                        popVal(ValType.V128);
                        pushVal(ValType.V128);
                        break;
                    }
                case F32x4_REPLACE_LANE:
                    {
                        popVal(ValType.F32);
                        popVal(ValType.V128);
                        pushVal(ValType.V128);
                        break;
                    }
                case F64x2_REPLACE_LANE:
                    {
                        popVal(ValType.F64);
                        popVal(ValType.V128);
                        pushVal(ValType.V128);
                        break;
                    }
                case I8x16_ALL_TRUE:
                case I8x16_BITMASK:
                case I16x8_ALL_TRUE:
                case I16x8_BITMASK:
                case I32x4_ALL_TRUE:
                case I32x4_BITMASK:
                case I64x2_ALL_TRUE:
                case I64x2_BITMASK:
                case I8x16_EXTRACT_LANE_S:
                case I8x16_EXTRACT_LANE_U:
                case I16x8_EXTRACT_LANE_S:
                case I16x8_EXTRACT_LANE_U:
                case I32x4_EXTRACT_LANE:
                    {
                        popVal(ValType.V128);
                        pushVal(ValType.I32);
                        break;
                    }
                case F32x4_EXTRACT_LANE:
                    {
                        popVal(ValType.V128);
                        pushVal(ValType.F32);
                        break;
                    }
                case I64x2_EXTRACT_LANE:
                    {
                        popVal(ValType.V128);
                        pushVal(ValType.I64);
                        break;
                    }
                case F64x2_EXTRACT_LANE:
                    {
                        popVal(ValType.V128);
                        pushVal(ValType.F64);
                        break;
                    }
                case I8x16_SHUFFLE:
                case I8x16_SWIZZLE:
                case I8x16_EQ:
                case I8x16_NE:
                case I8x16_LT_S:
                case I8x16_LT_U:
                case I8x16_GT_S:
                case I8x16_GT_U:
                case I8x16_LE_S:
                case I8x16_LE_U:
                case I8x16_GE_S:
                case I8x16_GE_U:
                case I8x16_MIN_S:
                case I8x16_MIN_U:
                case I8x16_MAX_S:
                case I8x16_MAX_U:
                case I8x16_AVGR_U:
                case I8x16_SUB:
                case I8x16_SUB_SAT_S:
                case I8x16_SUB_SAT_U:
                case I8x16_ADD:
                case I8x16_ADD_SAT_S:
                case I8x16_ADD_SAT_U:
                case I8x16_NARROW_I16x8_S:
                case I8x16_NARROW_I16x8_U:
                case I16x8_NE:
                case I16x8_EQ:
                case I16x8_ADD:
                case I16x8_ADD_SAT_S:
                case I16x8_ADD_SAT_U:
                case I16x8_SUB:
                case I16x8_SUB_SAT_S:
                case I16x8_SUB_SAT_U:
                case I16x8_MUL:
                case I16x8_LT_S:
                case I16x8_LT_U:
                case I16x8_GT_S:
                case I16x8_GT_U:
                case I16x8_LE_S:
                case I16x8_LE_U:
                case I16x8_GE_S:
                case I16x8_GE_U:
                case I16x8_MIN_S:
                case I16x8_MIN_U:
                case I16x8_MAX_S:
                case I16x8_MAX_U:
                case I16x8_AVGR_U:
                case I16x8_NARROW_I32x4_S:
                case I16x8_NARROW_I32x4_U:
                case I16x8_Q15MULR_SAT_S:
                case I16x8_EXTMUL_LOW_I8x16_S:
                case I16x8_EXTMUL_HIGH_I8x16_S:
                case I16x8_EXTMUL_LOW_I8x16_U:
                case I16x8_EXTMUL_HIGH_I8x16_U:
                case F32x4_NE:
                case F32x4_LT:
                case F32x4_GT:
                case F32x4_LE:
                case F32x4_GE:
                case F32x4_EQ:
                case F32x4_MUL:
                case F32x4_MIN:
                case F32x4_MAX:
                case F32x4_PMIN:
                case F32x4_PMAX:
                case F32x4_DIV:
                case F32x4_ADD:
                case F32x4_SUB:
                case I32x4_NE:
                case I32x4_EQ:
                case I32x4_ADD:
                case I32x4_SUB:
                case I32x4_MUL:
                case I32x4_MIN_S:
                case I32x4_MIN_U:
                case I32x4_MAX_S:
                case I32x4_MAX_U:
                case I32x4_LT_S:
                case I32x4_LT_U:
                case I32x4_LE_S:
                case I32x4_LE_U:
                case I32x4_GE_S:
                case I32x4_GE_U:
                case I32x4_GT_S:
                case I32x4_GT_U:
                case I32x4_DOT_I16x8_S:
                case I32x4_EXTMUL_LOW_I16x8_S:
                case I32x4_EXTMUL_HIGH_I16x8_S:
                case I32x4_EXTMUL_LOW_I16x8_U:
                case I32x4_EXTMUL_HIGH_I16x8_U:
                case I64x2_NE:
                case I64x2_EQ:
                case I64x2_LT_S:
                case I64x2_LE_S:
                case I64x2_GT_S:
                case I64x2_GE_S:
                case I64x2_ADD:
                case I64x2_SUB:
                case I64x2_MUL:
                case I64x2_EXTMUL_LOW_I32x4_S:
                case I64x2_EXTMUL_HIGH_I32x4_S:
                case I64x2_EXTMUL_LOW_I32x4_U:
                case I64x2_EXTMUL_HIGH_I32x4_U:
                case F64x2_NE:
                case F64x2_LT:
                case F64x2_GT:
                case F64x2_LE:
                case F64x2_GE:
                case F64x2_DIV:
                case F64x2_MAX:
                case F64x2_MIN:
                case F64x2_PMAX:
                case F64x2_PMIN:
                case F64x2_EQ:
                case F64x2_ADD:
                case F64x2_SUB:
                case F64x2_MUL:
                case V128_AND:
                case V128_ANDNOT:
                case V128_OR:
                case V128_XOR:
                    {
                        popVal(ValType.V128);
                        popVal(ValType.V128);
                        pushVal(ValType.V128);
                        break;
                    }
                case I8x16_NEG:
                case I8x16_ABS:
                case I8x16_POPCNT:
                case I16x8_EXTADD_PAIRWISE_I8x16_S:
                case I16x8_EXTADD_PAIRWISE_I8x16_U:
                case I16x8_NEG:
                case I16x8_ABS:
                case I16x8_EXTEND_LOW_I8x16_S:
                case I16x8_EXTEND_HIGH_I8x16_S:
                case I16x8_EXTEND_LOW_I8x16_U:
                case I16x8_EXTEND_HIGH_I8x16_U:
                case I32x4_NEG:
                case I32x4_ABS:
                case I32x4_EXTEND_LOW_I16x8_S:
                case I32x4_EXTEND_HIGH_I16x8_S:
                case I32x4_EXTEND_LOW_I16x8_U:
                case I32x4_EXTEND_HIGH_I16x8_U:
                case I32x4_EXTADD_PAIRWISE_I16x8_S:
                case I32x4_EXTADD_PAIRWISE_I16x8_U:
                case I64x2_EXTEND_LOW_I32x4_S:
                case I64x2_EXTEND_HIGH_I32x4_S:
                case I64x2_EXTEND_LOW_I32x4_U:
                case I64x2_EXTEND_HIGH_I32x4_U:
                case F32x4_NEG:
                case F32x4_ABS:
                case F32x4_SQRT:
                case I32x4_TRUNC_SAT_F32X4_S:
                case I32x4_TRUNC_SAT_F32X4_U:
                case I32x4_TRUNC_SAT_F64x2_S_ZERO:
                case I32x4_TRUNC_SAT_F64x2_U_ZERO:
                case F32x4_CONVERT_I32x4_S:
                case F32x4_CONVERT_I32x4_U:
                case F32x4_CEIL:
                case F32x4_FLOOR:
                case F32x4_TRUNC:
                case F32x4_NEAREST:
                case F64x2_ABS:
                case F64x2_NEG:
                case F64x2_SQRT:
                case F64x2_CEIL:
                case F64x2_FLOOR:
                case F64x2_NEAREST:
                case F64x2_TRUNC:
                case F64x2_CONVERT_LOW_I32x4_S:
                case F64x2_CONVERT_LOW_I32x4_U:
                case F64x2_PROMOTE_LOW_F32x4:
                case F32x4_DEMOTE_LOW_F64x2_ZERO:
                case I64x2_NEG:
                case I64x2_ABS:
                case V128_NOT:
                    {
                        popVal(ValType.V128);
                        pushVal(ValType.V128);
                        break;
                    }
                case V128_BITSELECT:
                    {
                        popVal(ValType.V128);
                        popVal(ValType.V128);
                        popVal(ValType.V128);
                        pushVal(ValType.V128);
                        break;
                    }
                case V128_ANY_TRUE:
                    {
                        popVal(ValType.V128);
                        pushVal(ValType.I32);
                        break;
                    }
                case V128_STORE:
                case V128_STORE8_LANE:
                case V128_STORE16_LANE:
                case V128_STORE32_LANE:
                case V128_STORE64_LANE:
                    {
                        popVal(ValType.V128);
                        popVal(ValType.I32);
                        break;
                    }
                case V128_LOAD8_LANE:
                case V128_LOAD16_LANE:
                case V128_LOAD32_LANE:
                case V128_LOAD64_LANE:
                    {
                        popVal(ValType.V128);
                        popVal(ValType.I32);
                        pushVal(ValType.V128);
                        break;
                    }
                // GC opcodes
                case REF_EQ:
                    {
                        popVal(ValType.EqRef);
                        popVal(ValType.EqRef);
                        pushVal(ValType.I32);
                        break;
                    }
                case STRUCT_NEW:
                    {
                        int typeIdx = (int) op.operand(0);
                        var st = getStructType(typeIdx);
                        for (int fi = st.fieldTypes().length - 1; fi >= 0; fi--) {
                            popVal(unpackFieldType(st.fieldTypes()[fi]));
                        }
                        pushVal(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case STRUCT_NEW_DEFAULT:
                    {
                        int typeIdx = (int) op.operand(0);
                        var st = getStructType(typeIdx);
                        for (var ft : st.fieldTypes()) {
                            var t = unpackFieldType(ft);
                            if (!hasDefaultValue(t)) {
                                throw new InvalidException("field type is not defaultable");
                            }
                        }
                        pushVal(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case STRUCT_GET:
                    {
                        int typeIdx = (int) op.operand(0);
                        int fieldIdx = (int) op.operand(1);
                        var st = getStructType(typeIdx);
                        if (fieldIdx >= st.fieldTypes().length) {
                            throw new InvalidException("unknown field " + fieldIdx);
                        }
                        var ft = st.fieldTypes()[fieldIdx];
                        if (ft.storageType().packedType() != null) {
                            throw new InvalidException("field is packed");
                        }
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        pushVal(unpackFieldType(ft));
                        break;
                    }
                case STRUCT_GET_S:
                case STRUCT_GET_U:
                    {
                        int typeIdx = (int) op.operand(0);
                        int fieldIdx = (int) op.operand(1);
                        var st = getStructType(typeIdx);
                        if (fieldIdx >= st.fieldTypes().length) {
                            throw new InvalidException("unknown field " + fieldIdx);
                        }
                        var ft = st.fieldTypes()[fieldIdx];
                        if (ft.storageType().packedType() == null) {
                            throw new InvalidException("field is unpacked");
                        }
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        pushVal(ValType.I32);
                        break;
                    }
                case STRUCT_SET:
                    {
                        int typeIdx = (int) op.operand(0);
                        int fieldIdx = (int) op.operand(1);
                        var st = getStructType(typeIdx);
                        if (fieldIdx >= st.fieldTypes().length) {
                            throw new InvalidException("unknown field " + fieldIdx);
                        }
                        var ft = st.fieldTypes()[fieldIdx];
                        if (ft.mut() != MutabilityType.Var) {
                            throw new InvalidException("field is immutable");
                        }
                        popVal(unpackFieldType(ft));
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        break;
                    }
                case ARRAY_NEW:
                    {
                        int typeIdx = (int) op.operand(0);
                        var at = getArrayType(typeIdx);
                        popVal(ValType.I32);
                        popVal(unpackFieldType(at.fieldType()));
                        pushVal(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ARRAY_NEW_DEFAULT:
                    {
                        int typeIdx = (int) op.operand(0);
                        var at = getArrayType(typeIdx);
                        var t = unpackFieldType(at.fieldType());
                        if (!hasDefaultValue(t)) {
                            throw new InvalidException("array type is not defaultable");
                        }
                        popVal(ValType.I32);
                        pushVal(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ARRAY_NEW_FIXED:
                    {
                        int typeIdx = (int) op.operand(0);
                        int n = (int) op.operand(1);
                        var at = getArrayType(typeIdx);
                        var elemType = unpackFieldType(at.fieldType());
                        for (int fi = 0; fi < n; fi++) {
                            popVal(elemType);
                        }
                        pushVal(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ARRAY_NEW_DATA:
                    {
                        int typeIdx = (int) op.operand(0);
                        int dataIdx = (int) op.operand(1);
                        var at = getArrayType(typeIdx);
                        validateDataSegment(dataIdx);
                        var t = unpackFieldType(at.fieldType());
                        if (!t.isNumeric() && t.opcode() != ValType.ID.V128) {
                            throw new InvalidException("array type is not numeric or vector");
                        }
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        pushVal(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ARRAY_NEW_ELEM:
                    {
                        int typeIdx = (int) op.operand(0);
                        int elemIdx = (int) op.operand(1);
                        getArrayType(typeIdx);
                        getElement(elemIdx);
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        pushVal(valType(ValType.ID.Ref, typeIdx));
                        break;
                    }
                case ARRAY_GET:
                    {
                        int typeIdx = (int) op.operand(0);
                        var at = getArrayType(typeIdx);
                        if (at.fieldType().storageType().packedType() != null) {
                            throw new InvalidException("array is packed");
                        }
                        popVal(ValType.I32);
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        pushVal(unpackFieldType(at.fieldType()));
                        break;
                    }
                case ARRAY_GET_S:
                case ARRAY_GET_U:
                    {
                        int typeIdx = (int) op.operand(0);
                        var at = getArrayType(typeIdx);
                        if (at.fieldType().storageType().packedType() == null) {
                            throw new InvalidException("array is unpacked");
                        }
                        popVal(ValType.I32);
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        pushVal(ValType.I32);
                        break;
                    }
                case ARRAY_SET:
                    {
                        int typeIdx = (int) op.operand(0);
                        var at = getArrayType(typeIdx);
                        if (at.fieldType().mut() != MutabilityType.Var) {
                            throw new InvalidException("array is immutable");
                        }
                        popVal(unpackFieldType(at.fieldType()));
                        popVal(ValType.I32);
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        break;
                    }
                case ARRAY_LEN:
                    {
                        popVal(ValType.ArrayRef);
                        pushVal(ValType.I32);
                        break;
                    }
                case ARRAY_FILL:
                    {
                        int typeIdx = (int) op.operand(0);
                        var at = getArrayType(typeIdx);
                        if (at.fieldType().mut() != MutabilityType.Var) {
                            throw new InvalidException("array is immutable");
                        }
                        popVal(ValType.I32);
                        popVal(unpackFieldType(at.fieldType()));
                        popVal(ValType.I32);
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        break;
                    }
                case ARRAY_COPY:
                    {
                        int dstIdx = (int) op.operand(0);
                        int srcIdx = (int) op.operand(1);
                        var dstAt = getArrayType(dstIdx);
                        var srcAt = getArrayType(srcIdx);
                        if (dstAt.fieldType().mut() != MutabilityType.Var) {
                            throw new InvalidException("array is immutable");
                        }
                        // Compare storage types directly (not unpacked)
                        // to distinguish packed types like i8 vs i16
                        if (!srcAt.fieldType()
                                .storageType()
                                .equals(dstAt.fieldType().storageType())) {
                            throw new InvalidException("array types do not match");
                        }
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(valType(ValType.ID.RefNull, srcIdx));
                        popVal(ValType.I32);
                        popVal(valType(ValType.ID.RefNull, dstIdx));
                        break;
                    }
                case ARRAY_INIT_DATA:
                    {
                        int typeIdx = (int) op.operand(0);
                        int dataIdx = (int) op.operand(1);
                        var at = getArrayType(typeIdx);
                        if (at.fieldType().mut() != MutabilityType.Var) {
                            throw new InvalidException("array is immutable");
                        }
                        validateDataSegment(dataIdx);
                        var t = unpackFieldType(at.fieldType());
                        if (!t.isNumeric() && t.opcode() != ValType.ID.V128) {
                            throw new InvalidException("array type is not numeric or vector");
                        }
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        break;
                    }
                case ARRAY_INIT_ELEM:
                    {
                        int typeIdx = (int) op.operand(0);
                        int elemIdx = (int) op.operand(1);
                        var at = getArrayType(typeIdx);
                        if (at.fieldType().mut() != MutabilityType.Var) {
                            throw new InvalidException("array is immutable");
                        }
                        var elem = getElement(elemIdx);
                        var arrElem = unpackFieldType(at.fieldType());
                        if (!typeMatches(elem.type(), arrElem)) {
                            throw new InvalidException("type mismatch");
                        }
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(ValType.I32);
                        popVal(valType(ValType.ID.RefNull, typeIdx));
                        break;
                    }
                case REF_I31:
                    {
                        popVal(ValType.I32);
                        pushVal(
                                ValType.builder()
                                        .withOpcode(ValType.ID.Ref)
                                        .withTypeIdx(ValType.TypeIdxCode.I31.code())
                                        .build());
                        break;
                    }
                case I31_GET_S:
                case I31_GET_U:
                    {
                        popVal(ValType.I31Ref);
                        pushVal(ValType.I32);
                        break;
                    }
                case REF_TEST:
                case REF_TEST_NULL:
                    {
                        int heapType = (int) op.operand(0);
                        int topHt = topOfHeapType(heapType);
                        popVal(valType(ValType.ID.RefNull, topHt));
                        pushVal(ValType.I32);
                        break;
                    }
                case CAST_TEST:
                case CAST_TEST_NULL:
                    {
                        int heapType = (int) op.operand(0);
                        boolean nullable = op.opcode() == OpCode.CAST_TEST_NULL;
                        int topHt = topOfHeapType(heapType);
                        popVal(valType(ValType.ID.RefNull, topHt));
                        pushVal(valType(nullable ? ValType.ID.RefNull : ValType.ID.Ref, heapType));
                        break;
                    }
                case BR_ON_CAST:
                case BR_ON_CAST_FAIL:
                    {
                        int flags = (int) op.operand(0);
                        int n = (int) op.operand(1);
                        int ht1 = (int) op.operand(2);
                        int ht2 = (int) op.operand(3);
                        boolean null1 = (flags & 1) != 0;
                        boolean null2 = (flags & 2) != 0;
                        var rt1 = valType(null1 ? ValType.ID.RefNull : ValType.ID.Ref, ht1);
                        var rt2 = valType(null2 ? ValType.ID.RefNull : ValType.ID.Ref, ht2);
                        // rt2 <: rt1 is required by the spec
                        if (!typeMatches(rt2, rt1)) {
                            throw new InvalidException("type mismatch");
                        }
                        // diff_reftype: if rt2 is nullable, fallthrough is non-null rt1
                        var diffType =
                                valType(
                                        null2
                                                ? ValType.ID.Ref
                                                : (null1 ? ValType.ID.RefNull : ValType.ID.Ref),
                                        ht1);
                        var labelTypes = labelTypes(getCtrl(n));
                        if (labelTypes.isEmpty()) {
                            throw new InvalidException("type mismatch");
                        }
                        // The label's last type must match the branch type
                        var brType = op.opcode() == OpCode.BR_ON_CAST ? rt2 : diffType;
                        var lastLabel = labelTypes.get(labelTypes.size() - 1);
                        if (!typeMatches(brType, lastLabel)) {
                            throw new InvalidException("type mismatch");
                        }
                        var ts0 = labelTypes.subList(0, labelTypes.size() - 1);
                        popVal(rt1);
                        popVals(ts0);
                        pushVals(ts0);
                        if (op.opcode() == OpCode.BR_ON_CAST) {
                            pushVal(diffType);
                        } else {
                            pushVal(rt2);
                        }
                        break;
                    }
                case ANY_CONVERT_EXTERN:
                    {
                        var rt = popRef();
                        boolean nullable =
                                rt.equals(ValType.BOT)
                                        || rt.equals(ValType.RefBot)
                                        || rt.isNullable();
                        pushVal(
                                valType(
                                        nullable ? ValType.ID.RefNull : ValType.ID.Ref,
                                        ValType.TypeIdxCode.ANY.code()));
                        break;
                    }
                case EXTERN_CONVERT_ANY:
                    {
                        var rt = popRef();
                        boolean nullable =
                                rt.equals(ValType.BOT)
                                        || rt.equals(ValType.RefBot)
                                        || rt.isNullable();
                        pushVal(
                                valType(
                                        nullable ? ValType.ID.RefNull : ValType.ID.Ref,
                                        ValType.TypeIdxCode.EXTERN.code()));
                        break;
                    }
                default:
                    throw new IllegalArgumentException(
                            "Missing type validation opcode handling for " + op.opcode());
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidException(
                    errors.stream().map(Throwable::getMessage).collect(joining(" - ")));
        }

        // to satisfy the check mentioned in the NOTE
        // https://webassembly.github.io/spec/core/binary/modules.html#data-count-section
        if (module.codeSection().isRequiresDataCount() && module.dataCountSection().isEmpty()) {
            throw new MalformedException("data count section required");
        }
    }

    private void validateTailCall(List<ValType> funcReturnType) {
        var expected = labelTypes(ctrlFrameStack.get(0));

        if (funcReturnType.size() != expected.size()) {
            throw new InvalidException("type mismatch: return arity");
        }

        for (int i = 0; i < funcReturnType.size(); i++) {
            if (!typeMatches(funcReturnType.get(i), expected.get(i))) {
                throw new InvalidException(
                        "type mismatch: tail call doesn't match frame type at index " + i);
            }
        }
    }

    private void VALIDATE_CALL(int funcId, boolean isReturn) {
        int typeId = getFunctionType(funcId);
        var types = getType(typeId);
        for (int j = types.params().size() - 1; j >= 0; j--) {
            popVal(types.params().get(j));
        }
        pushVals(types.returns());
        if (isReturn) {
            validateTailCall(types.returns());
        }
    }

    private void VALIDATE_CALL_INDIRECT(long typeId, int tableId, boolean isReturn) {
        popVal(ValType.I32);
        var tableType = getTableType(tableId);
        if (!typeMatches(tableType, ValType.FuncRef)) {
            throw new InvalidException(
                    "type mismatch expected a table of FuncRefs buf found " + tableType);
        }
        var types = getType((int) typeId);
        for (int j = types.params().size() - 1; j >= 0; j--) {
            popVal(types.params().get(j));
        }
        pushVals(types.returns());
        if (isReturn) {
            validateTailCall(types.returns());
        }
    }

    private void VALIDATE_CALL_REF(int typeId, boolean isReturn) {
        var rt = popRef();
        var funcType = getType(typeId);
        popVals(funcType.params());
        pushVals(funcType.returns());

        if (isReturn) {
            validateTailCall(funcType.returns());
        }

        if (rt.typeIdx() != ValType.TypeIdxCode.BOT.code()) {
            int idx = rt.typeIdx();
            if (idx < 0) {
                // error
                throw new InvalidException(
                        "type mismatch: call_ref should be called on a defined"
                                + " reference type, got operand: "
                                + idx);
            }
        }
    }

    private void VALIDATE_RETURN() {
        popVals(labelTypes(ctrlFrameStack.get(0)));
        unreachable();
    }
}
