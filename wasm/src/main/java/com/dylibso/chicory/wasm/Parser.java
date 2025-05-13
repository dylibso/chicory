package com.dylibso.chicory.wasm;

import static com.dylibso.chicory.wasm.Encoding.readByte;
import static com.dylibso.chicory.wasm.Encoding.readBytes;
import static com.dylibso.chicory.wasm.Encoding.readFloat32;
import static com.dylibso.chicory.wasm.Encoding.readFloat64;
import static com.dylibso.chicory.wasm.Encoding.readName;
import static com.dylibso.chicory.wasm.Encoding.readVarSInt32;
import static com.dylibso.chicory.wasm.Encoding.readVarSInt64;
import static com.dylibso.chicory.wasm.Encoding.readVarUInt32;
import static com.dylibso.chicory.wasm.WasmLimits.MAX_FUNCTION_LOCALS;
import static com.dylibso.chicory.wasm.types.Instruction.EMPTY_OPERANDS;
import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.wasm.io.InputStreams;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.ActiveElement;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.CatchOpCode;
import com.dylibso.chicory.wasm.types.CodeSection;
import com.dylibso.chicory.wasm.types.CustomSection;
import com.dylibso.chicory.wasm.types.DataCountSection;
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
import com.dylibso.chicory.wasm.types.Memory;
import com.dylibso.chicory.wasm.types.MemoryImport;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MemorySection;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.NameCustomSection;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.PassiveDataSegment;
import com.dylibso.chicory.wasm.types.PassiveElement;
import com.dylibso.chicory.wasm.types.RawSection;
import com.dylibso.chicory.wasm.types.Section;
import com.dylibso.chicory.wasm.types.SectionId;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableImport;
import com.dylibso.chicory.wasm.types.TableLimits;
import com.dylibso.chicory.wasm.types.TableSection;
import com.dylibso.chicory.wasm.types.TagImport;
import com.dylibso.chicory.wasm.types.TagSection;
import com.dylibso.chicory.wasm.types.TagType;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.UnknownCustomSection;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Parser for Web Assembly binaries.
 */
@SuppressWarnings("UnnecessaryCodeBlock")
public final class Parser {

    static final byte[] MAGIC_BYTES = {0x00, 0x61, 0x73, 0x6D}; // Magic prefix \0asm
    static final byte[] VERSION_BYTES = {0x01, 0x00, 0x00, 0x00}; // Version 1

    private final Map<String, Function<byte[], CustomSection>> customParsers;
    private final BitSet includeSections;

    private static final Map<String, Function<byte[], CustomSection>> DEFAULT_CUSTOM_PARSERS =
            Map.of("name", NameCustomSection::parse);

    private Parser() {
        this(null, DEFAULT_CUSTOM_PARSERS);
    }

    private Parser(
            BitSet includeSections, Map<String, Function<byte[], CustomSection>> customParsers) {
        this.includeSections = includeSections;
        this.customParsers = Map.copyOf(customParsers);
    }

    private static ByteBuffer readByteBuffer(InputStream is) {
        try {
            var buffer = ByteBuffer.wrap(InputStreams.readAllBytes(is));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read wasm bytes.", e);
        }
    }

    private static void onSection(WasmModule.Builder module, Section s) {
        switch (s.sectionId()) {
            case SectionId.CUSTOM:
                var customSection = (CustomSection) s;
                module.addCustomSection(customSection.name(), customSection);
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
            case SectionId.DATA_COUNT:
                module.setDataCountSection((DataCountSection) s);
                break;
            case SectionId.TAG:
                module.setTagSection((TagSection) s);
                break;
            default:
                module.addIgnoredSection(s.sectionId());
                break;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Map<String, Function<byte[], CustomSection>> customParsers;
        private BitSet includeSections;

        private Builder() {}

        /*
         * @param sectionId : the sectionId to be included while parsing, e.g. SectionId.MEMORY
         */
        public Builder includeSectionId(int sectionId) {
            if (includeSections == null) {
                includeSections = new BitSet();
            }
            includeSections.set(sectionId);
            return this;
        }

        public Builder withCustomParsers(
                Map<String, Function<byte[], CustomSection>> customParsers) {
            this.customParsers = customParsers;
            return this;
        }

        public Parser build() {
            if (customParsers == null) {
                customParsers = DEFAULT_CUSTOM_PARSERS;
            }
            return new Parser(includeSections, customParsers);
        }
    }

    public static WasmModule parse(InputStream input) {
        return new Parser().parse(() -> input);
    }

    public static WasmModule parse(byte[] buffer) {
        return new Parser().parse(() -> new ByteArrayInputStream(buffer));
    }

    public static WasmModule parse(File file) {
        return parse(file.toPath());
    }

    public static WasmModule parse(Path path) {
        return new Parser()
                .parse(
                        () -> {
                            try {
                                return Files.newInputStream(path);
                            } catch (IOException e) {
                                throw new IllegalArgumentException(
                                        "Error opening file: " + path, e);
                            }
                        });
    }

    public WasmModule parse(Supplier<InputStream> inputStreamSupplier) {
        WasmModule.Builder moduleBuilder = WasmModule.builder();
        try (InputStream is = inputStreamSupplier.get()) {
            parse(is, (s) -> onSection(moduleBuilder, s));
        } catch (IOException e) {
            throw new ChicoryException(e);
        } catch (MalformedException e) {
            throw new MalformedException(
                    "section size mismatch, unexpected end of section or function, "
                            + e.getMessage(),
                    e);
        }
        return moduleBuilder.build();
    }

    public void parse(InputStream in, ParserListener listener) {
        parse(in, listener, true);
    }

    private void parse(InputStream in, ParserListener listener, boolean decode) {

        requireNonNull(listener, "listener");
        var validator = new SectionsValidator();

        var buffer = readByteBuffer(in);

        byte[] magic = new byte[4];
        readBytes(buffer, magic);
        if (!Arrays.equals(magic, MAGIC_BYTES)) {
            throw new MalformedException(
                    "magic header not detected, found: "
                            + Arrays.toString(magic)
                            + " expected: "
                            + Arrays.toString(MAGIC_BYTES));
        }

        byte[] version = new byte[4];
        readBytes(buffer, version);
        if (!Arrays.equals(version, VERSION_BYTES)) {
            throw new MalformedException(
                    "unknown binary version, found: "
                            + Arrays.toString(version)
                            + " expected: "
                            + Arrays.toString(VERSION_BYTES));
        }

        // check if the custom section has malformed names only the first time that is parsed
        var firstTime = true;

        while (buffer.hasRemaining()) {
            var sectionId = readByte(buffer);
            var sectionSize = readVarUInt32(buffer);

            validator.validateSectionType(sectionId);

            ByteBuffer sectionByteBuffer = buffer.asReadOnlyBuffer();
            sectionByteBuffer.order(buffer.order());

            // move buffer to next section
            var sectionLimit = sectionByteBuffer.position() + (int) sectionSize;
            if (buffer.capacity() < sectionLimit) {
                throw new MalformedException("length out of bounds for section" + sectionId);
            }
            buffer.position(sectionLimit);

            if (shouldParseSection(sectionId)) {
                sectionByteBuffer.limit(sectionLimit);

                if (!decode) {
                    listener.onSection(parseRawSection(sectionByteBuffer, sectionId, sectionSize));
                    continue;
                }

                // Process different section types based on the sectionId
                switch (sectionId) {
                    case SectionId.CUSTOM:
                        {
                            var customSection =
                                    parseCustomSection(sectionByteBuffer, sectionSize, firstTime);
                            firstTime = false;
                            listener.onSection(customSection);
                            break;
                        }
                    case SectionId.TYPE:
                        {
                            var typeSection = parseTypeSection(sectionByteBuffer);
                            listener.onSection(typeSection);
                            break;
                        }
                    case SectionId.IMPORT:
                        {
                            var importSection = parseImportSection(sectionByteBuffer);
                            listener.onSection(importSection);
                            break;
                        }
                    case SectionId.FUNCTION:
                        {
                            var funcSection = parseFunctionSection(sectionByteBuffer);
                            listener.onSection(funcSection);
                            break;
                        }
                    case SectionId.TABLE:
                        {
                            var tableSection = parseTableSection(sectionByteBuffer);
                            listener.onSection(tableSection);
                            break;
                        }
                    case SectionId.MEMORY:
                        {
                            var memorySection = parseMemorySection(sectionByteBuffer);
                            listener.onSection(memorySection);
                            break;
                        }
                    case SectionId.TAG:
                        {
                            var tagSection = parseTagSection(sectionByteBuffer);
                            listener.onSection(tagSection);
                            break;
                        }
                    case SectionId.GLOBAL:
                        {
                            var globalSection = parseGlobalSection(sectionByteBuffer);
                            listener.onSection(globalSection);
                            break;
                        }
                    case SectionId.EXPORT:
                        {
                            var exportSection = parseExportSection(sectionByteBuffer);
                            listener.onSection(exportSection);
                            break;
                        }
                    case SectionId.START:
                        {
                            var startSection = parseStartSection(sectionByteBuffer);
                            listener.onSection(startSection);
                            break;
                        }
                    case SectionId.ELEMENT:
                        {
                            var elementSection =
                                    parseElementSection(sectionByteBuffer, sectionSize);
                            listener.onSection(elementSection);
                            break;
                        }
                    case SectionId.CODE:
                        {
                            var codeSection = parseCodeSection(sectionByteBuffer);
                            listener.onSection(codeSection);
                            break;
                        }
                    case SectionId.DATA:
                        {
                            var dataSection = parseDataSection(sectionByteBuffer);
                            listener.onSection(dataSection);
                            break;
                        }
                    case SectionId.DATA_COUNT:
                        {
                            var dataCountSection = parseDataCountSection(sectionByteBuffer);
                            listener.onSection(dataCountSection);
                            break;
                        }
                    default:
                        {
                            throw new MalformedException(
                                    "section size mismatch, malformed section id " + sectionId);
                        }
                }

                if (sectionByteBuffer.hasRemaining()) {
                    throw new MalformedException("section size mismatch");
                }
            }
        }
    }

    public static void parseWithoutDecoding(byte[] bytes, ParserListener listener) {
        new Parser().parseWithoutDecoding(new ByteArrayInputStream(bytes), listener);
    }

    public void parseWithoutDecoding(InputStream in, ParserListener listener) {
        parse(in, listener, false);
    }

    // https://webassembly.github.io/spec/core/binary/modules.html#binary-module
    private static class SectionsValidator {
        private List<Integer> sectionsOrder = new ArrayList<>();
        private int maxSection = -1;

        SectionsValidator() {
            sectionsOrder.add(SectionId.TYPE);
            sectionsOrder.add(SectionId.IMPORT);
            sectionsOrder.add(SectionId.FUNCTION);
            sectionsOrder.add(SectionId.TABLE);
            sectionsOrder.add(SectionId.MEMORY);
            sectionsOrder.add(SectionId.GLOBAL);
            sectionsOrder.add(SectionId.EXPORT);
            sectionsOrder.add(SectionId.START);
            sectionsOrder.add(SectionId.ELEMENT);
            sectionsOrder.add(SectionId.DATA_COUNT);
            sectionsOrder.add(SectionId.CODE);
            sectionsOrder.add(SectionId.DATA);
        }

        public void validateSectionType(byte sectionId) {
            if (sectionsOrder.contains((int) sectionId)) {
                if (maxSection < 0 || sectionsOrder.indexOf((int) sectionId) > maxSection) {
                    maxSection = sectionsOrder.indexOf((int) sectionId);
                } else {
                    throw new MalformedException("unexpected content after last section");
                }
            }
        }
    }

    private boolean shouldParseSection(int sectionId) {
        return (this.includeSections == null) || (this.includeSections.get(sectionId));
    }

    private CustomSection parseCustomSection(
            ByteBuffer buffer, long sectionSize, boolean checkMalformed) {
        var sectionPos = buffer.position();
        var name = readName(buffer, checkMalformed);
        var size = (sectionSize - (buffer.position() - sectionPos));
        if (size < 0) {
            throw new MalformedException("unexpected end");
        }
        var bytes = new byte[(int) size];
        readBytes(buffer, bytes);
        var parser = customParsers.get(name);
        return parser == null
                ? UnknownCustomSection.builder().withName(name).withBytes(bytes).build()
                : parser.apply(bytes);
    }

    private static RawSection parseRawSection(ByteBuffer buffer, byte sectionId, long sectionSize) {
        var bytes = new byte[Math.toIntExact(sectionSize)];
        readBytes(buffer, bytes);
        return new RawSection(sectionId, bytes);
    }

    private static TypeSection parseTypeSection(ByteBuffer buffer) {

        var typeCount = readVarUInt32(buffer);
        TypeSection.Builder typeSection = TypeSection.builder();

        // Parse individual types in the type section
        for (int i = 0; i < typeCount; i++) {
            var form = readVarUInt32(buffer);
            if (form > Byte.MAX_VALUE) {
                throw new MalformedException("integer representation too long");
            }

            if (form != 0x60) {
                throw new MalformedException(
                        "We don't support non func types. Form "
                                + String.format("0x%02X", form)
                                + " was given but we expected 0x60");
            }

            // Parse function types (form = 0x60)
            var paramCount = (int) readVarUInt32(buffer);
            var params = new ValType[paramCount];

            // Parse parameter types
            for (int j = 0; j < paramCount; j++) {
                params[j] = readValueType(buffer);
            }

            var returnCount = (int) readVarUInt32(buffer);
            var returns = new ValType[returnCount];

            // Parse return types
            for (int j = 0; j < returnCount; j++) {
                returns[j] = readValueType(buffer);
            }

            typeSection.addFunctionType(FunctionType.of(params, returns));
        }

        return typeSection.build();
    }

    private static ImportSection parseImportSection(ByteBuffer buffer) {

        var importCount = readVarUInt32(buffer);
        ImportSection.Builder importSection = ImportSection.builder();

        // Parse individual imports in the import section
        for (int i = 0; i < importCount; i++) {
            String moduleName = readName(buffer);
            String importName = readName(buffer);
            ExternalType descType;
            try {
                descType = ExternalType.byId((int) readVarUInt32(buffer));
            } catch (RuntimeException e) {
                throw new MalformedException("malformed import kind", e);
            }
            switch (descType) {
                case FUNCTION:
                    {
                        if (moduleName.isEmpty() && importName.isEmpty()) {
                            throw new MalformedException("malformed import kind");
                        }
                        importSection.addImport(
                                new FunctionImport(
                                        moduleName, importName, (int) readVarUInt32(buffer)));
                        break;
                    }
                case TABLE:
                    {
                        var rawTableType = readValueType(buffer);

                        var limitType = readByte(buffer);
                        assert limitType == 0x00 || limitType == 0x01;
                        var min = (int) readVarUInt32(buffer);
                        var limits =
                                limitType > 0
                                        ? new TableLimits(min, readVarUInt32(buffer))
                                        : new TableLimits(min);

                        importSection.addImport(
                                new TableImport(moduleName, importName, rawTableType, limits));
                        break;
                    }
                case MEMORY:
                    {
                        var limitType = readByte(buffer);
                        assert limitType == 0x00 || limitType == 0x01;
                        var min = (int) Math.min(MemoryLimits.MAX_PAGES, readVarUInt32(buffer));
                        var limits =
                                limitType > 0
                                        ? new MemoryLimits(
                                                min,
                                                (int)
                                                        Math.min(
                                                                MemoryLimits.MAX_PAGES,
                                                                readVarUInt32(buffer)))
                                        : new MemoryLimits(min, MemoryLimits.MAX_PAGES);

                        importSection.addImport(new MemoryImport(moduleName, importName, limits));
                        break;
                    }
                case GLOBAL:
                    var globalValType = readValueType(buffer);
                    var globalMut = MutabilityType.forId(readByte(buffer));
                    importSection.addImport(
                            new GlobalImport(moduleName, importName, globalMut, globalValType));
                    break;
                case TAG:
                    try {
                        var attribute = readByte(buffer);
                        var tagTypeIdx = (int) readVarUInt32(buffer);
                        importSection.addImport(
                                new TagImport(moduleName, importName, attribute, tagTypeIdx));
                    } catch (MalformedException e) {
                        throw new MalformedException("malformed import kind", e);
                    }
                    break;
                default:
                    throw new MalformedException("malformed import kind");
            }
        }

        return importSection.build();
    }

    private static FunctionSection parseFunctionSection(ByteBuffer buffer) {

        var functionCount = readVarUInt32(buffer);
        FunctionSection.Builder functionSection = FunctionSection.builder();

        // Parse individual functions in the function section
        for (int i = 0; i < functionCount; i++) {
            var typeIndex = readVarUInt32(buffer);
            functionSection.addFunctionType((int) typeIndex);
        }

        return functionSection.build();
    }

    private static TableLimits readTableLimits(ByteBuffer buffer) {
        var limitType = readByte(buffer);
        if (!(limitType == 0x00 || limitType == 0x01)) {
            throw new MalformedException("integer representation too long, integer too large");
        }
        var min = readVarUInt32(buffer);
        var limits =
                limitType > 0 ? new TableLimits(min, readVarUInt32(buffer)) : new TableLimits(min);
        return limits;
    }

    private static TableSection parseTableSection(ByteBuffer buffer) {

        var tableCount = readVarUInt32(buffer);
        TableSection.Builder tableSection = TableSection.builder();

        // Parse individual tables in the tables section
        for (int i = 0; i < tableCount; i++) {
            var firstByte = (int) readVarUInt32(buffer);
            if (firstByte == 0x40) {
                var secondByte = readVarUInt32(buffer);
                assert secondByte == 0x00;
                var tableType = readValueType(buffer);
                var limits = readTableLimits(buffer);
                var init = parseExpression(buffer);
                tableSection.addTable(new Table(tableType, limits, List.of(init)));
            } else {
                var tableType = readValueTypeFromOpCode(buffer, firstByte);
                var limits = readTableLimits(buffer);
                tableSection.addTable(new Table(tableType, limits));
            }
        }

        return tableSection.build();
    }

    private static MemorySection parseMemorySection(ByteBuffer buffer) {

        var memoryCount = readVarUInt32(buffer);
        MemorySection.Builder memorySection = MemorySection.builder();

        // Parse individual memories in the memory section
        for (int i = 0; i < memoryCount; i++) {
            var limits = parseMemoryLimits(buffer);
            memorySection.addMemory(new Memory(limits));
        }

        return memorySection.build();
    }

    private static MemoryLimits parseMemoryLimits(ByteBuffer buffer) {

        var limitType = readByte(buffer);
        if (!(limitType == 0x00 || limitType == 0x01)) {
            throw new MalformedException("integer representation too long, integer too large");
        }

        var initial = (int) readVarUInt32(buffer);
        if (limitType != 0x01) {
            return new MemoryLimits(initial);
        }

        int maximum = (int) readVarUInt32(buffer);
        return new MemoryLimits(initial, maximum);
    }

    private static GlobalSection parseGlobalSection(ByteBuffer buffer) {

        var globalCount = readVarUInt32(buffer);
        GlobalSection.Builder globalSection = GlobalSection.builder();

        // Parse individual globals
        for (int i = 0; i < globalCount; i++) {
            var valueType = readValueType(buffer);
            var mutabilityType = MutabilityType.forId(readByte(buffer));
            var init = parseExpression(buffer);
            globalSection.addGlobal(new Global(valueType, mutabilityType, List.of(init)));
        }

        return globalSection.build();
    }

    private static ExportSection parseExportSection(ByteBuffer buffer) {

        var exportCount = readVarUInt32(buffer);
        ExportSection.Builder exportSection = ExportSection.builder();

        // Parse individual functions in the function section
        for (int i = 0; i < exportCount; i++) {
            var name = readName(buffer, false);
            var exportType = ExternalType.byId((int) readVarUInt32(buffer));
            var index = (int) readVarUInt32(buffer);
            exportSection.addExport(new Export(name, index, exportType));
        }

        return exportSection.build();
    }

    private static StartSection parseStartSection(ByteBuffer buffer) {
        return StartSection.builder().setStartIndex(readVarUInt32(buffer)).build();
    }

    private static ElementSection parseElementSection(ByteBuffer buffer, long sectionSize) {
        var initialPosition = buffer.position();

        var elementCount = readVarUInt32(buffer);
        ElementSection.Builder elementSection = ElementSection.builder();

        for (var i = 0; i < elementCount; i++) {
            elementSection.addElement(parseSingleElement(buffer));
        }
        if (buffer.position() != initialPosition + sectionSize) {
            throw new MalformedException("section size mismatch");
        }

        return elementSection.build();
    }

    private static Element parseSingleElement(ByteBuffer buffer) {
        // Elements are actually fairly complex to parse.
        // See https://webassembly.github.io/spec/core/binary/modules.html#element-section

        int flags = (int) readVarUInt32(buffer);

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
                tableIdx = Math.toIntExact(readVarUInt32(buffer));
            }
            offset = List.of(parseExpression(buffer));
        }
        // common path
        ValType type;
        if (alwaysFuncRef) {
            if (exprInit) {
                type = ValType.FuncRef;
            } else {
                type = new ValType(ValType.ID.Ref, ValType.TypeIdxCode.FUNC.code());
            }
        } else if (hasElemKind) {
            int ek = (int) readVarUInt32(buffer);
            if (ek == 0x00) {
                type = new ValType(ValType.ID.Ref, ValType.TypeIdxCode.FUNC.code());
            } else {
                throw new ChicoryException("Invalid element kind");
            }
        } else {
            assert hasRefType;
            type = readValueType(buffer);
            if (!type.isReference()) {
                throw new MalformedException(
                        "malformed reference type: element section has non-reference type");
            }
        }
        int initCnt = Math.toIntExact(readVarUInt32(buffer));
        List<List<Instruction>> inits = new ArrayList<>(initCnt);
        if (exprInit) {
            // read the expressions directly from the stream
            for (int i = 0; i < initCnt; i++) {
                inits.add(List.of(parseExpression(buffer)));
            }
        } else {
            // read function references, and compose them as instruction lists
            for (int i = 0; i < initCnt; i++) {
                inits.add(
                        List.of(
                                new Instruction(
                                        -1, OpCode.REF_FUNC, new long[] {readVarUInt32(buffer)}),
                                new Instruction(-1, OpCode.END, EMPTY_OPERANDS)));
            }
        }
        if (declarative) {
            return new DeclarativeElement(type, inits);
        }
        if (passive) {
            return new PassiveElement(type, inits);
        }
        assert active;
        return new ActiveElement(type, inits, tableIdx, offset);
    }

    private static List<ValType> parseCodeSectionLocalTypes(ByteBuffer buffer) {
        var distinctTypesCount = readVarUInt32(buffer);
        var locals = new ArrayList<ValType>();

        for (int i = 0; i < distinctTypesCount; i++) {
            var numberOfLocals = readVarUInt32(buffer);
            if (numberOfLocals > MAX_FUNCTION_LOCALS) {
                throw new MalformedException("too many locals");
            }
            var type = readValueType(buffer);
            for (int j = 0; j < numberOfLocals; j++) {
                locals.add(type);
            }
        }

        return locals;
    }

    private static CodeSection parseCodeSection(ByteBuffer buffer) {
        var funcBodyCount = readVarUInt32(buffer);

        var root = new ControlTree();
        var codeSection = CodeSection.builder();

        // Parse individual function bodies in the code section
        for (int i = 0; i < funcBodyCount; i++) {
            var blockScope = new ArrayDeque<Instruction>();
            var depth = 0;
            var funcEndPoint = readVarUInt32(buffer) + buffer.position();
            var locals = parseCodeSectionLocalTypes(buffer);
            var instructions = new ArrayList<AnnotatedInstruction.Builder>();
            var lastInstruction = false;
            ControlTree currentControlFlow = null;

            do {
                var baseInstruction = parseInstruction(buffer);
                var instruction = AnnotatedInstruction.builder().from(baseInstruction);
                lastInstruction = buffer.position() >= funcEndPoint;
                if (instructions.isEmpty()) {
                    currentControlFlow = root.spawn(0, instruction);
                }

                // https://webassembly.github.io/spec/core/binary/modules.html#data-count-section
                switch (baseInstruction.opcode()) {
                    case MEMORY_INIT:
                    case DATA_DROP:
                        codeSection.setRequiresDataCount(true);
                }

                // depth control
                switch (baseInstruction.opcode()) {
                    case BLOCK:
                    case LOOP:
                    case IF:
                    case TRY_TABLE:
                        {
                            depth++;
                            instruction.withDepth(depth);
                            blockScope.push(baseInstruction);
                            instruction.withScope(blockScope.peek());
                            break;
                        }
                    case END:
                        {
                            instruction.withDepth(depth);
                            depth--;
                            instruction.withScope(
                                    blockScope.isEmpty() ? baseInstruction : blockScope.pop());
                            break;
                        }
                    default:
                        {
                            instruction.withDepth(depth);
                            break;
                        }
                }

                // control-flow
                switch (baseInstruction.opcode()) {
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
                                        instruction.updateLabelFalse(end);
                                    });

                            // defaults
                            instruction.withLabelTrue(defaultJmp);
                            instruction.withLabelFalse(defaultJmp);
                            break;
                        }
                    case ELSE:
                        {
                            currentControlFlow
                                    .instruction()
                                    .withLabelFalse(instructions.size() + 1);

                            currentControlFlow.addCallback(instruction::withLabelTrue);

                            break;
                        }
                    case BR_IF:
                    case BR_ON_NULL:
                    case BR_ON_NON_NULL:
                        {
                            instruction.withLabelFalse(instructions.size() + 1);
                        }
                        // fallthrough
                    case BR:
                        {
                            var offset = (int) baseInstruction.operand(0);
                            ControlTree reference = currentControlFlow;
                            while (offset > 0) {
                                if (reference == null) {
                                    throw new InvalidException("unknown label");
                                }
                                reference = reference.parent();
                                offset--;
                            }
                            reference.addCallback(instruction::withLabelTrue);
                            break;
                        }
                    case BR_TABLE:
                        {
                            var length = baseInstruction.operandCount();
                            var labelTable = new ArrayList<Integer>();
                            for (var idx = 0; idx < length; idx++) {
                                labelTable.add(null);
                                var offset = (int) baseInstruction.operand(idx);
                                ControlTree reference = currentControlFlow;
                                while (offset > 0) {
                                    if (reference == null) {
                                        throw new InvalidException("unknown label");
                                    }
                                    reference = reference.parent();
                                    offset--;
                                }
                                int finalIdx = idx;
                                reference.addCallback(end -> labelTable.set(finalIdx, end));
                            }
                            instruction.withLabelTable(labelTable);
                            break;
                        }
                    case TRY_TABLE:
                        {
                            // labels computation
                            var allLabels = CatchOpCode.allLabels(baseInstruction.operands());
                            var labelTable = new ArrayList<Integer>();
                            for (var idx = 0; idx < allLabels.size(); idx++) {
                                labelTable.add(null);
                                var offset = allLabels.get(idx);
                                ControlTree reference = currentControlFlow;
                                while (offset > 0) {
                                    if (reference == null) {
                                        throw new InvalidException("unknown label");
                                    }
                                    reference = reference.parent();
                                    offset--;
                                }
                                int finalIdx = idx;
                                reference.addCallback(end -> labelTable.set(finalIdx, end));
                            }
                            instruction.withLabelTable(labelTable);

                            // block start
                            currentControlFlow =
                                    currentControlFlow.spawn(instructions.size(), instruction);
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
                                    instruction.withScope(former.scope().get());
                                }
                            }
                            break;
                        }
                }
                if (lastInstruction && instruction.opcode() != OpCode.END) {
                    throw new MalformedException("END opcode expected, section size mismatch");
                }

                instructions.add(instruction);
            } while (!lastInstruction);

            // unbalanced END opcodes
            if (depth > 0) {
                throw new MalformedException("unexpected end");
            }

            var functionBody =
                    new FunctionBody(
                            locals,
                            Collections.unmodifiableList(
                                    instructions.stream()
                                            .map(ins -> ins.build())
                                            .collect(Collectors.toList())));
            codeSection.addFunctionBody(functionBody);
        }

        return codeSection.build();
    }

    private static DataSection parseDataSection(ByteBuffer buffer) {

        var dataSegmentCount = readVarUInt32(buffer);
        DataSection.Builder dataSection = DataSection.builder();

        for (var i = 0; i < dataSegmentCount; i++) {
            var mode = readVarUInt32(buffer);
            if (mode == 0) {
                var offset = parseExpression(buffer);
                byte[] data = new byte[(int) readVarUInt32(buffer)];
                readBytes(buffer, data);
                dataSection.addDataSegment(new ActiveDataSegment(0, List.of(offset), data));
            } else if (mode == 1) {
                byte[] data = new byte[(int) readVarUInt32(buffer)];
                readBytes(buffer, data);
                dataSection.addDataSegment(new PassiveDataSegment(data));
            } else if (mode == 2) {
                var memoryId = readVarUInt32(buffer);
                var offset = parseExpression(buffer);
                byte[] data = new byte[(int) readVarUInt32(buffer)];
                readBytes(buffer, data);
                dataSection.addDataSegment(new ActiveDataSegment(memoryId, List.of(offset), data));
            } else {
                throw new ChicoryException("Failed to parse data segment with data mode: " + mode);
            }
        }

        return dataSection.build();
    }

    private static DataCountSection parseDataCountSection(ByteBuffer buffer) {
        var dataCount = readVarUInt32(buffer);
        return DataCountSection.builder().withDataCount((int) dataCount).build();
    }

    private static TagSection parseTagSection(ByteBuffer buffer) {
        var tagsCount = readVarUInt32(buffer);
        var tagSection = TagSection.builder();
        for (int i = 0; i < tagsCount; i++) {
            var attribute = readByte(buffer);
            var typeIdx = (int) readVarUInt32(buffer);
            tagSection.addTagType(new TagType(attribute, typeIdx));
        }
        return tagSection.build();
    }

    private static Instruction parseInstruction(ByteBuffer buffer) {

        var address = buffer.position();
        int b = (int) readByte(buffer) & 0xff;
        if (b >= 0xfc && b < 0xff) { // is multi-byte
            b = (int) ((b << 8) + readVarUInt32(buffer));
        }
        var op = OpCode.byOpCode(b);
        if (op == null) {
            throw new MalformedException("illegal opcode, op value " + String.format("%02X ", b));
        }
        var signature = OpCode.signature(op);

        switch (op) {
            case MEMORY_GROW:
            case MEMORY_SIZE:
                {
                    var zero = readByte(buffer);
                    if (zero != 0x00) {
                        throw new MalformedException("zero byte expected");
                    }
                    break;
                }
            default:
                break;
        }

        if (signature.isEmpty()) {
            return new Instruction(address, op, EMPTY_OPERANDS);
        }

        var operands = new ArrayList<Long>();
        for (var sig : signature) {
            switch (sig) {
                case BYTE:
                    operands.add(Byte.toUnsignedLong(readByte(buffer)));
                    break;
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
                case VEC_CATCH:
                    {
                        var n = readVarUInt32(buffer);
                        operands.add(n);
                        for (var j = 0; j < n; j++) {
                            var catchOp = readByte(buffer);
                            operands.add(0L | catchOp);
                            var catchOpcode = CatchOpCode.byOpCode(catchOp);
                            switch (catchOpcode) {
                                case CATCH:
                                case CATCH_REF:
                                    operands.add(readVarUInt32(buffer)); // tag
                                    // intentional fall-through
                                case CATCH_ALL:
                                case CATCH_ALL_REF:
                                    operands.add(readVarUInt32(buffer)); // label
                                    break;
                            }
                        }
                        break;
                    }
                case V128:
                    {
                        byte[] bytes = new byte[16];
                        for (var j = 0; j < 16; j++) {
                            bytes[j] = readByte(buffer);
                        }
                        for (var val : Value.bytesToVec(bytes)) {
                            operands.add(val);
                        }
                        break;
                    }
                case BLOCK_TYPE:
                    var operand = (int) readVarUInt32(buffer);
                    if (ValType.ID.isValidOpcode(operand)) {
                        // is value type
                        ValType v = readValueTypeFromOpCode(buffer, operand);
                        operands.add(v.id());
                    } else {
                        operands.add((long) operand);
                    }
                    break;
                case VALUE_TYPE:
                    var valueType = readValueType(buffer);
                    operands.add(valueType.id());
                    break;
                case VEC_VALUE_TYPE:
                    var vcount = (int) readVarUInt32(buffer);
                    for (var j = 0; j < vcount; j++) {
                        operands.add(readValueType(buffer).id());
                    }
                    break;
            }
        }
        var operandsArray = new long[operands.size()];
        for (var i = 0; i < operands.size(); i++) {
            operandsArray[i] = operands.get(i);
        }
        verifyAlignment(op, operandsArray);
        return new Instruction(address, op, operandsArray);
    }

    private static void verifyAlignment(OpCode op, long[] operands) {
        var align = -1;
        switch (op) {
            case I32_LOAD8_U:
            case I32_LOAD8_S:
            case I64_LOAD8_U:
            case I64_LOAD8_S:
            case I32_STORE8:
            case I64_STORE8:
            case V128_LOAD8_SPLAT:
            case V128_STORE8_LANE:
            case V128_LOAD8_LANE:
                align = 8;
                break;
            case I32_LOAD16_U:
            case I32_LOAD16_S:
            case I64_LOAD16_U:
            case I64_LOAD16_S:
            case I32_STORE16:
            case I64_STORE16:
            case V128_LOAD16_SPLAT:
            case V128_STORE16_LANE:
            case V128_LOAD16_LANE:
                align = 16;
                break;
            case I32_LOAD:
            case F32_LOAD:
            case I64_LOAD32_U:
            case I64_LOAD32_S:
            case I64_STORE32:
            case I32_STORE:
            case F32_STORE:
            case V128_LOAD32_SPLAT:
            case V128_STORE32_LANE:
            case V128_LOAD32_LANE:
                align = 32;
                break;
            case I64_LOAD:
            case F64_LOAD:
            case I64_STORE:
            case F64_STORE:
            case V128_LOAD8x8_S:
            case V128_LOAD8x8_U:
            case V128_LOAD16x4_S:
            case V128_LOAD16x4_U:
            case V128_LOAD32x2_S:
            case V128_LOAD32x2_U:
            case V128_LOAD64_SPLAT:
            case V128_STORE64_LANE:
            case V128_LOAD64_LANE:
                align = 64;
                break;
            case V128_LOAD:
            case V128_STORE:
                align = 128;
                break;
        }

        if (align > 0) {
            var operand0 = ((int) operands[0]);
            var offset = 1 << operand0;

            if (operand0 >= align) {
                throw new MalformedException("malformed memop flags");
            } else if (offset < 0 || offset > (align >> 3)) {
                throw new InvalidException(
                        "alignment must not be larger than natural alignment (" + operand0 + ")");
            }
        }
    }

    private static ValType readValueTypeFromOpCode(ByteBuffer buffer, int valueTypeOpCode) {
        if (valueTypeOpCode == ValType.ID.Ref || valueTypeOpCode == ValType.ID.RefNull) {
            return new ValType(valueTypeOpCode, (int) readVarSInt32(buffer));
        } else {
            return new ValType(valueTypeOpCode);
        }
    }

    private static ValType readValueType(ByteBuffer buffer) {
        var valueTypeOpCode = (int) readVarUInt32(buffer);

        return readValueTypeFromOpCode(buffer, valueTypeOpCode);
    }

    private static Instruction[] parseExpression(ByteBuffer buffer) {
        var expr = new ArrayList<Instruction>();
        while (buffer.hasRemaining()) {
            var i = parseInstruction(buffer);
            if (i.opcode() == OpCode.END) {
                return expr.toArray(new Instruction[0]);
            }
            expr.add(i);
        }

        throw new MalformedException("illegal opcode: expected end opcode");
    }
}
