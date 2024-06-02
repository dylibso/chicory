package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public boolean doControlTransfer = false;
    public boolean isControlFrame = true;

    private final List<Instruction> code;
    private Instruction currentInstruction;

    private final int funcId;
    private int pc;
    private final Value[] locals;
    private final Instance instance;

    private final ArrayDeque<Integer> stackSizeBeforeBlock = new ArrayDeque<>();

    public StackFrame(Instance instance, int funcId, Value[] args, List<ValueType> localTypes) {
        this(Collections.emptyList(), instance, funcId, args, localTypes);
    }

    public StackFrame(List<Instruction> code, Instance instance, int funcId, Value[] args, List<ValueType> localTypes) {
        this.code = code;
        this.instance = instance;
        this.funcId = funcId;
        this.locals = Arrays.copyOf(args, args.length + localTypes.size());

        // initialize codesegment locals.
        for (var i = 0; i < localTypes.size(); i++) {
            ValueType type = localTypes.get(i);
            // TODO: How do we initialize non-numeric V128
            if (type != ValueType.V128) {
                locals[i + args.length] = Value.zero(type);
            }
        }
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
            var funcName = nameSec.nameOfFunction(funcId);
            if (funcName != null) id = funcName + id;
        }
        return id + "\n\tpc=" + pc + " locals=" + Arrays.toString(locals);
    }

    public Instruction loadCurrentInstruction() {
        currentInstruction = code.get(pc++);
        return currentInstruction;
    }

    public boolean isLastBlock() {
        return currentInstruction.depth() == 0;
    }

    public boolean terminated() {
        return pc >= code.size();
    }

    public void registerStackSize(MStack stack) {
        stackSizeBeforeBlock.push(stack.size());
    }

    public void jumpTo(int newPc) {
        pc = newPc;
    }

    public void dropValuesOutOfBlock(MStack stack) {
        if (currentInstruction.depth() > 0) {
            while (stackSizeBeforeBlock.size() > currentInstruction.depth()) {
                stackSizeBeforeBlock.pop();
            }

            int expectedStackSize = stackSizeBeforeBlock.pop();
            while (stack.size() > expectedStackSize) {
                stack.pop();
            }
        }
    }

    public void endOfNonControlBlock() {
        if (currentInstruction.depth() > 0) {
            stackSizeBeforeBlock.pop();
        }
    }
}
