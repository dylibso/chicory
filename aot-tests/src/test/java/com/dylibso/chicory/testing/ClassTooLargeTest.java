package com.dylibso.chicory.testing;

import static com.dylibso.chicory.corpus.WatGenerator.bigWat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.compiler.CompilerMachine;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class ClassTooLargeTest {

    @Test
    public void testFunc50k() {
        var funcCount = 50_000;
        byte[] wasm = Wat2Wasm.parse(bigWat(funcCount, 0));
        var instance =
                Instance.builder(Parser.parse(wasm))
                        .withMachineFactory(CompilerMachine::new)
                        .withStart(false)
                        .build();

        funcCount = 1000;
        var expected = 0;
        for (int i = 1; i <= funcCount; i++) {
            expected += i;
        }
        ExportFunction func1 = instance.export("func_" + funcCount);
        assertEquals(expected, func1.apply(0)[0]);
    }

    @Test
    public void testManyBigFuncs() {
        var funcCount = 10;
        byte[] wasm = Wat2Wasm.parse(bigWat(funcCount, 15_000));
        var instance =
                Instance.builder(Parser.parse(wasm))
                        .withMachineFactory(CompilerMachine::new)
                        .withStart(false)
                        .build();

        var expected = 0;
        for (int i = 1; i <= funcCount; i++) {
            expected += i;
        }
        ExportFunction func1 = instance.export("func_" + funcCount);
        assertEquals(expected, func1.apply(0)[0]);
    }
}
