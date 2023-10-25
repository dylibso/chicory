package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.control_flow.ControlFlowStateMachine;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import com.dylibso.chicory.wasm.types.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Parser {
    private String filePath;
    private InputStream inputStream;
    private ParserListener listener;
    private List<Integer> includeSections;

    private static int MAGIC_BYTES = 1836278016; // Magic prefix \0asm

    public Parser(String filePath) {
        this.filePath = filePath;
        this.listener = null;
        this.includeSections = null;
    }

    public Parser(InputStream inputStream) {
        this.inputStream = inputStream;
        this.listener = null;
        this.includeSections = null;
    }

    public void setListener(ParserListener listener) {
        this.listener = listener;
    }

    public void includeSection(int sectionId) {
        if (this.includeSections == null) this.includeSections = new ArrayList<>();
        this.includeSections.add(sectionId);
    }

    private ByteBuffer readByteBuffer() {
        try {
            // Read the Wasm file into a ByteBuffer
            var fileInputStream = inputStream;
            if (this.inputStream == null) {
                fileInputStream = new FileInputStream(filePath);
            }
            byte[] buf = new byte[fileInputStream.available()];
            fileInputStream.read(buf);
            fileInputStream.close();

            var buffer = ByteBuffer.wrap(buf);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Failed to read file " + filePath);
        }
    }

    public Module parseModule() {
        if (this.listener != null) {
            throw new IllegalArgumentException(
                    "This method overrides your custom listener. You probably want to use"
                            + " Module#parse() instead");
        }
        var builder = new ModuleBuilder();
        setListener(builder);
        parse();
        return builder.getModule();
    }

    public void parse() {
        if (this.listener == null) {
            throw new IllegalArgumentException(
                    "Missing ParserListener. Use Module#setListener to set a listener;");
        }

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
            var sectionId = (int) readVarUInt32(buffer);
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
                            System.out.println("Skipping Unknown Section with ID: " + sectionId);
                            buffer.position((int) (buffer.position() + sectionSize));
                            break;
                        }
                }
            } else {
                System.out.println("Skipping Section with ID due to configuration: " + sectionId);
                buffer.position((int) (buffer.position() + sectionSize));
            }
        }
    }

    private boolean shouldParseSection(int sectionId) {
        if (this.includeSections == null) return true;
        if (this.includeSections.contains(sectionId)) return true;
        return false;
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
            var descIdx = readVarUInt32(buffer);
            var desc = new ImportDesc(descIdx, descType);
            imports[i] = new Import(moduleName, fieldName, desc);
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
            var limitType = readVarUInt32(buffer);
            assert limitType == 0x00 || limitType == 0x01;
            var initial = (int) readVarUInt32(buffer);
            Integer max = null;
            if (limitType == 0x01) {
                max = (int) readVarUInt32(buffer);
            }
            memories[i] = new Memory(new MemoryLimits(initial, max));
        }

        return new MemorySection(sectionId, sectionSize, memories);
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
        var elementCount = readVarUInt32(buffer);
        var elements = new Element[(int) elementCount];

        for (var i = 0; i < elementCount; i++) {
            var tableIndex = readVarUInt32(buffer);
            var expr = parseExpression(buffer);
            var funcIndexCount = readVarUInt32(buffer);
            var funcIndices = new long[(int) funcIndexCount];
            for (var j = 0; j < funcIndexCount; j++) {
                funcIndices[j] = readVarUInt32(buffer);
            }
            elements[i] = new Element(tableIndex, expr, funcIndices);
        }

        return new ElementSection(sectionId, sectionSize, elements);
    }

    private static CodeSection parseCodeSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {
        var funcBodyCount = readVarUInt32(buffer);
        var functionBodies = new FunctionBody[(int) funcBodyCount];

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
            var instructions = new ArrayList<Instruction>();
            var cfSM = new ControlFlowStateMachine();
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

                // control flow
                cfSM.process(instruction);
                instructions.add(instruction);

                // System.out.println(Integer.toHexString(instruction.getAddress()) + " " +
                // instruction);
            } while (buffer.position() < funcEndPoint);

            // label the instructions with jumps
            //            ControlFlow.labelBranches(instructions);

            // Control flow final consistency check
            // cfSM.end();

            functionBodies[i] = new FunctionBody(locals, instructions);
        }

        return new CodeSection(sectionId, sectionSize, functionBodies);
    }

    private static DataSection parseDataSection(
            ByteBuffer buffer, long sectionId, long sectionSize) {
        var dataSegmentCount = readVarUInt32(buffer);
        var dataSegments = new DataSegment[(int) dataSegmentCount];

        for (var i = 0; i < dataSegmentCount; i++) {
            var idx = readVarUInt32(buffer);
            var offset = parseExpression(buffer);
            byte[] data = new byte[(int) readVarUInt32(buffer)];
            buffer.get(data);
            dataSegments[i] = new DataSegment(idx, offset, data);
        }

        return new DataSection(sectionId, sectionSize, dataSegments);
    }

    private static Instruction parseInstruction(ByteBuffer buffer) {
        var address = buffer.position();
        var b = buffer.get() & 0xff;
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
     * Parse a varuint32 from the buffer. We can't fit an unsigned 32bit int
     * into a java int, so we must use a long.
     *
     * @param buffer
     * @return
     */
    public static long readVarUInt32(ByteBuffer buffer) {
        return Encoding.readUnsignedLeb128(buffer);
    }

    /**
     * Parse a varsint32 from the buffer. We can't fit an unsigned 32bit int
     * into a java int, so we must use a long to use the same type as unsigned.
     *
     * @param buffer
     * @return
     */
    public static long readVarSInt32(ByteBuffer buffer) {
        return Encoding.readSigned32Leb128(buffer);
    }

    /**
     * Parse a varsint64 from the buffer which fits neatly into a long
     *
     * @param buffer
     * @return
     */
    public static long readVarSInt64(ByteBuffer buffer) {
        return Encoding.readSigned64Leb128(buffer);
    }

    public static long readFloat64(ByteBuffer buffer) {
        return buffer.getLong();
    }

    public static long readFloat32(ByteBuffer buffer) {
        return buffer.getInt();
    }

    public static String readName(ByteBuffer buffer) {
        var length = (int) readVarUInt32(buffer);
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
