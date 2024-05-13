package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;

/**
 * Class for tracking context relevant to compiling a single function
 */
public class AotContext {

    protected final int funcId;
    protected final FunctionType type;
    protected final FunctionBody body;

    public AotContext(int funcId, FunctionType type, FunctionBody body) {
        this.funcId = funcId;
        this.type = type;
        this.body = body;
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
}
