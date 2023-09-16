package com.dylibso.chicory.wasm.types;

public class ImportDesc {
    private final long index;
    private final ImportDescType type;

    public ImportDesc(long index, ImportDescType type) {
        this.index = index;
        this.type = type;
    }

    public long getIndex() {
        return index;
    }

    public ImportDescType getType() {
        return type;
    }

    public String toString() {
        return switch (type) {
            case FuncIdx -> "func["+index+"]";
            case TableIdx -> "table["+index+"]";
            case MemIdx -> "memory["+index+"]";
            case GlobalIdx -> "global["+index+"]";
        };
    }
}
