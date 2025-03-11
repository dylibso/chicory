package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public enum CatchOpCode {
    CATCH(0x00),
    CATCH_REF(0x01),
    CATCH_ALL(0x02),
    CATCH_ALL_REF(0x03);

    private static final int OP_CODES_SIZE = 4;

    // trick: the enum constructor cannot access its own static fields
    // but can access another class
    private static final class CatchOpCodes {
        private CatchOpCodes() {}

        private static final CatchOpCode[] byOpCode = new CatchOpCode[OP_CODES_SIZE];
    }

    private final int opcode;

    CatchOpCode(int opcode) {
        this.opcode = opcode;
        CatchOpCodes.byOpCode[opcode] = this;
    }

    public int opcode() {
        return opcode;
    }

    public static CatchOpCode byOpCode(int opcode) {
        return CatchOpCodes.byOpCode[opcode];
    }

    // Optional: found/not found
    // value: the index to look up in the labelTable
    public static Optional<Integer> catchLabelIdx(int currentTag, long[] operands) {
        var n = operands[1];
        var idx = 0;
        for (int i = 2; i < operands.length; i++) {
            var catchEnum = CatchOpCode.byOpCode((int) operands[i++]);
            switch (catchEnum) {
                case CATCH:
                case CATCH_REF:
                    var tag = (int) operands[i++];
                    if (currentTag == tag) {
                        return Optional.of(idx);
                    }
                    break;
                case CATCH_ALL:
                case CATCH_ALL_REF:
                    return Optional.of(idx);
            }
            idx++;
        }
        return Optional.empty();
    }

    public static Optional<Integer> catchLabelValue(int currentTag, long[] operands) {
        var n = operands[1];
        for (int i = 2; i < operands.length; i++) {
            var catchEnum = CatchOpCode.byOpCode((int) operands[i++]);
            switch (catchEnum) {
                case CATCH:
                case CATCH_REF:
                    var tag = (int) operands[i++];
                    if (currentTag == tag) {
                        return Optional.of((int) operands[i]);
                    }
                    break;
                case CATCH_ALL:
                case CATCH_ALL_REF:
                    return Optional.of((int) operands[i]);
            }
        }
        return Optional.empty();
    }

    public static Optional<CatchOpCode> catchOpCode(int currentTag, long[] operands) {
        var n = operands[1];
        for (int i = 2; i < operands.length; i++) {
            var catchEnum = CatchOpCode.byOpCode((int) operands[i++]);
            switch (catchEnum) {
                case CATCH:
                case CATCH_REF:
                    var tag = (int) operands[i++];
                    if (currentTag == tag) {
                        return Optional.of(catchEnum);
                    }
                    break;
                case CATCH_ALL:
                case CATCH_ALL_REF:
                    return Optional.of(catchEnum);
            }
        }
        return Optional.empty();
    }

    public static List<Integer> allLabels(long[] operands) {
        var result = new ArrayList<Integer>();
        var n = operands[1];
        for (int i = 2; i < operands.length; i++) {
            if (result.size() == n) {
                return result;
            }
            var catchEnum = CatchOpCode.byOpCode((int) operands[i++]);
            switch (catchEnum) {
                case CATCH:
                case CATCH_REF:
                    i++; // skip tag
                case CATCH_ALL:
                case CATCH_ALL_REF:
                    result.add((int) operands[i]);
            }
        }
        assert (result.size() == n);
        return result;
    }
}
