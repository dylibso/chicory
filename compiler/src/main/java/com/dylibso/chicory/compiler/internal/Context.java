package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.compiler.internal.CompilerUtil.hasTooManyParameters;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.slotCount;

import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.TagImport;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for tracking context relevant to compiling a single function
 */
final class Context {

    private final WasmModule module;
    private final String internalClassName;
    private final int maxFunctionsPerClass;
    private final List<ValType> globalTypes;
    private final List<FunctionType> functionTypes;
    private final int funcId;
    private final FunctionType type;
    private final FunctionBody body;
    private final List<Integer> slots;
    private final int memorySlot;
    private final int instanceSlot;
    private final int tempSlot;
    private final List<TagImport> tagImports;

    public Context(
            WasmModule module,
            String internalClassName,
            int maxFunctionsPerClass,
            List<ValType> globalTypes,
            List<FunctionType> functionTypes,
            int funcId,
            FunctionType type,
            FunctionBody body) {
        this.module = module;
        this.internalClassName = internalClassName;
        this.maxFunctionsPerClass = maxFunctionsPerClass;
        this.globalTypes = globalTypes;
        this.functionTypes = functionTypes;
        this.funcId = funcId;
        this.type = type;
        this.body = body;

        // compute JVM slot indices for WASM locals
        List<Integer> slots = new ArrayList<>(type.params().size() + body.localTypes().size());
        int slot = 0;

        // WASM arguments
        if (hasTooManyParameters(type)) {
            slot += 1; // long[]
        } else {
            for (ValType param : type.params()) {
                slots.add(slot);
                slot += slotCount(param);
            }
        }

        // extra arguments
        this.memorySlot = slot;
        slot++;
        this.instanceSlot = slot;
        slot++;

        // the long[] gets unboxed
        if (hasTooManyParameters(type)) {
            for (ValType param : type.params()) {
                slots.add(slot);
                slot += slotCount(param);
            }
        }

        // WASM locals
        for (ValType local : body.localTypes()) {
            slots.add(slot);
            slot += slotCount(local);
        }

        this.slots = List.copyOf(slots);
        this.tempSlot = slot;

        this.tagImports =
                module.importSection().stream()
                        .filter((x) -> x.importType() == ExternalType.TAG)
                        .map((x) -> (TagImport) x)
                        .collect(Collectors.toList());
    }

    public String internalClassName() {
        return internalClassName;
    }

    public List<ValType> globalTypes() {
        return globalTypes;
    }

    public List<FunctionType> functionTypes() {
        return functionTypes;
    }

    public FunctionType type(int idx) {
        return module.typeSection().getType(idx);
    }

    public FunctionType[] types() {
        return module.typeSection().types();
    }

    public TypeSection typeSection() {
        return module.typeSection();
    }

    public int getId() {
        return funcId;
    }

    public FunctionType getType() {
        return type;
    }

    public FunctionBody getBody() {
        return body;
    }

    public int localSlotIndex(int localIndex) {
        return slots.get(localIndex);
    }

    public int memorySlot() {
        return memorySlot;
    }

    public int instanceSlot() {
        return instanceSlot;
    }

    public int tempSlot() {
        return tempSlot;
    }

    public String classNameForFuncGroup(String prefix, int funcId) {
        return prefix + "FuncGroup_" + (funcId / maxFunctionsPerClass);
    }

    public FunctionType tagFunctionType(int tagId) {
        if (tagId < 0) {
            throw new IllegalArgumentException("Tag ID must be non-negative");
        }
        int idx;
        if (tagId < tagImports.size()) {
            var tag = tagImports.get(tagId);
            idx = tag.tagType().typeIdx();
        } else {
            if (module.tagSection().isEmpty()) {
                throw new IllegalStateException("No tag section available");
            }
            idx = module.tagSection().get().getTag(tagId - tagImports.size()).typeIdx();
        }
        return type(idx);
    }
}
