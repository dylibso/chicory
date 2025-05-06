package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AotInterpreterMachine extends InterpreterMachine {

    private static final HashSet<Integer> usedInterpretedFunctions;

    static {
        if (Boolean.parseBoolean(
                System.getProperty("chicory.compiler.printUseOfInterpretedFunctions"))) {
            usedInterpretedFunctions = new HashSet<>();
        } else {
            usedInterpretedFunctions = null;
        }
    }

    Set<Integer> interpretedFuncIds;

    public AotInterpreterMachine(Instance instance, int[] interpretedFuncIds) {
        super(instance);
        this.interpretedFuncIds =
                java.util.Arrays.stream(interpretedFuncIds).boxed().collect(Collectors.toSet());
    }

    @Override
    protected long[] call(
            MStack stack,
            Instance instance,
            Deque<StackFrame> callStack,
            int funcId,
            long[] args,
            FunctionType callType,
            boolean popResults)
            throws ChicoryException {
        if (usedInterpretedFunctions != null && !usedInterpretedFunctions.contains(funcId)) {
            usedInterpretedFunctions.add(funcId);
            System.err.println("Chicory: calling interpreted function " + funcId);
        }
        return super.call(stack, instance, callStack, funcId, args, callType, popResults);
    }

    @Override
    protected void CALL(Operands operands) {
        var instance = instance();
        var funcId = (int) operands.get(0);
        if (interpretedFuncIds.contains(funcId) || instance.function(funcId) == null) {
            // continue interpreting for interpreted functions or imported functions
            super.CALL(operands);
        } else {
            // We end up here after a function switched to interpreted mode,
            // going back to Java bytecode.
            //
            // Must be an AOT function: switch back the to the AOT machine that's assigned to the
            // instance.

            var stack = stack();
            var typeId = instance.functionType(funcId);
            var type = instance.type(typeId);
            var args = extractArgsForParams(stack, type.params());

            var results = instance.getMachine().call(funcId, args);
            // a host function can return null or an array of ints
            // which we will push onto the stack
            if (results != null) {
                for (var result : results) {
                    stack.push(result);
                }
            }
        }
    }

    @Override
    protected boolean useCurrentInstanceInterpreter(
            Instance instance, Instance refInstance, int funcId) {
        // this function influence the behavior of CALL_INDIRECT without rewriting it
        // if we are on the same instance and the next invoked function needs to stay
        // in interpreted mode, alternatively go through `Machine::call`
        return refInstance.equals(instance) && interpretedFuncIds.contains(funcId);
    }
}
