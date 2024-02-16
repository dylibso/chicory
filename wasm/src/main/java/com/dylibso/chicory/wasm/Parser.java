package com.dylibso.chicory.wasm;

import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import com.dylibso.chicory.wasm.io.WasmParseException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.ActiveElement;
import com.dylibso.chicory.wasm.types.CodeSection;
import com.dylibso.chicory.wasm.types.CustomSection;
import com.dylibso.chicory.wasm.types.DataSection;
import com.dylibso.chicory.wasm.types.DeclarativeElement;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.ElementSection;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.FunctionSection;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalImport;
import com.dylibso.chicory.wasm.types.GlobalSection;
import com.dylibso.chicory.wasm.types.ImportSection;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Memory;
import com.dylibso.chicory.wasm.types.MemoryImport;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MemorySection;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.NameCustomSection;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;
import com.dylibso.chicory.wasm.types.PassiveElement;
import com.dylibso.chicory.wasm.types.Section;
import com.dylibso.chicory.wasm.types.SectionId;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableImport;
import com.dylibso.chicory.wasm.types.TableSection;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.UnknownCustomSection;
import com.dylibso.chicory.wasm.types.UnknownSection;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Parser for Web Assembly binaries.
 */
public final class Parser {

    private static final int MAGIC_BYTES = 1836278016; // Magic prefix \0asm

    private final Map<String, Function<WasmInputStream, CustomSection>> customParsers;
    private final BitSet includeSections;
    private final Logger logger;

    public Parser(Logger logger) {
        this(logger, new BitSet());
    }

    public Parser(Logger logger, BitSet includeSections) {
        this(logger, includeSections, Map.of("name", NameCustomSection::new));
    }

    public Parser(
            Logger logger,
            BitSet includeSections,
            Map<String, Function<WasmInputStream, CustomSection>> customParsers) {
        this.logger = requireNonNull(logger, "logger");
        this.includeSections = requireNonNull(includeSections, "includeSections");
        this.customParsers = Map.copyOf(customParsers);
    }

    public Module parseModule(InputStream in) {
        Module module = new Module();

        parse(
                WasmInputStream.of(in),
                (s) -> {
                    switch (s.sectionId()) {
                        case SectionId.CUSTOM:
                            module.addCustomSection((CustomSection) s);
                            break;
                        case SectionId.TYPE:
                            module.setTypeSection((TypeSection) s);
                            break;
                        case SectionId.IMPORT:
                            module.setImportSection((ImportSection) s);
                            break;
                        case SectionId.FUNCTION:
                            module.setFunctionSection((FunctionSection) s);
                            break;
                        case SectionId.TABLE:
                            module.setTableSection((TableSection) s);
                            break;
                        case SectionId.MEMORY:
                            module.setMemorySection((MemorySection) s);
                            break;
                        case SectionId.GLOBAL:
                            module.setGlobalSection((GlobalSection) s);
                            break;
                        case SectionId.EXPORT:
                            module.setExportSection((ExportSection) s);
                            break;
                        case SectionId.START:
                            module.setStartSection((StartSection) s);
                            break;
                        case SectionId.ELEMENT:
                            module.setElementSection((ElementSection) s);
                            break;
                        case SectionId.CODE:
                            module.setCodeSection((CodeSection) s);
                            break;
                        case SectionId.DATA:
                            module.setDataSection((DataSection) s);
                            break;
                        default:
                            logger.warnf("Ignoring section with id: %d", s.sectionId());
                            break;
                    }
                });

        return module;
    }

    // package protected to make it visible for testing
    void parse(WasmInputStream in, ParserListener listener) {

        requireNonNull(listener, "listener");

        int magicNumber = in.raw32le();
        if (magicNumber != MAGIC_BYTES) {
            throw new MalformedException(
                    "unexpected token: magic number mismatch, found: "
                            + magicNumber
                            + " expected: "
                            + MAGIC_BYTES);
        }
        int version = in.raw32le();
        if (version != 1) {
            throw new MalformedException(
                    "unexpected token: unsupported version, found: " + version + " expected: " + 1);
        }

        while (in.peekRawByteOpt() != -1) {
            var sectionId = in.rawByte();
            var sectionSize = in.u32Long();

            try (WasmInputStream sectionData = in.slice(sectionSize)) {
                if (shouldParseSection(sectionId)) {
                    // Process different section types based on the sectionId
                    switch (sectionId) {
                        case SectionId.CUSTOM:
                            {
                                var customSection = parseCustomSection(sectionData);
                                listener.onSection(customSection);
                                break;
                            }
                        case SectionId.TYPE:
                            {
                                var typeSection = parseTypeSection(sectionData);
                                listener.onSection(typeSection);
                                break;
                            }
                        case SectionId.IMPORT:
                            {
                                var importSection = parseImportSection(sectionData);
                                listener.onSection(importSection);
                                break;
                            }
                        case SectionId.FUNCTION:
                            {
                                var funcSection = parseFunctionSection(sectionData);
                                listener.onSection(funcSection);
                                break;
                            }
                        case SectionId.TABLE:
                            {
                                var tableSection = parseTableSection(sectionData);
                                listener.onSection(tableSection);
                                break;
                            }
                        case SectionId.MEMORY:
                            {
                                var memorySection = parseMemorySection(sectionData);
                                listener.onSection(memorySection);
                                break;
                            }
                        case SectionId.GLOBAL:
                            {
                                var globalSection = parseGlobalSection(sectionData);
                                listener.onSection(globalSection);
                                break;
                            }
                        case SectionId.EXPORT:
                            {
                                var exportSection = parseExportSection(sectionData);
                                listener.onSection(exportSection);
                                break;
                            }
                        case SectionId.START:
                            {
                                var startSection = parseStartSection(sectionData);
                                listener.onSection(startSection);
                                break;
                            }
                        case SectionId.ELEMENT:
                            {
                                var elementSection = parseElementSection(sectionData);
                                listener.onSection(elementSection);
                                break;
                            }
                        case SectionId.CODE:
                            {
                                var codeSection = parseCodeSection(sectionData);
                                listener.onSection(codeSection);
                                break;
                            }
                        case SectionId.DATA:
                            {
                                var dataSection = parseDataSection(sectionData);
                                listener.onSection(dataSection);
                                break;
                            }
                        default:
                            {
                                // "Skipping Section with ID due to configuration: " + sectionId
                                listener.onSection(new UnknownSection(sectionId));
                                break;
                            }
                    }
                } else {
                    System.out.println(
                            "Skipping Section with ID due to configuration: " + sectionId);
                }
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

    private CustomSection parseCustomSection(WasmInputStream in) {
        var name = in.utf8();
        var parser = customParsers.get(name);
        if (parser == null) {
            return new UnknownCustomSection(name);
        } else {
            return parser.apply(in);
        }
    }

    private static TypeSection parseTypeSection(WasmInputStream in) {

        var typeCount = in.u31();
        TypeSection typeSection = new TypeSection(typeCount);

        // Parse individual types in the type section
        for (int i = 0; i < typeCount; i++) {
            var form = in.u8();

            if (form != 0x60) {
                throw new RuntimeException(
                        "We don't support non func types. Form "
                                + String.format("0x%02X", form)
                                + " was given but we expected 0x60");
            }

            // Parse function types (form = 0x60)
            var paramCount = in.u16();
            var params = new ValueType[paramCount];

            // Parse parameter types
            for (int j = 0; j < paramCount; j++) {
                params[j] = ValueType.forId(in.u8());
            }

            var returnCount = in.u16();
            var returns = new ValueType[returnCount];

            // Parse return types
            for (int j = 0; j < returnCount; j++) {
                returns[j] = ValueType.forId(in.u8());
            }

            typeSection.addFunctionType(FunctionType.of(params, returns));
        }

        return typeSection;
    }

    private static ImportSection parseImportSection(WasmInputStream in) {

        var importCount = in.u31();
        ImportSection importSection = new ImportSection(importCount);

        // Parse individual imports in the import section
        for (int i = 0; i < importCount; i++) {
            String moduleName = in.utf8();
            String importName = in.utf8();
            var descType = ExternalType.byId(in.u8());
            switch (descType) {
                case FUNCTION:
                    {
                        importSection.addImport(
                                new FunctionImport(moduleName, importName, in.u31()));
                        break;
                    }
                case TABLE:
                    {
                        var rawTableType = in.u8();
                        assert rawTableType == 0x70 || rawTableType == 0x6F;
                        var tableType =
                                (rawTableType == 0x70) ? ValueType.FuncRef : ValueType.ExternRef;

                        var limitType = in.u32();
                        if (limitType != 0x00 && limitType != 0x01) {
                            throw new WasmParseException("Invalid limit type");
                        }
                        var min = in.u32Long();
                        var limits =
                                limitType > 0 ? new Limits(min, in.u32Long()) : new Limits(min);

                        importSection.addImport(
                                new TableImport(moduleName, importName, tableType, limits));
                        break;
                    }
                case MEMORY:
                    {
                        MemoryLimits limits = parseMemoryLimits(in);
                        importSection.addImport(new MemoryImport(moduleName, importName, limits));
                        break;
                    }
                case GLOBAL:
                    var globalValType = ValueType.forId(in.u8());
                    var globalMut = MutabilityType.forId(in.u8());
                    importSection.addImport(
                            new GlobalImport(moduleName, importName, globalMut, globalValType));
                    break;
            }
        }

        return importSection;
    }

    private static FunctionSection parseFunctionSection(WasmInputStream in) {

        var functionCount = in.u31();
        FunctionSection functionSection = new FunctionSection(functionCount);

        // Parse individual functions in the function section
        for (int i = 0; i < functionCount; i++) {
            var typeIndex = in.u31();
            functionSection.addFunctionType(typeIndex);
        }

        return functionSection;
    }

    private static TableSection parseTableSection(WasmInputStream in) {

        var tableCount = in.u31();
        TableSection tableSection = new TableSection(tableCount);

        // Parse individual tables in the tables section
        for (int i = 0; i < tableCount; i++) {
            var tableType = ValueType.refTypeForId(in.u8());
            var limitType = in.u32();
            if (limitType != 0x00 && limitType != 0x01) {
                throw new WasmParseException("Invalid limit type");
            }
            var min = in.u32Long();
            var limits = limitType > 0 ? new Limits(min, in.u32Long()) : new Limits(min);
            tableSection.addTable(new Table(tableType, limits));
        }

        return tableSection;
    }

    private static MemorySection parseMemorySection(WasmInputStream in) {

        var memoryCount = in.u31();
        MemorySection memorySection = new MemorySection(memoryCount);

        // Parse individual memories in the memory section
        for (int i = 0; i < memoryCount; i++) {
            var limits = parseMemoryLimits(in);
            memorySection.addMemory(new Memory(limits));
        }

        return memorySection;
    }

    private static MemoryLimits parseMemoryLimits(WasmInputStream in) {

        var limitType = in.u8();
        switch (limitType) {
            case 0x00:
                return new MemoryLimits(in.u31());
            case 0x01:
                return new MemoryLimits(in.u31(), in.u31());
            case 0x03:
                return new MemoryLimits(in.u31(), in.u31() /*, true*/);
            default:
                throw new WasmParseException("Invalid limit type");
        }
    }

    private static GlobalSection parseGlobalSection(WasmInputStream in) {

        var globalCount = in.u31();
        GlobalSection globalSection = new GlobalSection(globalCount);

        // Parse individual globals
        for (int i = 0; i < globalCount; i++) {
            var valueType = ValueType.forId(in.u8());
            var mutabilityType = MutabilityType.forId(in.u8());
            var init = parseExpression(in);
            globalSection.addGlobal(new Global(valueType, mutabilityType, List.of(init)));
        }

        return globalSection;
    }

    private static ExportSection parseExportSection(WasmInputStream in) {

        var exportCount = in.u31();
        ExportSection exportSection = new ExportSection(exportCount);

        // Parse individual functions in the function section
        for (int i = 0; i < exportCount; i++) {
            var name = in.utf8();
            var exportType = ExternalType.byId(in.u8());
            var index = in.u31();
            exportSection.addExport(new Export(name, index, exportType));
        }

        return exportSection;
    }

    private static StartSection parseStartSection(WasmInputStream in) {

        var startSection = new StartSection();
        startSection.setStartIndex(in.u31());
        return startSection;
    }

    private static ElementSection parseElementSection(WasmInputStream in) {
        var elementCount = in.u31();
        ElementSection elementSection = new ElementSection(elementCount);

        for (var i = 0; i < elementCount; i++) {
            elementSection.addElement(parseSingleElement(in));
        }
        assert in.peekRawByteOpt() == -1;

        return elementSection;
    }

    private static Element parseSingleElement(WasmInputStream in) {
        // Elements are actually fairly complex to parse.
        // See https://webassembly.github.io/spec/core/binary/modules.html#element-section

        int flags = in.u32();

        // Active elements have bit 0 clear
        boolean active = (flags & 0b001) == 0;
        // Declarative elements are non-active elements with bit 1 set
        boolean declarative = !active && (flags & 0b010) != 0;
        // Otherwise, it's passive
        boolean passive = !active && !declarative;

        // Now, characteristics for parsing
        // Does the (active) segment have a table index, or is it always 0?
        boolean hasTableIdx = active && (flags & 0b010) != 0;
        // Is the type always funcref, or do we have to read the type?
        boolean alwaysFuncRef = active && !hasTableIdx;
        // Are initializers expressions or function indices?
        boolean exprInit = (flags & 0b100) != 0;
        // Is the type encoded as an elemkind?
        boolean hasElemKind = !exprInit && !alwaysFuncRef;
        // Is the type encoded as a reftype?
        boolean hasRefType = exprInit && !alwaysFuncRef;

        // the table index is assumed to be zero
        int tableIdx = 0;
        List<Instruction> offset = List.of();

        if (active) {
            if (hasTableIdx) {
                tableIdx = in.u31();
            }
            offset = List.of(parseExpression(in));
        }
        // common path
        ValueType type;
        if (alwaysFuncRef) {
            type = ValueType.FuncRef;
        } else if (hasElemKind) {
            int ek = in.rawByte();
            switch (ek) {
                case 0x00:
                    {
                        type = ValueType.FuncRef;
                        break;
                    }
                default:
                    {
                        throw new ChicoryException("Invalid element kind");
                    }
            }
        } else {
            assert hasRefType;
            type = ValueType.refTypeForId(in.u8());
        }
        int initCnt = in.u31();
        List<List<Instruction>> inits = new ArrayList<>(initCnt);
        if (exprInit) {
            // read the expressions directly from the stream
            for (int i = 0; i < initCnt; i++) {
                inits.add(List.of(parseExpression(in)));
            }
        } else {
            // read function references, and compose them as instruction lists
            for (int i = 0; i < initCnt; i++) {
                inits.add(
                        List.of(
                                new Instruction(-1, OpCode.REF_FUNC, new long[] {in.u31()}),
                                new Instruction(-1, OpCode.END, new long[0])));
            }
        }
        if (declarative) {
            return new DeclarativeElement(type, inits);
        } else if (passive) {
            return new PassiveElement(type, inits);
        } else {
            assert active;
            return new ActiveElement(type, inits, tableIdx, offset);
        }
    }

    private static List<ValueType> parseCodeSectionLocalTypes(WasmInputStream in) {
        var distinctTypesCount = in.u31();
        var locals = new ArrayList<ValueType>();

        for (int i = 0; i < distinctTypesCount; i++) {
            var numberOfLocals = in.u31();
            var type = ValueType.forId(in.u8());
            for (int j = 0; j < numberOfLocals; j++) {
                locals.add(type);
            }
        }

        return locals;
    }

    private static CodeSection parseCodeSection(WasmInputStream in) {
        var funcBodyCount = in.u31();

        var root = new ControlTree();
        var codeSection = new CodeSection(funcBodyCount);

        // Parse individual function bodies in the code section
        for (int i = 0; i < funcBodyCount; i++) {
            var blockScope = new ArrayDeque<Instruction>();
            var depth = 0;
            var funcEndPoint = in.u32Long() + in.position();
            var locals = parseCodeSectionLocalTypes(in);
            var instructions = new ArrayList<Instruction>();
            var lastInstruction = false;
            ControlTree currentControlFlow = null;

            do {
                var instruction = parseInstruction(in);
                lastInstruction = in.position() >= funcEndPoint;
                if (instructions.isEmpty()) {
                    currentControlFlow = root.spawn(0, instruction);
                }

                // depth control
                switch (instruction.opcode()) {
                    case BLOCK:
                    case LOOP:
                    case IF:
                        {
                            instruction.setDepth(++depth);
                            blockScope.push(instruction);
                            instruction.setScope(blockScope.peek());
                            break;
                        }
                    case END:
                        {
                            instruction.setDepth(depth--);
                            instruction.setScope(
                                    blockScope.isEmpty() ? instruction : blockScope.pop());
                            break;
                        }
                    default:
                        {
                            instruction.setDepth(depth);
                            break;
                        }
                }

                // control-flow
                switch (instruction.opcode()) {
                    case BLOCK:
                    case LOOP:
                        {
                            currentControlFlow =
                                    currentControlFlow.spawn(instructions.size(), instruction);
                            break;
                        }
                    case IF:
                        {
                            currentControlFlow =
                                    currentControlFlow.spawn(instructions.size(), instruction);

                            var defaultJmp = instructions.size() + 1;
                            currentControlFlow.addCallback(
                                    end -> {
                                        // check that there is no "else" branch
                                        if (instruction.labelFalse() == defaultJmp) {
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
                            assert (currentControlFlow.instruction().opcode() == OpCode.IF);
                            currentControlFlow.instruction().setLabelFalse(instructions.size() + 1);

                            currentControlFlow.addCallback(instruction::setLabelTrue);

                            break;
                        }
                    case BR_IF:
                        {
                            instruction.setLabelFalse(instructions.size() + 1);
                        }
                    case BR:
                        {
                            var offset = (int) instruction.operands()[0];
                            ControlTree reference = currentControlFlow;
                            while (offset > 0) {
                                reference = reference.parent();
                                offset--;
                            }
                            reference.addCallback(instruction::setLabelTrue);
                            break;
                        }
                    case BR_TABLE:
                        {
                            instruction.setLabelTable(new int[instruction.operands().length]);
                            for (var idx = 0; idx < instruction.labelTable().length; idx++) {
                                var offset = (int) instruction.operands()[idx];
                                ControlTree reference = currentControlFlow;
                                while (offset > 0) {
                                    reference = reference.parent();
                                    offset--;
                                }
                                int finalIdx = idx;
                                reference.addCallback(
                                        end -> instruction.labelTable()[finalIdx] = end);
                            }
                            break;
                        }
                    case END:
                        {
                            currentControlFlow.setFinalInstructionNumber(
                                    instructions.size(), instruction);
                            currentControlFlow = currentControlFlow.parent();

                            if (lastInstruction && instructions.size() > 1) {
                                var former = instructions.get(instructions.size() - 1);
                                if (former.opcode() == OpCode.END) {
                                    instruction.setScope(former.scope());
                                }
                            }
                            break;
                        }
                }

                instructions.add(instruction);

                // System.out.println(Integer.toHexString(instruction.getAddress()) + " " +
                // instruction);
            } while (!lastInstruction);

            codeSection.addFunctionBody(new FunctionBody(locals, instructions));
        }

        return codeSection;
    }

    private static DataSection parseDataSection(WasmInputStream in) {

        var dataSegmentCount = in.u31();
        DataSection dataSection = new DataSection(dataSegmentCount);

        for (var i = 0; i < dataSegmentCount; i++) {
            var mode = in.u32();
            if (mode == 0) {
                var offset = parseExpression(in);
                byte[] data = in.byteVec();
                dataSection.addDataSegment(new ActiveDataSegment(List.of(offset), data));
            } else if (mode == 1) {
                byte[] data = in.byteVec();
                dataSection.addDataSegment(new PassiveDataSegment(data));
            } else if (mode == 2) {
                var memoryId = in.u31();
                var offset = parseExpression(in);
                byte[] data = in.byteVec();
                dataSection.addDataSegment(new ActiveDataSegment(memoryId, List.of(offset), data));
            } else {
                throw new ChicoryException("Failed to parse data segment with data mode: " + mode);
            }
        }

        return dataSection;
    }

    private static Instruction parseInstruction(WasmInputStream in) {

        var address = in.position();
        var b = in.rawByte();
        if (b == 0xfc) { // is multi-byte
            b = (0xfc << 8) + in.u8();
        }
        var op = OpCode.byOpCode(b);
        if (op == null) {
            throw new IllegalArgumentException("Can't find opcode for op value " + b);
        }
        // System.out.println("b: " + b + " op: " + op);
        var signature = OpCode.getSignature(op);
        if (signature.length == 0) {
            return new Instruction((int) address, op, new long[] {});
        }
        var operands = new ArrayList<Long>();
        for (var sig : signature) {
            switch (sig) {
                case VARUINT:
                    operands.add(Long.valueOf(in.u32Long()));
                    break;
                case VARSINT32:
                    operands.add(Long.valueOf(in.s32()));
                    break;
                case VARSINT64:
                    operands.add(Long.valueOf(in.s64()));
                    break;
                case FLOAT64:
                    operands.add(Long.valueOf(Double.doubleToRawLongBits(in.f64())));
                    break;
                case FLOAT32:
                    operands.add(Long.valueOf(Float.floatToRawIntBits(in.f32())));
                    break;
                case VEC_VARUINT:
                    {
                        var vcount = in.u31();
                        for (var j = 0; j < vcount; j++) {
                            operands.add(Long.valueOf(in.u32Long()));
                        }
                        break;
                    }
            }
        }
        var operandsArray = new long[operands.size()];
        for (var i = 0; i < operands.size(); i++) operandsArray[i] = operands.get(i);
        return new Instruction((int) address, op, operandsArray);
    }

    private static Instruction[] parseExpression(WasmInputStream in) {

        var expr = new ArrayList<Instruction>();
        while (true) {
            var i = parseInstruction(in);
            if (i.opcode() == OpCode.END) {
                break;
            }
            expr.add(i);
        }
        return expr.toArray(Instruction[]::new);
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
