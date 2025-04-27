package com.dylibso.chicory.testing;

import static com.dylibso.chicory.corpus.WatGenerator.bigWat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.experimental.aot.AotMachine;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class MethodTooLargeTest {

    @Test
    public void testBigFunc() {
        byte[] wasm = Wat2Wasm.parse(bigWat(1, 20_000));
        var instance =
                Instance.builder(Parser.parse(wasm))
                        .withMachineFactory(AotMachine::new)
                        .withStart(false)
                        .build();

        ExportFunction func1 = instance.export("func_" + 1);
        assertEquals(1, func1.apply(0)[0]);
    }
}
