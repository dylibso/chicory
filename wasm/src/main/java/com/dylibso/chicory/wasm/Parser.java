package com.dylibso.chicory.wasm;

import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import com.dylibso.chicory.wasm.types.CodeSection;
import com.dylibso.chicory.wasm.types.CustomSection;
import com.dylibso.chicory.wasm.types.DataSection;
import com.dylibso.chicory.wasm.types.ElementSection;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.FunctionSection;
import com.dylibso.chicory.wasm.types.GlobalSection;
import com.dylibso.chicory.wasm.types.ImportSection;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MemorySection;
import com.dylibso.chicory.wasm.types.NameCustomSection;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Section;
import com.dylibso.chicory.wasm.types.SectionId;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.TableSection;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.UnknownCustomSection;
import com.dylibso.chicory.wasm.types.UnknownSection;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.function.Function;

/**
 * Parser for Web Assembly binaries.
 */
public final class Parser {

    private static final int MAGIC_BYTES = 1836278016; // Magic prefix \0asm

    private final Map<String, Function<String, CustomSection>> customSectionFactories;
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
            Map<String, Function<String, CustomSection>> customSectionFactories) {
        this.logger = requireNonNull(logger, "logger");
        this.includeSections = requireNonNull(includeSections, "includeSections");
        this.customSectionFactories = Map.copyOf(customSectionFactories);
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
                    Section section;
                    // Process different section types based on the sectionId
                    switch (sectionId) {
                        case SectionId.CUSTOM:
                            {
                                var name = in.utf8();
                                section =
                                        customSectionFactories
                                                .getOrDefault(name, UnknownCustomSection::new)
                                                .apply(name);
                                break;
                            }
                        case SectionId.TYPE:
                            {
                                section = new TypeSection();
                                break;
                            }
                        case SectionId.IMPORT:
                            {
                                section = new ImportSection();
                                break;
                            }
                        case SectionId.FUNCTION:
                            {
                                section = new FunctionSection();
                                break;
                            }
                        case SectionId.TABLE:
                            {
                                section = new TableSection();
                                break;
                            }
                        case SectionId.MEMORY:
                            {
                                section = new MemorySection();
                                break;
                            }
                        case SectionId.GLOBAL:
                            {
                                section = new GlobalSection();
                                break;
                            }
                        case SectionId.EXPORT:
                            {
                                section = new ExportSection();
                                break;
                            }
                        case SectionId.START:
                            {
                                section = new StartSection();
                                break;
                            }
                        case SectionId.ELEMENT:
                            {
                                section = new ElementSection();
                                break;
                            }
                        case SectionId.CODE:
                            {
                                section = new CodeSection();
                                break;
                            }
                        case SectionId.DATA:
                            {
                                section = new DataSection();
                                break;
                            }
                        default:
                            {
                                section = new UnknownSection(sectionId);
                                break;
                            }
                    }

                    // parse it
                    section.readFrom(sectionData);
                    // notify listeners of parsed section
                    listener.onSection(section);
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

    public static Instruction[] parseExpression(WasmInputStream in) {
        var expr = new ArrayList<Instruction>();
        while (true) {
            var i = Instruction.readFrom(in);
            if (i.opcode() == OpCode.END) {
                break;
            }
            expr.add(i);
        }
        return expr.toArray(Instruction[]::new);
    }
}
