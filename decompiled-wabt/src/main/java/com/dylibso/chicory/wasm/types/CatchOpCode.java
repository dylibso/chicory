package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public static final class Catch {
        private final CatchOpCode opcode;
        private final int tag;
        private final int label;
        private int resolvedLabel;

        private Catch(CatchOpCode opcode, int label) {
            this(opcode, -1, label);
            assert (opcode == CATCH_ALL || opcode == CATCH_ALL_REF);
        }

        private Catch(CatchOpCode opcode, int tag, int label) {
            assert (tag == -1 || opcode == CATCH || opcode == CATCH_REF);
            this.opcode = opcode;
            this.tag = tag;
            this.label = label;
        }

        public CatchOpCode opcode() {
            return opcode;
        }

        public int tag() {
            return tag;
        }

        public int label() {
            return label;
        }

        public void resolvedLabel(int label) {
            resolvedLabel = label;
        }

        public int resolvedLabel() {
            return resolvedLabel;
        }
    }

    @SuppressWarnings("ModifiedControlVariable")
    public static List<Catch> decode(long[] operands) {
        var length = operands[1];
        var result = new ArrayList<Catch>();
        for (int i = 2; i < operands.length; i++) {
            var catchEnum = CatchOpCode.byOpCode((int) operands[i++]);
            switch (catchEnum) {
                case CATCH:
                case CATCH_REF:
                    {
                        var tag = (int) operands[i++];
                        var label = (int) operands[i];
                        result.add(new Catch(catchEnum, tag, label));
                        break;
                    }
                case CATCH_ALL:
                case CATCH_ALL_REF:
                    {
                        var label = (int) operands[i];
                        result.add(new Catch(catchEnum, label));
                        break;
                    }
            }
        }
        assert (result.size() == length);
        return result;
    }

    public static List<Integer> allLabels(long[] operands) {
        return decode(operands).stream().map(Catch::label).collect(Collectors.toList());
    }
}
