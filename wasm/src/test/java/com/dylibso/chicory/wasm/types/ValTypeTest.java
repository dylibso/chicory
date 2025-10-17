package com.dylibso.chicory.wasm.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.wasm.Parser;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ValTypeTest {
    private RecType context(int idx) {
        return RecType.builder()
                .withSubTypeBuilders(
                        new SubType.Builder[] {
                            SubType.builder()
                                    .withFinal(true)
                                    .withTypeIdx(new int[0])
                                    .withCompTypeBuilder(
                                            CompType.builder()
                                                    .withFuncType(
                                                            FunctionType.builder()
                                                                    .withParams(List.of())
                                                                    .withReturns(
                                                                            List.of(
                                                                                    ValType
                                                                                            .builder()
                                                                                            .withOpcode(
                                                                                                    ValType
                                                                                                            .ID
                                                                                                            .FuncRef)))))
                        })
                .build(i -> null);
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
                Parser.parse(
                        ValTypeTest.class.getResourceAsStream(
                                "/compiled/externref-example.wat.wasm"));

        assertEquals(3, module.typeSection().types().length);

        var type0 =
                module.typeSection()
                        .types()[0]
                        .subTypes()[0]
                        .compType()
                        .funcType()
                        .returns()
                        .get(0);
        assertEquals(ValType.TypeIdxCode.EXTERN.code(), type0.typeIdx());
    }
}
