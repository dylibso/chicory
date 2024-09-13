package com.dylibso.chicory.simd;

import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.types.OpCode;
import java.util.HashMap;
import java.util.Map;

public class Simd {

    public static Map<OpCode, Machine.OpImpl> opcodesImpl = new HashMap<>();

    static {
        // opcodesImpl.put(OpCode)
    }
}
