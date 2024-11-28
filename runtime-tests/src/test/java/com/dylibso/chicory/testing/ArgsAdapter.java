package com.dylibso.chicory.testing;

import java.util.ArrayDeque;

public final class ArgsAdapter {
    private final ArrayDeque<Long> stack;

    private ArgsAdapter() {
        stack = new ArrayDeque<>();
    }

    public static ArgsAdapter builder() {
        return new ArgsAdapter();
    }

    public long[] build() {
        var result = new long[stack.size()];
        int i = stack.size() - 1;
        while (!stack.isEmpty()) {
            result[i--] = stack.pop();
        }
        return result;
    }

    public ArgsAdapter add(long[] args) {
        for (var arg : args) {
            stack.push(arg);
        }
        return this;
    }

    public ArgsAdapter add(long arg) {
        stack.push(arg);
        return this;
    }
}
