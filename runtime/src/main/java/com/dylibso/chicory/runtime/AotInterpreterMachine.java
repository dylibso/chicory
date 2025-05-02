package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.ValType.sizeOf;

import com.dylibso.chicory.wasm.types.OpCode;
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
            var callStack = callStack();

            var typeId = instance.functionType(funcId);
            var type = instance.type(typeId);
            var args = extractArgsForParams(stack, type.params());

            // This works like import func calls.
            var stackFrame = new StackFrame(instance, funcId, args);
            stackFrame.pushCtrl(OpCode.CALL, 0, sizeOf(type.returns()), stack.size());
            callStack.push(stackFrame);
            try {
                var results = instance.getMachine().call(funcId, args);
                // a host function can return null or an array of ints
                // which we will push onto the stack
                if (results != null) {
                    for (var result : results) {
                        stack.push(result);
                    }
                }
            } catch (WasmException e) {
                THROW_REF(instance, instance.registerException(e), stack, stackFrame, callStack);
            }
        }
    }

    @Override
    protected boolean useMachineCallForIndirectCall(
            Instance instance, Instance refInstance, int funcId) {
        return !refInstance.equals(instance) || !interpretedFuncIds.contains(funcId);
    }
}
