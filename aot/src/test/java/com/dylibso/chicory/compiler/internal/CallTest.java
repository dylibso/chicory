package com.dylibso.chicory.compiler.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.dylibso.chicory.experimental.aot.AotMachine;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class CallTest {

    @Test
    public void callLotsOfArgs() throws InterruptedException {
        var module =
                Parser.parse(CallTest.class.getResourceAsStream("/compiled/lots-of-args.wat.wasm"));
        var instance = Instance.builder(module).withMachineFactory(AotMachine::new).build();

        var function = instance.export("test");
        long[] result = function.apply(2, 3);
        assertArrayEquals(new long[] {5}, result);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void callLotsOfArgsOnDeprecatedAotMachine() throws InterruptedException {
        var module =
                Parser.parse(CallTest.class.getResourceAsStream("/compiled/lots-of-args.wat.wasm"));
        var instance = Instance.builder(module).withMachineFactory(AotMachine::new).build();

        var function = instance.export("test");
        long[] result = function.apply(2, 3);
        assertArrayEquals(new long[] {5}, result);
    }
}
