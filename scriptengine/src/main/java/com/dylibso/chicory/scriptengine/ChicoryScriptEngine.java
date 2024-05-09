package com.dylibso.chicory.scriptengine;

import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasm.types.Value;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Objects;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class ChicoryScriptEngine implements ScriptEngine {

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        return null;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return null;
    }

    @Override
    public Object eval(String script) throws ScriptException {
        return eval(script, (ScriptContext) null);
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return eval(reader, (ScriptContext) null);
    }

    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException {
        return eval(new StringReader(script), bindings);
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {

        StringWriter sw = new StringWriter();
        try {
            reader.transferTo(sw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] wasmBytes =
                Base64.getDecoder().decode(Objects.requireNonNull(sw.toString(), "script"));

        int arg0 = ((Integer) bindings.get("arg0")).intValue();
        String func = (String) bindings.get("func");

        var module = Module.builder(wasmBytes).build();
        var instance = module.instantiate();
        var iterFact = instance.export(func);
        var result = iterFact.apply(Value.i32(arg0))[0].asInt();

        return result;
    }

    @Override
    public void put(String key, Object value) {}

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public Bindings getBindings(int scope) {
        return null;
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {}

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptContext getContext() {
        return null;
    }

    @Override
    public void setContext(ScriptContext context) {}

    @Override
    public ScriptEngineFactory getFactory() {
        return null;
    }
}
