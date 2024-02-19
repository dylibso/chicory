package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.ControlTree;
import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CodeSection extends Section {
    private final ArrayList<FunctionBody> functionBodies;

    /**
     * Construct a new, empty section instance.
     */
    public CodeSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of functions to reserve space for
     */
    public CodeSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private CodeSection(ArrayList<FunctionBody> functionBodies) {
        super(SectionId.CODE);
        this.functionBodies = functionBodies;
    }

    public FunctionBody[] functionBodies() {
        return functionBodies.toArray(FunctionBody[]::new);
    }

    public int functionBodyCount() {
        return functionBodies.size();
    }

    public FunctionBody getFunctionBody(int idx) {
        return functionBodies.get(idx);
    }

    /**
     * Add a function body to this section.
     *
     * @param functionBody the function body to add to this section (must not be {@code null})
     * @return the index of the newly-added function body
     */
    public int addFunctionBody(FunctionBody functionBody) {
        Objects.requireNonNull(functionBody, "functionBody");
        int idx = functionBodies.size();
        functionBodies.add(functionBody);
        return idx;
    }

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var funcBodyCount = in.u31();
        functionBodies.ensureCapacity(functionBodies.size() + funcBodyCount);

        var root = new ControlTree();

        // Parse individual function bodies in the code section
        for (int i = 0; i < funcBodyCount; i++) {
            var blockScope = new ArrayDeque<Instruction>();
            var depth = 0;
            var funcEndPoint = in.u32Long() + in.position();
            var locals = parseCodeSectionLocalTypes(in);
            var instructions = new ArrayList<Instruction>();
            var lastInstruction = false;
            ControlTree currentControlFlow = null;

            do {
                var instruction = Instruction.readFrom(in);
                lastInstruction = in.position() >= funcEndPoint;
                if (instructions.isEmpty()) {
                    currentControlFlow = root.spawn(0, instruction);
                }

                // depth control
                switch (instruction.opcode()) {
                    case BLOCK:
                    case LOOP:
                    case IF:
                        {
                            instruction.setDepth(++depth);
                            blockScope.push(instruction);
                            instruction.setScope(blockScope.peek());
                            break;
                        }
                    case END:
                        {
                            instruction.setDepth(depth--);
                            instruction.setScope(
                                    blockScope.isEmpty() ? instruction : blockScope.pop());
                            break;
                        }
                    default:
                        {
                            instruction.setDepth(depth);
                            break;
                        }
                }

                // control-flow
                switch (instruction.opcode()) {
                    case BLOCK:
                    case LOOP:
                        {
                            currentControlFlow =
                                    currentControlFlow.spawn(instructions.size(), instruction);
                            break;
                        }
                    case IF:
                        {
                            currentControlFlow =
                                    currentControlFlow.spawn(instructions.size(), instruction);

                            var defaultJmp = instructions.size() + 1;
                            currentControlFlow.addCallback(
                                    end -> {
                                        // check that there is no "else" branch
                                        if (instruction.labelFalse() == defaultJmp) {
                                            instruction.setLabelFalse(end);
                                        }
                                    });

                            // defaults
                            instruction.setLabelTrue(defaultJmp);
                            instruction.setLabelFalse(defaultJmp);
                            break;
                        }
                    case ELSE:
                        {
                            assert (currentControlFlow.instruction().opcode() == OpCode.IF);
                            currentControlFlow.instruction().setLabelFalse(instructions.size() + 1);

                            currentControlFlow.addCallback(instruction::setLabelTrue);

                            break;
                        }
                    case BR_IF:
                        {
                            instruction.setLabelFalse(instructions.size() + 1);
                        }
                    case BR:
                        {
                            var offset = (int) instruction.operands()[0];
                            ControlTree reference = currentControlFlow;
                            while (offset > 0) {
                                reference = reference.parent();
                                offset--;
                            }
                            reference.addCallback(instruction::setLabelTrue);
                            break;
                        }
                    case BR_TABLE:
                        {
                            instruction.setLabelTable(new int[instruction.operands().length]);
                            for (var idx = 0; idx < instruction.labelTable().length; idx++) {
                                var offset = (int) instruction.operands()[idx];
                                ControlTree reference = currentControlFlow;
                                while (offset > 0) {
                                    reference = reference.parent();
                                    offset--;
                                }
                                int finalIdx = idx;
                                reference.addCallback(
                                        end -> instruction.labelTable()[finalIdx] = end);
                            }
                            break;
                        }
                    case END:
                        {
                            currentControlFlow.setFinalInstructionNumber(
                                    instructions.size(), instruction);
                            currentControlFlow = currentControlFlow.parent();

                            if (lastInstruction && instructions.size() > 1) {
                                var former = instructions.get(instructions.size() - 1);
                                if (former.opcode() == OpCode.END) {
                                    instruction.setScope(former.scope());
                                }
                            }
                            break;
                        }
                }

                instructions.add(instruction);

                // System.out.println(Integer.toHexString(instruction.getAddress()) + " " +
                // instruction);
            } while (!lastInstruction);

            addFunctionBody(new FunctionBody(locals, instructions));
        }
    }

    private static List<ValueType> parseCodeSectionLocalTypes(WasmInputStream in) {
        var distinctTypesCount = in.u31();
        var locals = new ArrayList<ValueType>();

        for (int i = 0; i < distinctTypesCount; i++) {
            var numberOfLocals = in.u31();
            var type = ValueType.forId(in.u8());
            for (int j = 0; j < numberOfLocals; j++) {
                locals.add(type);
            }
        }

        return locals;
    }
}
