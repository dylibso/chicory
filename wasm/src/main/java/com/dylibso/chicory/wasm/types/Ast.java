package com.dylibso.chicory.wasm.types;

import java.util.ArrayDeque;

public class Ast {
    private final CodeBlock root;
    private final ArrayDeque<CodeBlock> stack;

    // private List<Instruction> instructions;

    public Ast() {
        this.root = new CodeBlock(BlockType.BLOCK);
        this.stack = new ArrayDeque<>();
        this.stack.push(root);
    }

    public CodeBlock root() {
        return root;
    }

    public void addInstruction(Instruction i) {
        var current = peek();
        switch (i.opcode()) {
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

    private void push(CodeBlock block) {
        stack.push(block);
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
        for (var i : block.instructions()) {
            sb.append("0x");
            sb.append(Integer.toHexString(i.address()));
            sb.append(" | ");
            sb.append("\t".repeat(depth));
            sb.append(i);
            sb.append("\n");

            if (i.codeBlock() != null) {
                printAst(sb, i.codeBlock(), depth + 1);
            }
        }
    }
}
