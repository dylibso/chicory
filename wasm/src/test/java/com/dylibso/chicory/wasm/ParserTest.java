package com.dylibso.chicory.wasm;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.CustomSection;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.SectionId;
import com.dylibso.chicory.wasm.types.ValType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ParserTest {

    @Test
    public void shouldParseFile() throws IOException {
        try (InputStream is = CorpusResources.getResource("compiled/start.wat.wasm")) {
            var module = Parser.parse(is);

            // check types section
            var typeSection = module.typeSection();
            var types = typeSection.types();
            assertEquals(2, types.length);
            assertEquals("(I32) -> nil", types[0].toString());
            assertEquals("() -> nil", types[1].toString());

            // check import section
            var importSection = module.importSection();
            assertEquals(1, importSection.importCount());
            assertEquals(ExternalType.FUNCTION, importSection.getImport(0).importType());
            assertEquals("env", importSection.getImport(0).module());
            assertEquals("gotit", importSection.getImport(0).name());

            // check data section
            var dataSection = module.dataSection();
            assertEquals(1, dataSection.dataSegmentCount());
            var segment = (ActiveDataSegment) dataSection.getDataSegment(0);
            assertEquals(0, segment.index());
            assertEquals(OpCode.I32_CONST, segment.offsetInstructions().get(0).opcode());
            assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x03}, segment.data());

            // check start section
            var startSection = module.startSection();
            assertEquals(1, startSection.get().startIndex());

            // check function section
            var funcSection = module.functionSection();
            assertEquals(1, funcSection.functionCount());
            assertEquals(1, funcSection.getFunctionType(0));

            // check export section
            //        var exportSection = module.getExportSection();
            //        var exports = exportSection.getExports();
            //        assertEquals(1, exports.size());

            // check memory section
            var memorySection = module.memorySection();
            assertEquals(1, memorySection.get().memoryCount());
            assertEquals(1, memorySection.get().getMemory(0).limits().initialPages());
            assertEquals(65536, memorySection.get().getMemory(0).limits().maximumPages());

            var codeSection = module.codeSection();
            assertEquals(1, codeSection.functionBodyCount());
            var func = codeSection.getFunctionBody(0);
            assertEquals(0, func.localTypes().size());
            var instructions = func.instructions();
            assertEquals(3, instructions.size());

            assertTrue(instructions.get(0).toString().contains("0x00000032: I32_CONST [42]"));
            assertEquals(OpCode.I32_CONST, instructions.get(0).opcode());
            assertEquals(42L, instructions.get(0).operand(0));
            assertEquals(OpCode.CALL, instructions.get(1).opcode());
            assertEquals(0L, instructions.get(1).operand(0));
            assertEquals(OpCode.END, instructions.get(2).opcode());
        }
    }

    @Test
    public void shouldParseIterfact() throws IOException {
        try (InputStream is = CorpusResources.getResource("compiled/iterfact.wat.wasm")) {
            var module = Parser.parse(is);

            // check types section
            var typeSection = module.typeSection();
            var types = typeSection.types();
            assertEquals(1, types.length);
            assertEquals("(I32) -> (I32)", types[0].toString());

            // check function section
            var funcSection = module.functionSection();
            assertEquals(1, funcSection.functionCount());
            assertEquals(0L, funcSection.getFunctionType(0));

            var codeSection = module.codeSection();
            assertEquals(1, codeSection.functionBodyCount());
            var func = codeSection.getFunctionBody(0);
            var locals = func.localTypes();
            assertEquals(1, locals.size());
            assertEquals(ValType.I32, locals.get(0));
            var instructions = func.instructions();
            assertEquals(22, instructions.size());
        }
    }

    @Test
    public void shouldParseAllFiles() throws IOException {
        for (var f : wasmCorpusFiles()) {
            try (InputStream is = new FileInputStream(f)) {
                Parser.parse(is);
            } catch (IOException | RuntimeException e) {
                throw new RuntimeException(String.format("Failed to parse file %s", f), e);
            }
        }
    }

    @Test
    public void shouldSupportCustomListener() throws IOException {
        var parser = Parser.builder().includeSectionId(SectionId.CUSTOM).build();

        try (InputStream is = CorpusResources.getResource("compiled/count_vowels.rs.wasm")) {
            parser.parse(
                    is,
                    s -> {
                        if (s.sectionId() == SectionId.CUSTOM) {
                            var customSection = (CustomSection) s;
                            var name = customSection.name();
                            assertFalse(name.isEmpty());
                        } else {
                            fail("Should not have received section with id: " + s.sectionId());
                        }
                    });
        }
    }

    @Test
    public void shouldParseFloats() throws IOException {
        try (InputStream is = CorpusResources.getResource("compiled/float.wat.wasm")) {
            var module = Parser.parse(is);
            var codeSection = module.codeSection();
            var fbody = codeSection.getFunctionBody(0);
            var f32 = Float.intBitsToFloat((int) fbody.instructions().get(0).operand(0));
            assertEquals(0.12345678f, f32, 0.0);
            var f64 = Double.longBitsToDouble(fbody.instructions().get(1).operand(0));
            assertEquals(0.123456789012345d, f64, 0.0);
        }
    }

    @Test
    public void shouldProperlyParseSignedValue() throws IOException {
        try (InputStream is = CorpusResources.getResource("compiled/i32.wat.wasm")) {
            var module = Parser.parse(is);
            var codeSection = module.codeSection();
            var fbody = codeSection.getFunctionBody(0);
            assertEquals(-2147483648L, fbody.instructions().get(0).operand(0));
            assertEquals(0L, fbody.instructions().get(2).operand(0));
            assertEquals(2147483647L, fbody.instructions().get(4).operand(0));
            assertEquals(-9223372036854775808L, fbody.instructions().get(6).operand(0));
            assertEquals(0L, fbody.instructions().get(8).operand(0));
            assertEquals(9223372036854775807L, fbody.instructions().get(10).operand(0));
            assertEquals(-2147483647L, fbody.instructions().get(12).operand(0));
            assertEquals(2147483646L, fbody.instructions().get(14).operand(0));
            assertEquals(-9223372036854775807L, fbody.instructions().get(16).operand(0));
            assertEquals(9223372036854775806L, fbody.instructions().get(18).operand(0));
            assertEquals(-1L, fbody.instructions().get(20).operand(0));
            assertEquals(1L, fbody.instructions().get(22).operand(0));
            assertEquals(-1L, fbody.instructions().get(24).operand(0));
            assertEquals(1L, fbody.instructions().get(26).operand(0));
        }
    }

    @Test
    public void shouldParseLocalDefinitions() throws Exception {
        try (InputStream is = CorpusResources.getResource("compiled/define-locals.wat.wasm")) {
            var module = Parser.parse(is);
            var codeSection = module.codeSection();
            var fbody = codeSection.getFunctionBody(0);
            assertEquals(fbody.localTypes().get(0), ValType.I32);
            assertEquals(fbody.localTypes().get(1), ValType.I64);
        }
    }

    @Test
    public void shouldParseNamesSection() throws IOException {
        try (InputStream is = CorpusResources.getResource("compiled/count_vowels.rs.wasm")) {
            var module = Parser.parse(is);
            var nameSec = module.nameSection();
            assertEquals(module.codeSection().functionBodyCount(), nameSec.functionNameCount());
            assertEquals("__stack_pointer", nameSec.nameOfGlobal(0));
            assertEquals(".rodata", nameSec.nameOfData(0));
        }
    }

    @Test
    public void shouldParseSIMD() throws IOException {
        try (InputStream is = CorpusResources.getResource("wasm/simd_load.0.wasm")) {
            Parser.parse(is);
        }
    }

    static List<File> wasmCorpusFiles() throws IOException {
        var compiledDir = new File("../wasm-corpus/src/main/resources/compiled/");
        try (var stream = Files.list(compiledDir.toPath())) {
            var files =
                    stream.map(Path::toFile)
                            .filter(f -> f.getName().toLowerCase(Locale.ROOT).endsWith(".wasm"))
                            .collect(toList());
            if (files.isEmpty()) {
                throw new IOException("Could not find files");
            }
            return files;
        }
    }

    @Test
    public void shouldParseOnlyImportedTags() throws IOException {
        try (InputStream is = CorpusResources.getResource("compiled/issue_906.wat.wasm")) {
            Parser.parse(is);
        }
    }
}
