package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

import java.util.Arrays;
import java.util.HashMap;
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
    public int funcId;
    public int pc;
    public HashMap<Integer, Value> locals;
    public int blockDepth;

    public StackFrame(int funcId, int pc, Value[] args, List<Value> locals) {
        this.funcId = funcId;
        this.pc = pc;
        this.locals = new HashMap<>();
        // pre-initialize everything to 0
        for (var i = 0; i < locals.size(); i++) {
            var type = locals.get(i).getType() == null ? ValueType.I32 : locals.get(i).getType();
            this.setLocal(i, new Value(type, 0));
        }
        // set values from args
        for (var i = 0; i < args.length; i++) this.setLocal(i, args[i]);
        this.blockDepth = 0;
    }

    public void setLocal(int i, Value v) {
        this.locals.put(i, v);
    }

    public Value getLocal(int i) {
        return this.locals.get(i);
    }

    public String toString() {
        return "func=" +
                funcId +
                " " +
                "pc=" +
                pc +
                " " +
                "locals=" +
                Arrays.toString(locals.values().toArray());
    }
}