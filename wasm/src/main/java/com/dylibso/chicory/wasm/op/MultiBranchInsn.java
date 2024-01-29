package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;
import java.util.Arrays;

/**
 * An instruction which targets some enclosing label.
 */
public final class MultiBranchInsn extends Insn<Op.MultiBranch> implements Cacheable {
    private final int[] branchTargets;
    private final int defaultTarget;

    private MultiBranchInsn(
            Op.MultiBranch op, int[] branchTargets, int defaultTarget, boolean ignored) {
        super(op, Arrays.hashCode(branchTargets) * 31 + defaultTarget);
        this.branchTargets = branchTargets.length == 0 ? branchTargets : branchTargets.clone();
        this.defaultTarget = defaultTarget;
    }

    MultiBranchInsn(Op.MultiBranch op, int[] branchTargets, int defaultTarget) {
        this(op, branchTargets.clone(), defaultTarget, true);
    }

    public int[] branchTargets() {
        return branchTargets.clone();
    }

    public int branchTargetCount() {
        return branchTargets.length;
    }

    public int branchTarget(int idx) {
        return branchTargets[idx];
    }

    public int defaultTarget() {
        return defaultTarget;
    }

    /**
     * {@return the computed switch density in the range <code>0.0 ≤ n ≤ 1.0</code>}
     * The density is calculated as {@code n / t} where {@code n} is the number of branch targets
     * that are not equal to the default target (not including the default target itself),
     * and {@code t} is the total number of branch targets (not including the default target).
     * If there are zero branch targets, {@code 1.0} is returned.
     * <p>
     * Instructions with a density above some threshold might perform better using a table-based strategy,
     * whereas instructions with a density below that threshold might perform better using a searching strategy.
     */
    public float density() {
        final int t = branchTargets.length;
        if (t == 0) {
            // treat it as perfect
            return 1.0f;
        }
        int n = 0;
        for (int branchTarget : branchTargets) {
            if (branchTarget == defaultTarget) {
                n++;
            }
        }
        return (float) n / (float) t;
    }

    @Override
    public boolean equals(Object obj) {
        return equals((MultiBranchInsn) obj);
    }

    public boolean equals(MultiBranchInsn other) {
        return this == other
                || super.equals(other)
                        && defaultTarget == other.defaultTarget
                        && Arrays.equals(branchTargets, other.branchTargets);
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.u31(branchTargets.length);
        for (int branchTarget : branchTargets) {
            out.u31(branchTarget);
        }
        out.u31(defaultTarget);
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb);
        for (int branchTarget : branchTargets) {
            sb.append(' ').append(branchTarget);
        }
        return sb.append(' ').append(defaultTarget);
    }
}
