package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.OpCode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The purpose of this class is to provide a control structure to, in-line, label branches in a list of instructions.
 * <p>
 * Wasm has a structured form of control flow. There is no `jmp` to an address.
 * So instead, each branching point can only "jump" to a few points internal to the function
 * we are executing in. This algorithm mimics the control flow rules and annotates each
 * branching instruction with the labels (here the jumping point is the index of the instruction in the body)
 * that it can jump to. Branching instructions can only jump to the beginning of a block if this target is a loop,
 * or the end of a block for every other target block.
 * <p>
 * It's up to the Machine to decide, based on what is on the stack, which label to choose.
 * <p>
 * Here is an example on how to label some code. The left side contains indexes. Comments show where the labels go.
 * <pre>
 * 0     (local i32)
 * 1     (block
 * 2       (block
 * 3          (block
 *                 ;; x == 0
 * 4               local.get 0
 * 5               i32.eqz
 * 6               br_if 0 ;; true=14 false=7
 *
 *                 ;; x == 1
 * 7               local.get 0
 * 8               i32.const 1
 * 9               i32.eq
 * 10              br_if 1 ;; true=17 false=11
 *
 *                 ;; the `else` case
 * 11              i32.const 7
 * 12              local.set 1
 * 13              br 2    ;; true=19
 *            )
 * 14         i32.const 42
 * 15         local.set 1
 * 16         br 1         ;; true=19
 *            )
 * 17     i32.const 99
 * 18     local.set 1
 *        )
 * 19   local.get 1)
 * </pre>
 */
final class ControlTree {
    private final AnnotatedInstruction.Builder instruction;
    private final int initialInstructionNumber;
    private final ControlTree parent;
    private final List<ControlTree> nested;
    private final List<Consumer<Integer>> callbacks;

    public ControlTree() {
        this.instruction = null;
        this.initialInstructionNumber = 0;
        this.parent = null;
        this.nested = new ArrayList<>();
        this.callbacks = new ArrayList<>();
    }

    private ControlTree(
            int initialInstructionNumber,
            AnnotatedInstruction.Builder instruction,
            ControlTree parent) {
        this.instruction = instruction;
        this.initialInstructionNumber = initialInstructionNumber;
        this.parent = parent;
        this.nested = new ArrayList<>();
        this.callbacks = new ArrayList<>();
    }

    public ControlTree spawn(
            int initialInstructionNumber, AnnotatedInstruction.Builder instruction) {
        var node = new ControlTree(initialInstructionNumber, instruction, this);
        this.addNested(node);
        return node;
    }

    public AnnotatedInstruction.Builder instruction() {
        return instruction;
    }

    public int instructionNumber() {
        return initialInstructionNumber;
    }

    public void addNested(ControlTree nested) {
        this.nested.add(nested);
    }

    public ControlTree parent() {
        return parent;
    }

    public void addCallback(Consumer<Integer> callback) {
        this.callbacks.add(callback);
    }

    public void setFinalInstructionNumber(
            int finalInstructionNumber, AnnotatedInstruction.Builder end) {
        // to be set when END is reached
        if (end.scope().isPresent() && end.scope().get().opcode() == OpCode.LOOP) {
            var lastLoopInstruction = 0;
            if (this.parent != null) {
                for (var ct : this.parent.nested) {
                    if (ct.instruction().opcode() == OpCode.LOOP) {
                        lastLoopInstruction = ct.instructionNumber();
                    }
                }
            }
            finalInstructionNumber = lastLoopInstruction + 1;
        }

        for (var callback : this.callbacks) {
            callback.accept(finalInstructionNumber);
        }
    }
}
