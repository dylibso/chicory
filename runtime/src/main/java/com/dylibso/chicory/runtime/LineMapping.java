package com.dylibso.chicory.runtime;

public class LineMapping {

    private final int lineFileID;
    private final long inputStartLine;
    private final long outputStartLine;
    private final int inputLineCount;
    private final int outputLineCount;

    public LineMapping(
            int lineFileID,
            long inputStartLine,
            long outputStartLine,
            int inputLineCount,
            int outputLineCount) {
        this.inputLineCount = inputLineCount;
        this.outputLineCount = outputLineCount;
        this.lineFileID = lineFileID;
        this.inputStartLine = inputStartLine;
        this.outputStartLine = outputStartLine;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.lineFileID = lineFileID;
        builder.inputStartLine = inputStartLine;
        builder.outputStartLine = outputStartLine;
        builder.inputLineCount = inputLineCount;
        builder.outputLineCount = outputLineCount;
        return builder;
    }

    public static final class Builder {
        int lineFileID;
        long inputStartLine = -1;
        long outputStartLine = -1;
        int inputLineCount = 1;
        int outputLineCount = 1;

        private Builder() {}

        public Builder withInputStartLine(long inputStartLine) {
            if (inputStartLine < 0) {
                throw new IllegalArgumentException("" + inputStartLine);
            }
            this.inputStartLine = inputStartLine;
            return this;
        }

        public Builder withOutputStartLine(long outputStartLine) {
            if (outputStartLine < 0) {
                throw new IllegalArgumentException("" + outputStartLine);
            }
            this.outputStartLine = outputStartLine;
            return this;
        }

        public Builder withLineFileID(int lineFileID) {
            if (lineFileID < 0) {
                throw new IllegalArgumentException("" + lineFileID);
            }
            this.lineFileID = lineFileID;
            return this;
        }

        public Builder withInputLineCount(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("" + value);
            }
            this.inputLineCount = (int) value;
            return this;
        }

        public Builder withOutputLineCount(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("" + value);
            }
            this.outputLineCount = (int) value;
            return this;
        }

        public LineMapping build() {
            return new LineMapping(
                    lineFileID, inputStartLine, outputStartLine, inputLineCount, outputLineCount);
        }

        public long inputStartLine() {
            return inputStartLine;
        }

        public long outputStartLine() {
            return outputStartLine;
        }

        public int lineFileID() {
            return lineFileID;
        }

        public int inputLineCount() {
            return inputLineCount;
        }

        public int outputLineCount() {
            return outputLineCount;
        }
    }

    public long inputStartLine() {
        return inputStartLine;
    }

    public long outputStartLine() {
        return outputStartLine;
    }

    public int lineFileID() {
        return lineFileID;
    }

    public int inputLineCount() {
        return inputLineCount;
    }

    public int outputLineCount() {
        return outputLineCount;
    }
}
