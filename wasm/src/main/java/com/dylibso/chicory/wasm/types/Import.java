package com.dylibso.chicory.wasm.types;

public class Import {
    private String moduleName;
    private String fieldName;
    private ImportDesc desc;

    public Import(String moduleName, String fieldName, ImportDesc desc) {
        this.moduleName = moduleName;
        this.fieldName = fieldName;
        this.desc = desc;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public ImportDesc getDesc() {
        return desc;
    }

    public String toString() {
        var builder = new StringBuilder();
        builder.append(desc.toString());
        builder.append(" <");
        builder.append(moduleName);
        builder.append('.');
        builder.append(fieldName);
        builder.append('>');
        return builder.toString();
    }
}
