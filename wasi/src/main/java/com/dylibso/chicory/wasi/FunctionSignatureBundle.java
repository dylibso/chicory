package com.dylibso.chicory.wasi;

public class FunctionSignatureBundle implements HostModule {

    private final String name;
    private final FunctionSignature[] signatures;

    public FunctionSignatureBundle(String name, FunctionSignature[] signatures) {
        this.name = name;
        this.signatures = signatures;
    }

    @Override
    public FunctionSignature[] signatures() {
        return signatures;
    }

    @Override
    public String name() {
        return name;
    }
}
