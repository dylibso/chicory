package com.dylibso.chicory.observable.runtime;

import static com.dylibso.chicory.wasm.types.ValueType.sizeOf;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.MStack;
import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.NameCustomSection;
import com.dylibso.chicory.wasm.types.OpCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Deque;

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
    protected long[] call(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            int funcId,
            long[] args,
            FunctionType callType,
            boolean popResults,
            CallCtx ctx)
            throws ChicoryException {
        String spanName = "function_" + funcId; // just a safe fallback
        String funcType;

        checkInterruption();
        var typeId = instance.functionType(funcId);
        var type = instance.type(typeId);

        if (callType != null) {
            verifyIndirectCall(type, callType);
        }

        var func = instance.function(funcId);
        if (func != null) {
            funcType = "guest";
            var customSection = instance.module().customSection("name");

            if (customSection != null && customSection instanceof NameCustomSection) {
                var nameCustomSection = (NameCustomSection) customSection;
                var funcName = nameCustomSection.nameOfFunction(funcId);
                if (funcName != null) {
                    spanName = funcName + "[" + funcId + "]";
                }
            }

            var stackFrame =
                    new StackFrame(
                            instance,
                            funcId,
                            args,
                            type.params(),
                            func.localTypes(),
                            func.instructions());
            stackFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
            callStack.push(stackFrame);

            var spanBuilder = tracer.spanBuilder(spanName);
            if (ctx != null) {
                // System.out.println("DEBUG: parent is NOT null - " + spanName);
                spanBuilder.setParent(((ObservabilityContext) ctx).ctx());
            }
            var span = spanBuilder.startSpan();
            ctx = new ObservabilityContext(Context.current().with(span));
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("function-type", funcType);
                for (int i = 0; i < args.length; i++) {
                    span.setAttribute("arg[" + i + "]", args[i]);
                }
                eval(stack, instance, callStack, ctx);
            } catch (StackOverflowError e) {
                throw new ChicoryException("call stack exhausted", e);
            } catch (Exception e) {
                span.recordException(e);
                throw e;
            } finally {
                span.end();
            }
        } else {
            funcType = "host";

            var importFunc = instance.imports().function(funcId);
            spanName = importFunc.module() + "." + importFunc.name() + "[" + funcId + "]";

            var stackFrame = new StackFrame(instance, funcId, args);
            stackFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
            callStack.push(stackFrame);

            var imprt = instance.imports().function(funcId);

            var spanBuilder = tracer.spanBuilder(spanName);
            if (ctx != null) {
                spanBuilder.setParent(((ObservabilityContext) ctx).ctx());
            }
            var span = spanBuilder.startSpan();
            ctx = new ObservabilityContext(Context.current().with(span));
            long[] results;
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("function-type", funcType);
                results = imprt.handle().apply(instance, args);
            } catch (Exception e) {
                span.recordException(e);
                throw e;
            } finally {
                span.end();
            }
            // a host function can return null or an array of ints
            // which we will push onto the stack
            if (results != null) {
                for (var result : results) {
                    stack.push(result);
                }
            }
        }

        if (!callStack.isEmpty()) {
            callStack.pop();
        }

        if (!popResults) {
            return null;
        }

        if (type.returns().isEmpty()) {
            return null;
        }
        if (stack.size() == 0) {
            return null;
        }

        var totalResults = sizeOf(type.returns());
        var results = new long[totalResults];
        for (var i = totalResults - 1; i >= 0; i--) {
            results[i] = stack.pop();
        }
        return results;
    }

    // @Override
    // public long[] call(int funcId, long[] args, CallCtx ctx) throws ChicoryException {
    //        var func = instance.function(funcId);
    //        String spanName = "function_" + funcId; // just a safe fallback
    //        String funcType;
    //        if (func != null) { // wasm guest function
    //            funcType = "guest";
    //            var customSection = instance.module().customSection("name");
    //
    //            if (customSection != null && customSection instanceof NameCustomSection) {
    //                var nameCustomSection = (NameCustomSection) customSection;
    //                var funcName = nameCustomSection.nameOfFunction(funcId);
    //                if (funcName != null) {
    //                    spanName = funcName + "[" + funcId + "]";
    //                }
    //            }
    //        } else { // host function
    //            funcType = "host";
    //
    //            var importFunc = instance.imports().function(funcId);
    //            spanName = importFunc.module() + "." + importFunc.name() + "[" + funcId + "]";
    //        }
    //
    //        // OtelContextStorage
    //        // Context contextWithSpan = Context.current().with(span);
    //        // var parentSpan = Context.current();
    //        // System.out.println("DEBUG: " + funcId + " - " + parentSpan);
    //        var spanBuilder = tracer.spanBuilder(spanName);
    //        if (ctx != null) {
    //            System.out.println("DEBUG: " + ctx);
    //            spanBuilder.setParent(((ObservabilityContext) ctx).ctx());
    //        } else {
    //            System.out.println("DEBUG: " + spanName + " is root?");
    //            ctx = new ObservabilityContext(Context.root());
    //        }
    //        var span = spanBuilder.startSpan();
    //        try (Scope scope = span.makeCurrent()) {
    //            span.setAttribute("function-type", funcType);
    //            return super.call(
    //                    this.stack, this.instance, this.callStack, funcId, args, null, true, ctx);
    //        } catch (Exception e) {
    //            span.recordException(e);
    //            throw e;
    //        } finally {
    //            span.end();
    //        }
    //    }
}
