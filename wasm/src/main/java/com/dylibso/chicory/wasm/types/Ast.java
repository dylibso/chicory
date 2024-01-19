package com.dylibso.chicory.wasm.types;

import java.util.Stack;

public class Ast {
    private final CodeBlock root;
    private final Stack<CodeBlock> stack;

    // private List<Instruction> instructions;

    public Ast() {
        this.root = new CodeBlock(BlockType.BLOCK);
        this.stack = new Stack<>();
        this.stack.push(root);
    }

    public CodeBlock getRoot() {
        return root;
    }

    public void addInstruction(Instruction i) {
        var current = peek();
        switch (i.getOpcode()) {
            case BLOCK:
                {
                    current.addInstruction(i);
                    var next = new CodeBlock(BlockType.BLOCK);
                    i.setCodeBlock(next);
                    push(next);
                    break;
                }
            case LOOP:
                {
                    current.addInstruction(i);
                    var next = new CodeBlock(BlockType.LOOP);
                    i.setCodeBlock(next);
                    push(next);
                    break;
                }
            case END:
                {
                    current.addInstruction(i);
                    pop();
                    break;
                }
            default:
                current.addInstruction(i);
        }
    }

    private CodeBlock push(CodeBlock block) {
        return this.stack.push(block);
    }

    private CodeBlock peek() {
        return this.stack.peek();
    }

    private void pop() {
        this.stack.pop();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        printAst(sb, root, 0);
        return sb.toString();
    }

    private void printAst(StringBuilder sb, CodeBlock block, int depth) {
        for (var i : block.getInstructions()) {
            sb.append("0x");
            sb.append(Integer.toHexString(i.getAddress()));
            sb.append(" | ");
            sb.append("\t".repeat(depth));
            sb.append(i);
            sb.append("\n");

            if (i.getCodeBlock() != null) {
                printAst(sb, i.getCodeBlock(), depth + 1);
            }
        }
    }
}
