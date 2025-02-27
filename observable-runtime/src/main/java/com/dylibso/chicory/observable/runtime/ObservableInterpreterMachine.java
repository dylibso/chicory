package com.dylibso.chicory.observable.runtime;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.types.NameCustomSection;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * This is responsible for holding and interpreting the Wasm code.
 */
public class ObservableInterpreterMachine extends InterpreterMachine {
    private static final String OBSERVABILITY_TRACER_NAME = "com.dylibso.chicory";
    private final Tracer tracer;

    public ObservableInterpreterMachine(Instance instance) {
        super(instance);
        tracer =
                OtelAutoconfiguredSDK.autoconfiguredSdk()
                        .getSdkTracerProvider()
                        .get(OBSERVABILITY_TRACER_NAME);
    }

    private static class ObservabilityContext implements CallCtx {
        private Context ctx;

        public ObservabilityContext(Context ctx) {
            this.ctx = ctx;
        }

        public Context ctx() {
            return this.ctx;
        }
    }

    @Override
    public long[] call(int funcId, long[] args, CallCtx ctx) throws ChicoryException {
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
                    spanName = funcName + "[" + funcId + "]";
                }
            }
        } else { // host function
            funcType = "host";

            var importFunc = instance.imports().function(funcId);
            spanName = importFunc.module() + "." + importFunc.name() + "[" + funcId + "]";
        }

        // OtelContextStorage
        // Context contextWithSpan = Context.current().with(span);
        // var parentSpan = Context.current();
        // System.out.println("DEBUG: " + funcId + " - " + parentSpan);
        var spanBuilder = tracer.spanBuilder(spanName);
        if (ctx != null) {
            System.out.println("DEBUG: " + ctx);
            spanBuilder.setParent(((ObservabilityContext) ctx).ctx());
        } else {
            System.out.println("DEBUG: " + spanName + " is root?");
            ctx = new ObservabilityContext(Context.root());
        }
        var span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("function-type", funcType);
            return call(stack, instance, callStack, funcId, args, null, true, ctx);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
