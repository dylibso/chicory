package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import java.util.List;
import java.util.function.Function;

/**
 * The purpose of this class is to label branches in a list of instructions.
 * Wasm has a structured form of control flow. There is no `jmp` to an address.
 * So instead each branching point can only "jump" to a few points internal to the function
 * we are executing in. This algorithm mimics the control flow rules and annotates each
 * branching instruction with the labels (here the jumping point is the index of the instruction in the body)
 * that it can jump to. Branching instructions can only jump to the beginning of a block if this target is a loop,
 * or the end of a block for every other target block.
 * It's up to the Machine to decide, based on what is on the stack, which label to choose.
 * Here is an example on how to label some code. The left side contains indexes, comments show where the labels go
 *
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
 *
 * This is very sub-optimal at the moment
 */
public class ControlFlow {
    public static void labelBranches(List<Instruction> instructions) {
        Function<Integer, Integer> clamp = (i) -> Math.min(i, instructions.size());

        for (int i = 0; i < instructions.size(); i++) {
            var instr = instructions.get(i);
            switch (instr.getOpcode()) {
                case IF:
                    {
                        instr.setLabelTrue(clamp.apply(i + 1));
                        // find the matching ELSE (which is optional)
                        var next =
                                findNext(
                                        instructions,
                                        OpCode.ELSE,
                                        instr.getDepth(),
                                        clamp.apply(i + 1));
                        if (next != null) {
                            // point to instruction after the ELSE
                            instr.setLabelFalse(clamp.apply(next + 1));
                        } else {
                            var end =
                                    findNext(
                                            instructions,
                                            OpCode.END,
                                            instr.getDepth(),
                                            clamp.apply(i + 1));
                            instr.setLabelFalse(clamp.apply(end + 1));
                        }
                        break;
                    }
                case ELSE:
                    {
                        var end =
                                findNext(
                                        instructions,
                                        OpCode.END,
                                        instr.getDepth(),
                                        clamp.apply(i + 1));
                        instr.setLabelTrue(clamp.apply(end + 1));
                        break;
                    }
                case BR:
                    {
                        var d = (int) instr.getOperands()[0];
                        var end =
                                findNext(
                                        instructions,
                                        OpCode.END,
                                        instr.getDepth() - d,
                                        clamp.apply(i + 1));
                        var endI = instructions.get(end);
                        if (endI.getScope() == OpCode.LOOP) {
                            var loop = findLast(instructions, OpCode.LOOP, instr.getDepth() - d, i);
                            instr.setLabelTrue(loop + 1);
                        } else {
                            instr.setLabelTrue(clamp.apply(end + 1));
                        }
                        break;
                    }
                case BR_IF:
                    {
                        instr.setLabelFalse(clamp.apply(i + 1));
                        var d = (int) instr.getOperands()[0];
                        var end =
                                findNext(
                                        instructions,
                                        OpCode.END,
                                        instr.getDepth() - d,
                                        clamp.apply(i + 1));
                        var endI = instructions.get(end);
                        if (endI.getScope() == OpCode.LOOP) {
                            var loop = findLast(instructions, OpCode.LOOP, instr.getDepth() - d, i);
                            instr.setLabelTrue(loop + 1);
                        } else {
                            instr.setLabelTrue(clamp.apply(end + 1));
                        }
                        break;
                    }
                case BR_TABLE:
                    {
                        instr.setLabelTable(new int[instr.getOperands().length]);
                        for (var idx = 0; idx < instr.getLabelTable().length; idx++) {
                            var d = (int) instr.getOperands()[idx];
                            var end =
                                    findNext(
                                            instructions,
                                            OpCode.END,
                                            instr.getDepth() - d,
                                            clamp.apply(i + 1));
                            var endI = instructions.get(end);
                            if (endI.getScope() == OpCode.LOOP) {
                                var loop =
                                        findLast(
                                                instructions, OpCode.LOOP, instr.getDepth() - d, i);
                                instr.getLabelTable()[idx] = loop + 1;
                            } else {
                                instr.getLabelTable()[idx] = clamp.apply(end + 1);
                            }
                        }
                        break;
                    }
            }
        }
    }

    /**
     * Finds the next instruction matching the given details. This is pretty sub-optimal to scan this way.
     * We could quickly speed this up by keeping an index of ENDs that we can scan instead of scanning every
     * instruction.
     */
    static Integer findNext(List<Instruction> instructions, OpCode opcode, int depth, int start) {
        for (var i = start; i < instructions.size(); i++) {
            var instr = instructions.get(i);
            if (instr.getOpcode() == opcode && instr.getDepth() == depth) {
                return i;
            }
        }
        return null;
    }

    /**
     * Finds the last instruction matching the given details. This is pretty sub-optimal to scan this way.
     * We could quickly speed this up by keeping an index of LOOPs that we can scan instead of scanning every
     * instruction.
     */
    static Integer findLast(List<Instruction> instructions, OpCode opcode, int depth, int start) {
        for (var i = start; i >= 0; i--) {
            var instr = instructions.get(i);
            if (instr.getOpcode() == opcode && instr.getDepth() == depth) {
                return i;
            }
        }
        return null;
    }
}
