package com.dylibso.chicory.runtime;

import java.util.Set;
import java.util.stream.Collectors;

public class AotInterpreterMachine extends InterpreterMachine {

    Set<Integer> interpretedFuncIds;

    public AotInterpreterMachine(Instance instance, int[] interpretedFuncIds) {
        super(instance);
        this.interpretedFuncIds =
                java.util.Arrays.stream(interpretedFuncIds).boxed().collect(Collectors.toSet());
    }

    @Override
    protected void CALL(Operands operands) {
        var instance = instance();
        var funcId = (int) operands.get(0);
        if (interpretedFuncIds.contains(funcId) || instance.function(funcId) == null) {
            // continue interpreting for interpreted functions or imported functions
            super.CALL(operands);
        } else {
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
    protected boolean skipMachineForIndirectCall(
            Instance instance, Instance refInstance, int funcId) {
        return refInstance.equals(instance) && interpretedFuncIds.contains(funcId);
    }
}
