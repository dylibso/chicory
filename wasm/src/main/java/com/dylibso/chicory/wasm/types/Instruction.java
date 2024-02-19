package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Instruction {
    private int address;
    private OpCode opcode;
    private long[] operands;
    private CodeBlock block;

    // metadata fields
    private Integer labelTrue;
    private Integer labelFalse;
    private int[] labelTable;
    private int depth;
    private Instruction scope;

    public Instruction(int address, OpCode opcode, long[] operands) {
        this.address = address;
        this.opcode = opcode;
        this.operands = operands;
    }

    public OpCode opcode() {
        return opcode;
    }

    public long[] operands() {
        return operands;
    }

    public void setCodeBlock(CodeBlock block) {
        this.block = block;
    }

    public CodeBlock codeBlock() {
        return block;
    }

    public String toString() {
        var result = String.format("0x%08X", address) + ": ";
        if (operands.length > 0) {
            return result + opcode + " " + Arrays.toString(operands);
        }
        return result + opcode.toString();
    }

    public int address() {
        return address;
    }

    public Integer labelTrue() {
        return labelTrue;
    }

    public void setLabelTrue(Integer labelTrue) {
        this.labelTrue = labelTrue;
    }

    public Integer labelFalse() {
        return labelFalse;
    }

    public void setLabelFalse(Integer labelFalse) {
        this.labelFalse = labelFalse;
    }

    public int[] labelTable() {
        return labelTable;
    }

    public void setLabelTable(int[] labelTable) {
        this.labelTable = labelTable;
    }

    public int depth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public Instruction scope() {
        return scope;
    }

    public void setScope(Instruction scope) {
        this.scope = scope;
    }

    public static Instruction readFrom(WasmInputStream in) throws WasmIOException {
        var address = in.position();
        var b = in.rawByte();
        if (b == 0xfc) { // is multi-byte
            b = (0xfc << 8) + in.u8();
        }
        var op = OpCode.byOpCode(b);
        if (op == null) {
            throw new IllegalArgumentException("Can't find opcode for op value " + b);
        }
        // System.out.println("b: " + b + " op: " + op);
        var signature = OpCode.getSignature(op);
        if (signature.length == 0) {
            return new Instruction((int) address, op, new long[] {});
        }
        var operands = new ArrayList<Long>();
        for (var sig : signature) {
            switch (sig) {
                case VARUINT:
                    operands.add(Long.valueOf(in.u32Long()));
                    break;
                case VARSINT32:
                    operands.add(Long.valueOf(in.s32()));
                    break;
                case VARSINT64:
                    operands.add(Long.valueOf(in.s64()));
                    break;
                case FLOAT64:
                    operands.add(Long.valueOf(Double.doubleToRawLongBits(in.f64())));
                    break;
                case FLOAT32:
                    operands.add(Long.valueOf(Float.floatToRawIntBits(in.f32())));
                    break;
                case VEC_VARUINT:
                    {
                        var vcount = in.u31();
                        for (var j = 0; j < vcount; j++) {
                            operands.add(Long.valueOf(in.u32Long()));
                        }
                        break;
                    }
            }
        }
        var operandsArray = new long[operands.size()];
        for (var i = 0; i < operands.size(); i++) operandsArray[i] = operands.get(i);
        return new Instruction((int) address, op, operandsArray);
    }
}
