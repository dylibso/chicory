package com.dylibso.chicory.wasm.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.corpus.CorpusResources;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class ValTypeTest {
    private FunctionType context(int idx) {
        return FunctionType.returning(ValType.FuncRef);
    }

    @Test
    public void roundtrip() {
        var cases =
                new ValType[] {
                    ValType.F64,
                    ValType.F32,
                    ValType.I64,
                    ValType.I32,
                    ValType.V128,
                    ValType.FuncRef,
                    ValType.ExternRef,
                    ValType.builder()
                            .withOpcode(ValType.ID.RefNull)
                            .withTypeIdx(ValType.TypeIdxCode.FUNC.code())
                            .build(this::context),
                    ValType.builder()
                            .withOpcode(ValType.ID.Ref)
                            .withTypeIdx(ValType.TypeIdxCode.EXTERN.code())
                            .build(this::context),
                    ValType.builder()
                            .withOpcode(ValType.ID.Ref)
                            .withTypeIdx(16)
                            .build(this::context),
                };

        for (var vt : cases) {
            long id = vt.id();
            ValType roundTrip = ValType.builder().fromId(id).build(this::context);
            assert vt.equals(roundTrip) : "Failed to roundtrip: " + vt;
        }
    }

    @Test
    public void checkExternRef() {
        var module =
                Parser.parse(CorpusResources.getResource("compiled/externref-example.wat.wasm"));

        assertEquals(3, module.typeSection().types().length);

        var type0 = module.typeSection().types()[0].returns().get(0);
        assertEquals(ValType.TypeIdxCode.EXTERN.code(), type0.typeIdx());
    }
}
