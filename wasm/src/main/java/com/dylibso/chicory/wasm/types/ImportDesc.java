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
        switch (type) {
            case FuncIdx:
                return "func["+index+"]";
            case TableIdx:
                return "table["+index+"]";
            case MemIdx:
                return "memory["+index+"]";
            case GlobalIdx:
                return "global["+index+"]";
            default:
                return "unknown["+index+"]";
        }
        // if (type instanceof FuncIdx) {
        //     return "func["+index+"]";
        // } else if (type instanceof TableIdx) {
        //     return "table["+index+"]";
        // } else if (type instanceof MemIdx) {
        //     return "memory["+index+"]";
        // } else if (type instanceof GlobalIdx) {
        //     return "global["+index+"]";
        // }

    }
}
