package com.dylibso.chicory.wasm.types;

public class ImportDesc {
    private int index;
    private ImportDescType type;
    private MutabilityType mutabilityType;
    private ValueType valType;
    private Limits limits;

    public ImportDesc(ImportDescType type, int index) {
        this.type = type;
        this.index = index;
    }

    public ImportDesc(ImportDescType type, MutabilityType mutabilityType, ValueType valType) {
        this.type = type;
        this.mutabilityType = mutabilityType;
        this.valType = valType;
    }

    public ImportDesc(ImportDescType type, Limits limits) {
        this.type = type;
        this.limits = limits;
        this.valType = null;
    }

    public ImportDesc(ImportDescType type, Limits limits, ValueType valType) {
        assert (valType == ValueType.FuncRef || valType == ValueType.ExternRef);
        this.type = type;
        this.limits = limits;
        this.valType = valType;
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
                return "func[]";
            case TableIdx:
                return "table[]";
            case MemIdx:
                return "memory[]";
            case GlobalIdx:
                return "global[] " + valType + " mutability=" + mutabilityType;
            default:
                return "unknown[]";
        }
    }
}
