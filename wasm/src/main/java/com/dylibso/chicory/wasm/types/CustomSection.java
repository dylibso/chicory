package com.dylibso.chicory.wasm.types;

public class CustomSection extends Section {
    private String name;

    private byte[] bytes;

    public CustomSection(long id, long size) {
       super(id, size);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
