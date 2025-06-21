package com.dylibso.chicory.runtime.internal.smap;

public abstract class LineMapping {

    abstract LineMapping withInputStartLine(long inputStartLine);

    abstract LineMapping withOutputStartLine(long outputStartLine);

    public abstract long inputStartLine();

    public abstract long outputStartLine();

    protected int lineFileID;
    protected int inputLineCount = 1;
    protected int outputLineCount = 1;

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
        if (outputLineCount() != 1) {
            out.append(",").append(outputLineCount());
        }
        out.append('\n');
        return out.toString();
    }

    LineMapping withInputLineCount(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("" + value);
        }
        this.inputLineCount = (int) value;
        return this;
    }

    LineMapping withOutputLineCount(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("" + value);
        }
        this.outputLineCount = (int) value;
        return this;
    }

    public int inputLineCount() {
        return inputLineCount;
    }

    public int outputLineCount() {
        return outputLineCount;
    }

    @Override
    public String toString() {
        return getString(true);
    }
}
