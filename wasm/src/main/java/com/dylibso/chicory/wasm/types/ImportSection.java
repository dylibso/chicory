package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import com.dylibso.chicory.wasm.io.WasmParseException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

public final class ImportSection extends Section {
    private final ArrayList<Import> imports;

    /**
     * Construct a new, empty section instance.
     */
    public ImportSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of imports to reserve space for
     */
    public ImportSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private ImportSection(ArrayList<Import> imports) {
        super(SectionId.IMPORT);
        this.imports = imports;
    }

    public int importCount() {
        return imports.size();
    }

    public Import getImport(int idx) {
        return imports.get(idx);
    }

    public Stream<Import> stream() {
        return imports.stream();
    }

    /**
     * Add an import definition to this section.
     *
     * @param import_ the import to add to this section (must not be {@code null})
     * @return the index of the newly-added import
     */
    public int addImport(Import import_) {
        Objects.requireNonNull(import_, "import_");
        int idx = imports.size();
        imports.add(import_);
        return idx;
    }

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var importCount = in.u31();
        imports.ensureCapacity(imports.size() + importCount);

        // Parse individual imports in the import section
        for (int i = 0; i < importCount; i++) {
            String moduleName = in.utf8();
            String importName = in.utf8();
            var descType = ExternalType.byId(in.u8());
            switch (descType) {
                case FUNCTION:
                    {
                        addImport(new FunctionImport(moduleName, importName, in.u31()));
                        break;
                    }
                case TABLE:
                    {
                        var tableType = in.refType();
                        var limitType = in.u32();
                        if (limitType != 0x00 && limitType != 0x01) {
                            throw new WasmParseException("Invalid limit type");
                        }
                        var min = in.u32Long();
                        var limits =
                                limitType > 0 ? new Limits(min, in.u32Long()) : new Limits(min);

                        addImport(new TableImport(moduleName, importName, tableType, limits));
                        break;
                    }
                case MEMORY:
                    {
                        MemoryLimits limits = MemoryLimits.parseFrom(in);
                        addImport(new MemoryImport(moduleName, importName, limits));
                        break;
                    }
                case GLOBAL:
                    var globalValType = in.type();
                    var globalMut = in.mut();
                    addImport(new GlobalImport(moduleName, importName, globalMut, globalValType));
                    break;
            }
        }
    }
}
