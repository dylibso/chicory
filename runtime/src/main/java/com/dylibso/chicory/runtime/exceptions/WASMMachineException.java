package com.dylibso.chicory.runtime.exceptions;

import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import java.util.List;

public class WASMMachineException extends ChicoryException {
    private final List<StackFrame> frames;

    public WASMMachineException(List<StackFrame> frames, String msg) {
        super(msg);

        this.frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public WASMMachineException(List<StackFrame> frames, Throwable cause) {
        super(cause);

        this.frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public WASMMachineException(List<StackFrame> frames, String msg, Throwable cause) {
        super(msg, cause);

        this.frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public List<StackFrame> getStackFrames() {
        return frames;
    }
}
