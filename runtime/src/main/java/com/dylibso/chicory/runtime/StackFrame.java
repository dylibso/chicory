package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a frame, doesn't hold the stack, just local variables and the `pc` which
 * is the program counter in this function. Instead of keeping an absolute pointer
 * to positions in code the program counter is relative to the function and we store it
 * here so we know where to resume when we return from an inner function call.
 * This also means it's not possible to set the program counter to an instruction in another function
 * on accident, as this is not allowed in the spec. You can only jump to instructions
 * within the function you are in and only specific places.
 */
class StackFrame {
    private final List<AnnotatedInstruction> code;
    private AnnotatedInstruction currentInstruction;

    private final int funcId;
    private int pc;
    private final long[] locals;
    private final Instance instance;

    private final List<CtrlFrame> ctrlStack = new ArrayList<>();

    StackFrame(Instance instance, int funcId, long[] args, List<ValueType> localTypes) {
        this(instance, funcId, args, localTypes, Collections.emptyList());
    }

    StackFrame(
            Instance instance,
            int funcId,
            long[] args,
            List<ValueType> localTypes,
            List<AnnotatedInstruction> code) {
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

    void setLocal(int i, long v) {
        this.locals[i] = v;
    }

    long local(int i) {
        return locals[i];
    }

    @Override
    public String toString() {
        var nameSec = instance.module().nameSection();
        var id = "[" + funcId + "]";
        if (nameSec != null) {
            var funcName = nameSec.nameOfFunction(funcId);
            if (funcName != null) {
                id = funcName + id;
            }
        }
        return id + "\n\tpc=" + pc + " locals=" + Arrays.toString(locals);
    }

    AnnotatedInstruction loadCurrentInstruction() {
        currentInstruction = code.get(pc++);
        return currentInstruction;
    }

    boolean isLastBlock() {
        return currentInstruction.depth() == 0;
    }

    boolean terminated() {
        return pc >= code.size();
    }

    void pushCtrl(CtrlFrame ctrlFrame) {
        ctrlStack.add(ctrlFrame);
    }

    void pushCtrl(OpCode opcode, int startValues, int returnValues, int height) {
        ctrlStack.add(new CtrlFrame(opcode, startValues, returnValues, height));
    }

    CtrlFrame popCtrl() {
        var ctrlFrame = ctrlStack.remove(ctrlStack.size() - 1);
        return ctrlFrame;
    }

    CtrlFrame popCtrl(int n) {
        int mostRecentCallHeight = ctrlStack.size();
        while (true) {
            if (ctrlStack.get(--mostRecentCallHeight).opCode == OpCode.CALL) {
                break;
            }
        }
        var finalHeight = ctrlStack.size() - (mostRecentCallHeight + n + 1);
        CtrlFrame ctrlFrame = null;
        while (ctrlStack.size() > finalHeight) {
            ctrlFrame = popCtrl();
        }
        return ctrlFrame;
    }

    CtrlFrame popCtrlTillCall() {
        while (true) {
            var ctrlFrame = popCtrl();
            if (ctrlFrame.opCode == OpCode.CALL) {
                return ctrlFrame;
            }
        }
    }

    void jumpTo(int newPc) {
        pc = newPc;
    }

    static void doControlTransfer(CtrlFrame ctrlFrame, MStack stack) {
        var endResults = ctrlFrame.startValues + ctrlFrame.endValues; // unwind stack
        long[] returns = new long[endResults];
        for (int i = 0; i < returns.length; i++) {
            if (stack.size() > 0) {
                returns[i] = stack.pop();
            }
        }

        while (stack.size() > ctrlFrame.height) {
            stack.pop();
        }

        for (int i = 0; i < returns.length; i++) {
            long value = returns[returns.length - 1 - i];
            stack.push(value);
        }
    }
}
