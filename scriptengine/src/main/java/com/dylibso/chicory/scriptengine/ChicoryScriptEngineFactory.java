package com.dylibso.chicory.scriptengine;

import com.google.auto.service.AutoService;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

@AutoService(ScriptEngineFactory.class)
public class ChicoryScriptEngineFactory implements ScriptEngineFactory {

    @Override
    public String getEngineName() {
        return "Chicory ScriptEngine";
    }

    @Override
    public String getEngineVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public List<String> getExtensions() {
        return List.of("wasm");
    }

    @Override
    public List<String> getMimeTypes() {
        return List.of("application/wasm");
    }

    @Override
    public List<String> getNames() {
        return List.of("chicory");
    }

    @Override
    public String getLanguageName() {
        return "WebAssembly";
    }

    @Override
    public String getLanguageVersion() {
        return "1.0";
    }

    @Override
    public Object getParameter(String key) {
        return null;
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        return "";
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "";
    }

    @Override
    public String getProgram(String... statements) {
        return "";
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new ChicoryScriptEngine();
    }
}
