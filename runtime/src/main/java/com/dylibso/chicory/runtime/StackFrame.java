package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;

/**
 * Represents a frame. It's maybe a misonomer to call it a stack frame.
 * It doesn't hold the stack, just local variables and the `pc` which
 * is the program counter in this function. Instead of keeping an absolute pointer
 * to positions in code the program counter is relative to the function and we store it
 * here so we know where to resume when we return from an inner function call.
 * This also means it's not possible to set the program counter to an instruction in another function
 * on accident which is a good thing, as this is not allowed in the spec. You can only jump to instructions
 * within the function you are in and only specific places.
 */
public class StackFrame {
    public int funcId;
    public int pc;
    public Value[] locals;
    public int blockDepth;
    private Instance instance;
    public boolean doControlTransfer = false;
    public boolean isControlFrame = true;
    public int stackSizeBeforeBlock;
    public int numberOfValuesToReturn = 0;
    public Value branchConditionValue = null;

    public StackFrame(Instance instance, int funcId, int pc, Value[] args, ValueType[] localTypes) {
        this.instance = instance;
        this.funcId = funcId;
        this.pc = pc;
        this.locals = Arrays.copyOf(args, args.length + localTypes.length);

        // initialize codesegment locals.
        for (var i = 0; i < localTypes.length; i++) {
            ValueType type = localTypes[i];
            // TODO: How do we initialize non-numeric V128
            if (type != ValueType.V128) {
                locals[i + args.length] = Value.zero(type);
            }
        }
        this.blockDepth = 0;
    }

    void setLocal(int i, Value v) {
        this.locals[i] = v;
    }

    Value local(int i) {
        return locals[i];
    }

    @Override
    public String toString() {
        var nameSec = instance.module().nameSection();
        var id = "[" + funcId + "]";
        if (nameSec != null) {
            var funcName = nameSec.functionNames().get(funcId);
            if (funcName != null) id = funcName + id;
        }
        return id + "\n\tpc=" + pc + " locals=" + Arrays.toString(locals);
    }
}
