package com.dylibso.chicory.runtime.internal.smap;

public abstract class LineMapping {

    abstract LineMapping withInputStartLine(long inputStartLine);

    abstract LineMapping withOutputStartLine(long outputStartLine);

    abstract LineMapping withInputLineCount(long inputLineCount);

    abstract LineMapping withOutputLineIncrement(long outputLineIncrement);

    public abstract long inputStartLine();

    public abstract long outputStartLine();

    public abstract long inputLineCount();

    public abstract long outputLineIncrement();

    protected int lineFileID;

    public int lineFileID() {
        return lineFileID;
    }

    void withLineFileID(int lineFileID) {
        if (lineFileID < 0) {
            throw new IllegalArgumentException("" + lineFileID);
        }
        this.lineFileID = lineFileID;
    }

    public String getString(boolean includeFileID) {
        if (inputStartLine() == -1 || outputStartLine() == -1) {
            throw new IllegalStateException();
        }
        StringBuilder out = new StringBuilder();
        out.append(inputStartLine());
        if (includeFileID) {
            out.append("#").append(lineFileID);
        }
        if (inputLineCount() != 1) {
            out.append(",").append(inputLineCount());
        }
        out.append(":").append(outputStartLine());
        if (outputLineIncrement() != 1) {
            out.append(",").append(outputLineIncrement());
        }
        out.append('\n');
        return out.toString();
    }

    @Override
    public String toString() {
        return getString(true);
    }
}
