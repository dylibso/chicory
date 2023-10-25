package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ControlTree {
    private Instruction instruction;
    private int finalInstructionNumber = -1; // to be set when END is reached
    private final ControlTree parent;
    private final List<ControlTree> nested;
    private final List<Consumer<Integer>> callbacks;

    public ControlTree() {
        this.instruction = null;
        this.parent = null;
        this.nested = new ArrayList<>();
        this.callbacks = new ArrayList<>();
    }

    private ControlTree(Instruction instruction, ControlTree parent) {
        this.instruction = instruction;
        this.parent = parent;
        this.nested = new ArrayList<>();
        this.callbacks = new ArrayList<>();
    }

    public ControlTree spawn(Instruction instruction) {
        var node = new ControlTree(instruction, this);
        this.addNested(node);
        return node;
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    public void addNested(ControlTree nested) {
        this.nested.add(nested);
    }

    public ControlTree getParent() {
        return parent;
    }

    public void addCallback(Consumer<Integer> callback) {
        this.callbacks.add(callback);
    }

    public void setFinalInstructionNumber(int finalInstructionNumber) {
        this.finalInstructionNumber = finalInstructionNumber;
    }

    public void triggerCallbacks() {
        if (finalInstructionNumber == -1) {
             throw new RuntimeException("Callbacks should be triggered after everything have been computed");
        } else {
            for (var callback : this.callbacks) {
                callback.accept(finalInstructionNumber);
            }
            for (var child : this.nested) {
                child.triggerCallbacks();
            }
        }
    }
}
