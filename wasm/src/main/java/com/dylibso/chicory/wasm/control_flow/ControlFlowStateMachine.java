package com.dylibso.chicory.wasm.control_flow;

import com.dylibso.chicory.wasm.types.Instruction;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ControlFlowStateMachine {

    private Stack<IfControlFlow> ifControlFlows = new Stack<>();
    private Map<Integer, Stack<Endable>> BRcfs = new HashMap<>();
    private int instructionCounter = 0;

    public ControlFlowStateMachine() {}

    public void process(Instruction instruction) {
        switch (instruction.getOpcode()) {
            case IF:
                {
                    ifControlFlows.push(new IfControlFlow(instruction, instructionCounter));
                    break;
                }
            case ELSE:
                {
                    if (!ifControlFlows.empty()) {
                        ifControlFlows.peek().onElse(instruction, instructionCounter);
                    } else {
                        // this should not happen ...
                    }
                    break;
                }
            case END:
                {
                    if (!ifControlFlows.empty()) {
                        ifControlFlows.pop().onEnd(instruction, instructionCounter);
                    } else {
                        if (BRcfs.containsKey(instruction.getDepth())) {
                            BRcfs.get(instruction.getDepth())
                                    .forEach(
                                            e -> {
                                                e.onEnd(instruction, instructionCounter);
                                            });
                        }
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
