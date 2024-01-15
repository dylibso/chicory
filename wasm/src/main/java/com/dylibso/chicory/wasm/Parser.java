package com.dylibso.chicory.wasm;

import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.CodeSection;
import com.dylibso.chicory.wasm.types.CustomSection;
import com.dylibso.chicory.wasm.types.DataSection;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.ElemData;
import com.dylibso.chicory.wasm.types.ElemElem;
import com.dylibso.chicory.wasm.types.ElemFunc;
import com.dylibso.chicory.wasm.types.ElemGlobal;
import com.dylibso.chicory.wasm.types.ElemMem;
import com.dylibso.chicory.wasm.types.ElemStart;
import com.dylibso.chicory.wasm.types.ElemTable;
import com.dylibso.chicory.wasm.types.ElemType;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.ElementSection;
import com.dylibso.chicory.wasm.types.ElementType;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExportDesc;
import com.dylibso.chicory.wasm.types.ExportDescType;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionSection;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalSection;
import com.dylibso.chicory.wasm.types.Import;
import com.dylibso.chicory.wasm.types.ImportDesc;
import com.dylibso.chicory.wasm.types.ImportDescType;
import com.dylibso.chicory.wasm.types.ImportSection;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Memory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MemorySection;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;
import com.dylibso.chicory.wasm.types.RefType;
import com.dylibso.chicory.wasm.types.SectionId;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableSection;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Stack;
import java.util.function.Supplier;

/**
 * Parser for Web Assembly binaries.
 */
public final class Parser {

    private static final System.Logger LOGGER = System.getLogger(Parser.class.getName());

    private static final int MAGIC_BYTES = 1836278016; // Magic prefix \0asm

    private final Supplier<InputStream> input;

    private final BitSet includeSections;

    public Parser(InputStream inputStream) {
        this(() -> inputStream, new BitSet());
    }

    public Parser(ByteBuffer buffer) {
        this(() -> new ByteArrayInputStream(buffer.array()), new BitSet());
    }

    public Parser(File file) {
        this(
                () -> {
                    try {
                        return new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException(
                                "File not found at path: " + file.getPath(), e);
                    }
                },
                new BitSet());
    }

    public Parser(Supplier<InputStream> input, BitSet includeSections) {
        this.input = requireNonNull(input, "input");
        this.includeSections = requireNonNull(includeSections, "includeSections");
    }

    private ByteBuffer readByteBuffer() {
        try {
            var buffer = ByteBuffer.wrap(readBytesFromInput());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read wasm bytes.", e);
        }
    }

    private byte[] readBytesFromInput() throws IOException {
        try (var in = input.get()) {
            return in.readAllBytes();
        }
    }

    public Module parseModule() {
        var builder = new ModuleBuilder();
        parse(builder);
        return builder.getModule();
    }

    public void parse(ParserListener listener) {

        requireNonNull(listener, "listener");

        var buffer = readByteBuffer();

        int magicNumber = buffer.getInt();
        if (magicNumber != MAGIC_BYTES) {
            throw new MalformedException(
                    "unexpected token: magic number mismatch, found: "
                            + magicNumber
                            + " expected: "
                            + MAGIC_BYTES);
        }
        int version = buffer.getInt();
        if (version != 1) {
            throw new MalformedException(
                    "unexpected token: unsupported version, found: " + version + " expected: " + 1);
        }

        while (buffer.hasRemaining()) {
            var sectionId = buffer.get();
            //            var sectionId = (int) readVarUInt32(buffer);
            var sectionSize = readVarUInt32(buffer);

            if (shouldParseSection(sectionId)) {
                // Process different section types based on the sectionId
                switch (sectionId) {
                    case SectionId.CUSTOM:
                        {
                            var customSection = parseCustomSection(buffer, sectionId, sectionSize);
                            listener.onSection(customSection);
                            break;
                        }
                    case SectionId.TYPE:
                        {
                            var typeSection = parseTypeSection(buffer, sectionId, sectionSize);
                            listener.onSection(typeSection);
                            break;
                        }
                    case SectionId.IMPORT:
                        {
                            var importSection = parseImportSection(buffer, sectionId, sectionSize);
                            listener.onSection(importSection);
                            break;
                        }
                    case SectionId.FUNCTION:
                        {
                            var funcSection = parseFunctionSection(buffer, sectionId, sectionSize);
                            listener.onSection(funcSection);
                            break;
                        }
                    case SectionId.TABLE:
                        {
                            var tableSection = parseTableSection(buffer, sectionId, sectionSize);
                            listener.onSection(tableSection);
                            break;
                        }
                    case SectionId.MEMORY:
                        {
                            var memorySection = parseMemorySection(buffer, sectionId, sectionSize);
                            listener.onSection(memorySection);
                            break;
                        }
                    case SectionId.GLOBAL:
                        {
                            var globalSection = parseGlobalSection(buffer, sectionId, sectionSize);
                            listener.onSection(globalSection);
                            break;
                        }
                    case SectionId.EXPORT:
                        {
                            var exportSection = parseExportSection(buffer, sectionId, sectionSize);
                            listener.onSection(exportSection);
                            break;
                        }
                    case SectionId.START:
                        {
                            var startSection = parseStartSection(buffer, sectionId, sectionSize);
                            listener.onSection(startSection);
                            break;
                        }
                    case SectionId.ELEMENT:
                        {
                            var elementSection =
                                    parseElementSection(buffer, sectionId, sectionSize);
                            listener.onSection(elementSection);
                            break;
                        }
                    case SectionId.CODE:
                        {
                            var codeSection = parseCodeSection(buffer, sectionId, sectionSize);
                            listener.onSection(codeSection);
                            break;
                        }
                    case SectionId.DATA:
                        {
                            var dataSection = parseDataSection(buffer, sectionId, sectionSize);
                            listener.onSection(dataSection);
                            break;
                        }
                    default:
                        {
                            LOGGER.log(
                                    System.Logger.Level.WARNING,
                                    "Skipping Section with ID due to configuration: " + sectionId);
                            buffer.position((int) (buffer.position() + sectionSize));
                            break;
                        }
                }
            } else {
                System.out.println("Skipping Section with ID due to configuration: " + sectionId);
                buffer.position((int) (buffer.position() + sectionSize));
                continue;
            }
        }
    }

    public void includeSection(int sectionId) {
        includeSections.set(sectionId);
    }

    private boolean shouldParseSection(int sectionId) {
        if (this.includeSections.isEmpty()) {
            return true;
        }
        return this.includeSections.get(sectionId);
    }

    private static CustomSection parseCustomSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var customSection = new CustomSection(sectionId, sectionSize);
        var name = readName(buffer);
        customSection.setName(name);
        var byteLen = name.getBytes().length;
        var bytes = new byte[(int) (sectionSize - byteLen - Encoding.computeLeb128Size(byteLen))];
        buffer.get(bytes);
        customSection.setBytes(bytes);
        return customSection;
    }

    private static TypeSection parseTypeSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var typeCount = readVarUInt32(buffer);
        var types = new FunctionType[(int) typeCount];

        // Parse individual types in the type section
        for (int i = 0; i < typeCount; i++) {
            var form = readVarUInt32(buffer);

            if (form != 0x60) {
                throw new RuntimeException(
                        "We don't support non func types. Form "
                                + String.format("0x%02X", form)
                                + " was given but we expected 0x60");
            }

            // Parse function types (form = 0x60)
            var paramCount = (int) readVarUInt32(buffer);
            var params = new ValueType[paramCount];

            // Parse parameter types
            for (int j = 0; j < paramCount; j++) {
                params[j] = ValueType.byId(readVarUInt32(buffer));
            }

            var returnCount = (int) readVarUInt32(buffer);
            var returns = new ValueType[returnCount];

            // Parse return types
            for (int j = 0; j < returnCount; j++) {
                returns[j] = ValueType.byId(readVarUInt32(buffer));
            }

            types[i] = new FunctionType(params, returns);
        }

        return new TypeSection(sectionId, sectionSize, types);
    }

    private static ImportSection parseImportSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var importCount = readVarUInt32(buffer);
        var imports = new Import[(int) importCount];

        // Parse individual imports in the import section
        for (int i = 0; i < importCount; i++) {
            String moduleName = readName(buffer);
            String fieldName = readName(buffer);
            var descType = ImportDescType.byId(readVarUInt32(buffer));
            switch (descType) {
                case FuncIdx:
                    {
                        var funcDesc = new ImportDesc(descType, (int) readVarUInt32(buffer));
                        imports[i] = new Import(moduleName, fieldName, funcDesc);
                        break;
                    }
                case TableIdx:
                    {
                        var rawTableType = readVarUInt32(buffer);
                        assert rawTableType == 0x70 || rawTableType == 0x6F;
                        var tableType =
                                (rawTableType == 0x70) ? ValueType.FuncRef : ValueType.ExternRef;

                        var limitType = (int) readVarUInt32(buffer);
                        assert limitType == 0x00 || limitType == 0x01;
                        var min = (int) readVarUInt32(buffer);
                        var max = -1;
                        if (limitType > 0) {
                            max = (int) readVarUInt32(buffer);
                        }
                        var limits = new Limits(min, max);

                        ImportDesc tableDesc = new ImportDesc(descType, limits, tableType);
                        imports[i] = new Import(moduleName, fieldName, tableDesc);
                        break;
                    }
                case MemIdx:
                    {
                        var limitType = (int) readVarUInt32(buffer);
                        assert limitType == 0x00 || limitType == 0x01;
                        var min = (int) readVarUInt32(buffer);
                        var max = -1;
                        if (limitType > 0) {
                            max = (int) readVarUInt32(buffer);
                        }
                        var limits = new Limits(min, max);

                        ImportDesc memDesc = new ImportDesc(descType, limits);
                        imports[i] = new Import(moduleName, fieldName, memDesc);
                        break;
                    }
                case GlobalIdx:
                    var globalValType = ValueType.byId(readVarUInt32(buffer));
                    var globalMut = MutabilityType.byId(readVarUInt32(buffer));
                    var globalDesc = new ImportDesc(descType, globalMut, globalValType);
                    imports[i] = new Import(moduleName, fieldName, globalDesc);
                    break;
            }
        }

        return new ImportSection(sectionId, sectionSize, imports);
    }

    private static FunctionSection parseFunctionSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var functionCount = readVarUInt32(buffer);
        var typeIndices = new int[(int) functionCount];

        // Parse individual functions in the function section
        for (int i = 0; i < functionCount; i++) {
            var typeIndex = readVarUInt32(buffer);
            typeIndices[i] = (int) typeIndex;
        }

        return new FunctionSection(sectionId, sectionSize, typeIndices);
    }

    private static TableSection parseTableSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var tableCount = readVarUInt32(buffer);
        var tables = new Table[(int) tableCount];

        // Parse individual functions in the function section
        for (int i = 0; i < tableCount; i++) {
            var tableType = ElementType.byId(readVarUInt32(buffer));
            var limitType = readVarUInt32(buffer);
            assert limitType == 0x00 || limitType == 0x01;
            var min = readVarUInt32(buffer);
            Long max = null;
            if (limitType == 0x01) {
                max = readVarUInt32(buffer);
            }
            tables[i] = new Table(tableType, min, max);
        }

        return new TableSection(sectionId, sectionSize, tables);
    }

    private static MemorySection parseMemorySection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var memoryCount = readVarUInt32(buffer);
        var memories = new Memory[(int) memoryCount];

        // Parse individual functions in the function section
        for (int i = 0; i < memoryCount; i++) {
            var limits = parseMemoryLimits(buffer);
            memories[i] = new Memory(limits);
        }

        return new MemorySection(sectionId, sectionSize, memories);
    }

    private static MemoryLimits parseMemoryLimits(ByteBuffer buffer) {

        var limitType = readVarUInt32(buffer);
        assert limitType == 0x00 || limitType == 0x01;

        var initial = (int) readVarUInt32(buffer);
        if (limitType != 0x01) {
            return new MemoryLimits(initial);
        }

        int maximum = (int) readVarUInt32(buffer);
        return new MemoryLimits(initial, maximum);
    }

    private static GlobalSection parseGlobalSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var globalCount = readVarUInt32(buffer);
        var globals = new Global[(int) globalCount];

        // Parse individual globals
        for (int i = 0; i < globalCount; i++) {
            var valueType = ValueType.byId(readVarUInt32(buffer));
            var mutabilityType = MutabilityType.byId(readVarUInt32(buffer));
            var init = parseExpression(buffer);
            globals[i] = new Global(valueType, mutabilityType, init);
        }

        return new GlobalSection(sectionId, sectionSize, globals);
    }

    private static ExportSection parseExportSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var exportCount = readVarUInt32(buffer);
        var exports = new Export[(int) exportCount];

        // Parse individual functions in the function section
        for (int i = 0; i < exportCount; i++) {
            var name = readName(buffer);
            var exportType = ExportDescType.byId(readVarUInt32(buffer));
            var index = readVarUInt32(buffer);
            var desc = new ExportDesc(index, exportType);
            exports[i] = new Export(name, desc);
        }

        return new ExportSection(sectionId, sectionSize, exports);
    }

    private static StartSection parseStartSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var startSection = new StartSection(sectionId, sectionSize);
        startSection.setStartIndex(readVarUInt32(buffer));
        return startSection;
    }

    private static ElementSection parseElementSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {
        var initialPosition = buffer.position();

        var elementCount = readVarUInt32(buffer);
        var elements = new Element[(int) elementCount];

        for (var i = 0; i < elementCount; i++) {
            elements[i] = parseSingleElement(buffer);
        }
        assert (buffer.position() == initialPosition + sectionSize);

        return new ElementSection(sectionId, sectionSize, elements);
    }

    private static Element parseSingleElement(ByteBuffer buffer) {
        var kind = readVarUInt32(buffer);
        switch ((int) kind) {
            case 0:
                {
                    var expr = parseExpression(buffer);
                    var funcIndices = readFuncIndices(buffer);
                    return new ElemType(expr, funcIndices);
                }
            case 1:
                {
                    var elemkind = (int) readVarUInt32(buffer);
                    assert (elemkind == 0x00);
                    var funcIndices = readFuncIndices(buffer);
                    return new ElemFunc(funcIndices);
                }
            case 2:
                {
                    var tableIndex = readVarUInt32(buffer);
                    var expr = parseExpression(buffer);
                    var elemkind = (int) readVarUInt32(buffer);
                    assert (elemkind == 0x00);
                    var funcIndices = readFuncIndices(buffer);
                    return new ElemTable(tableIndex, expr, funcIndices);
                }
            case 3:
                {
                    var elemkind = (int) readVarUInt32(buffer);
                    assert (elemkind == 0x00);
                    var funcIndices = readFuncIndices(buffer);
                    return new ElemMem(funcIndices);
                }
            case 4:
                {
                    var expr = parseExpression(buffer);
                    var exprs = readExprs(buffer);
                    return new ElemGlobal(expr, exprs);
                }
            case 5:
                {
                    var refType = RefType.byId(buffer.get());
                    var exprs = readExprs(buffer);
                    return new ElemElem(refType, exprs);
                }
            case 6:
                {
                    var tableIndex = readVarUInt32(buffer);
                    var expr = parseExpression(buffer);
                    var refType = RefType.byId(buffer.get());
                    var exprs = readExprs(buffer);
                    return new ElemData(tableIndex, expr, refType, exprs);
                }
            case 7:
                {
                    var refType = RefType.byId(buffer.get());
                    var exprs = readExprs(buffer);
                    return new ElemStart(refType, exprs);
                }
            default:
                {
                    throw new ChicoryException("Failed to parse the elements section.");
                }
        }
    }

    private static long[] readFuncIndices(ByteBuffer buffer) {
        var funcIndexCount = readVarUInt32(buffer);
        var funcIndices = new long[(int) funcIndexCount];
        for (var j = 0; j < funcIndexCount; j++) {
            funcIndices[j] = readVarUInt32(buffer);
        }
        return funcIndices;
    }

    private static Instruction[][] readExprs(ByteBuffer buffer) {
        var exprIndexCount = readVarUInt32(buffer);
        var exprs = new Instruction[(int) exprIndexCount][];
        for (var j = 0; j < exprIndexCount; j++) {
            var instr = parseExpression(buffer);
            exprs[j] = instr;
        }
        return exprs;
    }

    private static CodeSection parseCodeSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var funcBodyCount = readVarUInt32(buffer);
        var functionBodies = new FunctionBody[(int) funcBodyCount];

        var root = new ControlTree();
        var currentControlFlow = root;

        // Parse individual function bodies in the code section
        for (int i = 0; i < funcBodyCount; i++) {
            var blockScope = new Stack<OpCode>();
            var depth = 0;
            var funcEndPoint = readVarUInt32(buffer) + buffer.position();
            var localCount = readVarUInt32(buffer);
            var locals = new ArrayList<Value>();
            for (int j = 0; j < localCount; j++) {
                var bytes = readVarUInt32(buffer);
                var type = ValueType.byId(readVarUInt32(buffer));
                locals.add(new Value(type, bytes));
            }
            var instructionCount = 0;
            var instructions = new ArrayList<Instruction>();
            currentControlFlow = root;
            do {
                var instruction = parseInstruction(buffer);
                // depth control
                switch (instruction.getOpcode()) {
                    case BLOCK:
                    case LOOP:
                    case IF:
                        {
                            instruction.setDepth(++depth);
                            blockScope.push(instruction.getOpcode());
                            instruction.setScope(blockScope.peek());
                            break;
                        }
                    case END:
                        {
                            instruction.setDepth(depth);
                            depth--;
                            if (blockScope.isEmpty()) {
                                instruction.setScope(OpCode.END);
                            } else {
                                instruction.setScope(blockScope.pop());
                            }
                            break;
                        }
                    default:
                        {
                            instruction.setDepth(depth);
                            break;
                        }
                }

                // control-flow
                switch (instruction.getOpcode()) {
                    case BLOCK:
                    case LOOP:
                        {
                            currentControlFlow =
                                    currentControlFlow.spawn(instructionCount, instruction);
                            break;
                        }
                    case IF:
                        {
                            currentControlFlow =
                                    currentControlFlow.spawn(instructionCount, instruction);

                            var defaultJmp = instructionCount + 1;
                            currentControlFlow.addCallback(
                                    end -> {
                                        // check that there is no "else" branch
                                        if (instruction.getLabelFalse() == defaultJmp) {
                                            instruction.setLabelFalse(end);
                                        }
                                    });

                            // defaults
                            instruction.setLabelTrue(defaultJmp);
                            instruction.setLabelFalse(defaultJmp);
                            break;
                        }
                    case ELSE:
                        {
                            assert (currentControlFlow.getInstruction().getOpcode() == OpCode.IF);
                            currentControlFlow.getInstruction().setLabelFalse(instructionCount + 1);

                            currentControlFlow.addCallback(instruction::setLabelTrue);

                            break;
                        }
                    case BR_IF:
                        {
                            instruction.setLabelFalse(instructionCount + 1);
                        }
                    case BR:
                        {
                            var offset = (int) instruction.getOperands()[0];
                            ControlTree reference = currentControlFlow;
                            while (offset > 0) {
                                reference = reference.getParent();
                                offset--;
                            }
                            reference.addCallback(instruction::setLabelTrue);
                            break;
                        }
                    case BR_TABLE:
                        {
                            instruction.setLabelTable(new int[instruction.getOperands().length]);
                            for (var idx = 0; idx < instruction.getLabelTable().length; idx++) {
                                var offset = (int) instruction.getOperands()[idx];
                                ControlTree reference = currentControlFlow;
                                while (offset > 0) {
                                    reference = reference.getParent();
                                    offset--;
                                }
                                int finalIdx = idx;
                                reference.addCallback(
                                        end -> instruction.getLabelTable()[finalIdx] = end);
                            }
                            break;
                        }
                    case END:
                        {
                            currentControlFlow.setFinalInstructionNumber(
                                    instructionCount, instruction);
                            currentControlFlow = currentControlFlow.getParent();
                            break;
                        }
                }

                instructionCount++;
                instructions.add(instruction);

                // System.out.println(Integer.toHexString(instruction.getAddress()) + " " +
                // instruction);
            } while (buffer.position() < funcEndPoint);

            functionBodies[i] = new FunctionBody(locals, instructions);
        }

        return new CodeSection(sectionId, sectionSize, functionBodies);
    }

    private static DataSection parseDataSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {

        var dataSegmentCount = readVarUInt32(buffer);
        var dataSegments = new DataSegment[(int) dataSegmentCount];

        for (var i = 0; i < dataSegmentCount; i++) {
            var mode = readVarUInt32(buffer);
            if (mode == 0) {
                var offset = parseExpression(buffer);
                byte[] data = new byte[(int) readVarUInt32(buffer)];
                buffer.get(data);
                dataSegments[i] = new ActiveDataSegment(offset, data);
            } else if (mode == 1) {
                byte[] data = new byte[(int) readVarUInt32(buffer)];
                buffer.get(data);
                dataSegments[i] = new PassiveDataSegment(data);
            } else if (mode == 2) {
                var memoryId = readVarUInt32(buffer);
                var offset = parseExpression(buffer);
                byte[] data = new byte[(int) readVarUInt32(buffer)];
                buffer.get(data);
                dataSegments[i] = new ActiveDataSegment(memoryId, offset, data);
            } else {
                throw new ChicoryException("Failed to parse data segment with data mode: " + mode);
            }
        }

        return new DataSection(sectionId, sectionSize, dataSegments);
    }

    private static Instruction parseInstruction(ByteBuffer buffer) {

        var address = buffer.position();
        var b = (int) buffer.get() & 0xff;
        if (b == 0xfc) { // is multi-byte
            b = (0xfc << 8) | (buffer.get() & 0xff);
        }
        var op = OpCode.byOpCode(b);
        if (op == null) {
            throw new IllegalArgumentException("Can't find opcode for op value " + b);
        }
        // System.out.println("b: " + b + " op: " + op);
        var signature = OpCode.getSignature(op);
        if (signature.length == 0) {
            return new Instruction(address, op, new long[] {});
        }
        var operands = new ArrayList<Long>();
        for (var sig : signature) {
            switch (sig) {
                case VARUINT:
                    operands.add(readVarUInt32(buffer));
                    break;
                case VARSINT32:
                    operands.add(readVarSInt32(buffer));
                    break;
                case VARSINT64:
                    operands.add(readVarSInt64(buffer));
                    break;
                case FLOAT64:
                    operands.add(readFloat64(buffer));
                    break;
                case FLOAT32:
                    operands.add(readFloat32(buffer));
                    break;
                case VEC_VARUINT:
                    {
                        var vcount = (int) readVarUInt32(buffer);
                        for (var j = 0; j < vcount; j++) {
                            operands.add(readVarUInt32(buffer));
                        }
                        break;
                    }
            }
        }
        var operandsArray = new long[operands.size()];
        for (var i = 0; i < operands.size(); i++) operandsArray[i] = operands.get(i);
        return new Instruction(address, op, operandsArray);
    }

    private static Instruction[] parseExpression(ByteBuffer buffer) {

        var expr = new ArrayList<Instruction>();
        while (true) {
            var i = parseInstruction(buffer);
            if (i.getOpcode() == OpCode.END) {
                break;
            }
            expr.add(i);
        }
        return expr.toArray(new Instruction[0]);
    }

    /**
     * Read an unsigned I32 from the buffer. We can't fit an unsigned 32bit int
     * into a java int, so we must use a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#integers">2.2.2. Integers</a> of the WebAssembly Core Specification.
     *
     * @param buffer
     * @return
     */
    public static long readVarUInt32(ByteBuffer buffer) {
        return Encoding.readUnsignedLeb128(buffer);
    }

    /**
     * Read a signed I32 from the buffer. We can't fit an unsigned 32bit int into a java int, so we must use a long to use the same type as unsigned.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#integers">2.2.2. Integers</a> of the WebAssembly Core Specification.
     *
     * @param buffer
     * @return
     */
    public static long readVarSInt32(ByteBuffer buffer) {
        return Encoding.readSigned32Leb128(buffer);
    }

    /**
     * Read a signed I64 from the buffer which fits neatly into a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#integers">2.2.2. Integers</a> of the WebAssembly Core Specification.
     *
     * @param buffer
     * @return
     */
    public static long readVarSInt64(ByteBuffer buffer) {
        return Encoding.readSigned64Leb128(buffer);
    }

    /**
     * Read a F64 from the buffer which fits neatly into a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#floating-point">2.2.3. Floating-Point</a> of the WebAssembly Core Specification.
     *
     * @param buffer
     * @return
     */
    public static long readFloat64(ByteBuffer buffer) {
        return buffer.getLong();
    }

    /**
     * Read a F32 from the buffer which fits neatly into a long.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#floating-point">2.2.3. Floating-Point</a> of the WebAssembly Core Specification.
     *
     * @param buffer
     * @return
     */
    public static long readFloat32(ByteBuffer buffer) {
        return buffer.getInt();
    }

    /**
     * Read a symbol name from the buffer as UTF-8 String.
     * See <a href="https://www.w3.org/TR/wasm-core-1/#names%E2%91%A0">2.2.4. Names</a> of the WebAssembly Core Specification.
     *
     * @param buffer
     * @return
     */
    public static String readName(ByteBuffer buffer) {
        var length = (int) readVarUInt32(buffer);
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
