package com.dylibso.chicory.observable.runtime;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.types.NameCustomSection;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * This is responsible for holding and interpreting the Wasm code.
 */
public class ObservableInterpreterMachine extends InterpreterMachine {
    private static final String OBSERVABILITY_TRACER_NAME = "com.dylibso.chicory";
    private final Tracer tracer;

    private static OpenTelemetrySdk autoconfiguredSdk() {
        return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
    }

    public ObservableInterpreterMachine(Instance instance) {
        super(instance);
        tracer = autoconfiguredSdk().getSdkTracerProvider().get(OBSERVABILITY_TRACER_NAME);
    }

    @Override
    public long[] call(int funcId, long[] args) throws ChicoryException {
        var func = instance.function(funcId);
        String spanName = "function_" + funcId; // just a safe fallback
        String funcType;
        if (func != null) { // wasm guest function
            funcType = "guest";
            var customSection = instance.module().customSection("name");

            if (customSection != null && customSection instanceof NameCustomSection) {
                var nameCustomSection = (NameCustomSection) customSection;
                var funcName = nameCustomSection.nameOfFunction(funcId);
                if (funcName != null) {
                    spanName = funcName;
                }
            }
        } else { // host function
            funcType = "host";

            var importFunc = instance.imports().function(funcId);
            spanName = importFunc.module() + "." + importFunc.name();
        }

        Span span = tracer.spanBuilder(spanName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("function-type", funcType);
            return super.call(funcId, args);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
