package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DummyTest {

    Instance instance0 = Module.build("target/compiled-wast/f32_bitwise/spec.0.wasm").instantiate();
    @Test()
    public void test104() {
        ExportFunction varCopysign = instance0.getExport("copysign");
        assertEquals(Float.intBitsToFloat(Integer.parseUnsignedInt("2155872256")), varCopysign.apply(Value.f32(Integer.parseUnsignedInt("2155872256")), Value.f32(Integer.parseUnsignedInt("4290772992"))).asFloat(), 0.0);
    }

    @Test()
    public void test105() {
        ExportFunction varCopysign = instance0.getExport("copysign");
        assertEquals(Float.intBitsToFloat(Integer.parseUnsignedInt("8388608")), varCopysign.apply(Value.f32(Integer.parseUnsignedInt("2155872256")), Value.f32(Integer.parseUnsignedInt("2143289344"))).asFloat(), 0.0);
    }

}
