package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ControlFlowStateMachine {

    private Map<Integer, Stack<IfControlFlow>> cfs = new HashMap<>();
    // check at the end if we can squash the two:
    private Map<Integer, Stack<Endable>> BRcfs = new HashMap<>();
    private int instructionCounter = 0;

    public ControlFlowStateMachine() {}

    public void process(Instruction instruction) {
        switch (instruction.getOpcode()) {
            case IF:
                {
                    cfs.compute(
                            instruction.getDepth(),
                            (k, v) -> {
                                var stack = (v == null) ? new Stack<IfControlFlow>() : v;
                                stack.push(new IfControlFlow(instruction, instructionCounter));
                                return stack;
                            });
                    break;
                }
            case ELSE:
                {
                    cfs.get(instruction.getDepth()).peek().onElse(instruction, instructionCounter);
                    break;
                }
            case END:
                {
                    if (cfs.containsKey(instruction.getDepth())) {
                        cfs.get(instruction.getDepth())
                                .pop()
                                .onEnd(instruction, instructionCounter);
                    }

                    if (BRcfs.containsKey(instruction.getDepth())) {
                        BRcfs.get(instruction.getDepth())
                                .forEach(
                                        e -> {
                                            e.onEnd(instruction, instructionCounter);
                                        });
                    }
                    break;
                }
            case BR:
            case BR_IF:
                {
                    var cf = new BrControlFlow(instruction, instructionCounter);
                    BRcfs.compute(
                            instruction.getDepth() - cf.getOffset(),
                            (k, v) -> {
                                var stack = (v == null) ? new Stack<Endable>() : v;
                                stack.push(cf);
                                return stack;
                            });
                    break;
                }
            case BR_TABLE:
                {
                    instruction.setLabelTable(new int[instruction.getOperands().length]);
                    for (var idx = 0; idx < instruction.getLabelTable().length; idx++) {
                        var cf = new BrTableControlFlow(instruction, instructionCounter, idx);
                        BRcfs.compute(
                                instruction.getDepth() - cf.getOffset(),
                                (k, v) -> {
                                    var stack = (v == null) ? new Stack<Endable>() : v;
                                    stack.push(cf);
                                    return stack;
                                });
                    }
                    break;
                }
            default:
                {
                    break;
                }
        }

        instructionCounter++;
    }

    public void end() {
        //        if (!cfs.isEmpty()) {
        //            throw new RuntimeException("There are control flow instructions that are not
        // matched.");
        //        }
    }
}
