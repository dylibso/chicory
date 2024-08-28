package com.dylibso.chicory.aot.wabt;

import com.dylibso.chicory.gen.CompiledModule;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Value;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Wast2JsonMachine implements Machine {
    private Instance instance;

    public Wast2JsonMachine(Instance instance) {
        this.instance = instance;
    }

    private static Map<Integer, BiFunction<Instance, Value[], Value[]>> funcs;

    static {
        funcs = new HashMap<>();
        funcs.put(
                0,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_0(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                2,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_2(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                3,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_3(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                4,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_4(args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                5,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_5(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                6,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_6(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                7,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_7(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                8,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_8(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                9,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_9(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                10,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_10(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                11,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_11(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                12,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_12(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                13,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_13(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        args[7].asInt(),
                                        args[8].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                14,
                (instance, args) -> {
                    CompiledModule.func_14(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                15,
                (instance, args) -> {
                    CompiledModule.func_15(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                16,
                (instance, args) -> {
                    CompiledModule.func_16(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                17,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_17(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                18,
                (instance, args) -> {
                    CompiledModule.func_18(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                19,
                (instance, args) -> {
                    CompiledModule.func_19(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                20,
                (instance, args) -> {
                    CompiledModule.func_20(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                21,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_21(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                22,
                (instance, args) -> {
                    CompiledModule.func_22(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                23,
                (instance, args) -> {
                    CompiledModule.func_23(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                24,
                (instance, args) -> {
                    CompiledModule.func_24(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                25,
                (instance, args) -> {
                    CompiledModule.func_25(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                26,
                (instance, args) -> {
                    CompiledModule.func_26(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                27,
                (instance, args) -> {
                    CompiledModule.func_27(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                28,
                (instance, args) -> {
                    CompiledModule.func_28(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                29,
                (instance, args) -> {
                    CompiledModule.func_29(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                30,
                (instance, args) -> {
                    CompiledModule.func_30(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                31,
                (instance, args) -> {
                    CompiledModule.func_31(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                32,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_32(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                33,
                (instance, args) -> {
                    CompiledModule.func_33(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                34,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_34(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                35,
                (instance, args) -> {
                    CompiledModule.func_35(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                36,
                (instance, args) -> {
                    CompiledModule.func_36(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                37,
                (instance, args) -> {
                    CompiledModule.func_37(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                38,
                (instance, args) -> {
                    CompiledModule.func_38(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                39,
                (instance, args) -> {
                    CompiledModule.func_39(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                40,
                (instance, args) -> {
                    CompiledModule.func_40(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                41,
                (instance, args) -> {
                    CompiledModule.func_41(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                42,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_42(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                43,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_43(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                44,
                (instance, args) -> {
                    CompiledModule.func_44(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                45,
                (instance, args) -> {
                    CompiledModule.func_45(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                46,
                (instance, args) -> {
                    CompiledModule.func_46(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                47,
                (instance, args) -> {
                    CompiledModule.func_47(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                48,
                (instance, args) -> {
                    CompiledModule.func_48(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                49,
                (instance, args) -> {
                    CompiledModule.func_49(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                50,
                (instance, args) -> {
                    CompiledModule.func_50(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                51,
                (instance, args) -> {
                    CompiledModule.func_51(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                52,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_52(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                53,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_53(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                54,
                (instance, args) -> {
                    CompiledModule.func_54(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                55,
                (instance, args) -> {
                    CompiledModule.func_55(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                56,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_56(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                57,
                (instance, args) -> {
                    CompiledModule.func_57(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                58,
                (instance, args) -> {
                    CompiledModule.func_58(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                59,
                (instance, args) -> {
                    CompiledModule.func_59(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                60,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_60(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                61,
                (instance, args) -> {
                    CompiledModule.func_61(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                62,
                (instance, args) -> {
                    CompiledModule.func_62(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                63,
                (instance, args) -> {
                    CompiledModule.func_63(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                64,
                (instance, args) -> {
                    CompiledModule.func_64(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                65,
                (instance, args) -> {
                    CompiledModule.func_65(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                66,
                (instance, args) -> {
                    CompiledModule.func_66(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                67,
                (instance, args) -> {
                    CompiledModule.func_67(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                68,
                (instance, args) -> {
                    CompiledModule.func_68(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                69,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_69(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                70,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_70(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                71,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_71(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                72,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_72(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                73,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_73(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                74,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_74(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                75,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_75(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                76,
                (instance, args) -> {
                    CompiledModule.func_76(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                77,
                (instance, args) -> {
                    CompiledModule.func_77(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                78,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_78(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                79,
                (instance, args) -> {
                    CompiledModule.func_79(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            args[6].asInt(),
                            args[7].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                80,
                (instance, args) -> {
                    CompiledModule.func_80(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            args[6].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                81,
                (instance, args) -> {
                    CompiledModule.func_81(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                82,
                (instance, args) -> {
                    CompiledModule.func_82(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                83,
                (instance, args) -> {
                    CompiledModule.func_83(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                84,
                (instance, args) -> {
                    CompiledModule.func_84(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                85,
                (instance, args) -> {
                    CompiledModule.func_85(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                86,
                (instance, args) -> {
                    CompiledModule.func_86(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                87,
                (instance, args) -> {
                    CompiledModule.func_87(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                88,
                (instance, args) -> {
                    CompiledModule.func_88(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                89,
                (instance, args) -> {
                    CompiledModule.func_89(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                90,
                (instance, args) -> {
                    CompiledModule.func_90(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                91,
                (instance, args) -> {
                    CompiledModule.func_91(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                92,
                (instance, args) -> {
                    CompiledModule.func_92(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                93,
                (instance, args) -> {
                    CompiledModule.func_93(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                94,
                (instance, args) -> {
                    CompiledModule.func_94(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                95,
                (instance, args) -> {
                    CompiledModule.func_95(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                96,
                (instance, args) -> {
                    CompiledModule.func_96(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                97,
                (instance, args) -> {
                    CompiledModule.func_97(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                98,
                (instance, args) -> {
                    CompiledModule.func_98(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                99,
                (instance, args) -> {
                    CompiledModule.func_99(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                100,
                (instance, args) -> {
                    CompiledModule.func_100(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                101,
                (instance, args) -> {
                    CompiledModule.func_101(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                102,
                (instance, args) -> {
                    CompiledModule.func_102(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                103,
                (instance, args) -> {
                    CompiledModule.func_103(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                104,
                (instance, args) -> {
                    CompiledModule.func_104(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                105,
                (instance, args) -> {
                    CompiledModule.func_105(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                106,
                (instance, args) -> {
                    CompiledModule.func_106(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                107,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_107(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                108,
                (instance, args) -> {
                    CompiledModule.func_108(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                109,
                (instance, args) -> {
                    CompiledModule.func_109(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                110,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_110(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                111,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_111(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                112,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_112(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                113,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_113(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                114,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_114(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                115,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_115(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                116,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_116(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                117,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_117(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                118,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_118(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                119,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_119(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                120,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_120(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                121,
                (instance, args) -> {
                    CompiledModule.func_121(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                122,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_122(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                123,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_123(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                124,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_124(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                125,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_125(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                126,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_126(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                127,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_127(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                128,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_128(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                129,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_129(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                130,
                (instance, args) -> {
                    CompiledModule.func_130(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                131,
                (instance, args) -> {
                    CompiledModule.func_131(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                132,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_132(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                133,
                (instance, args) -> {
                    CompiledModule.func_133(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                134,
                (instance, args) -> {
                    CompiledModule.func_134(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                135,
                (instance, args) -> {
                    CompiledModule.func_135(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                136,
                (instance, args) -> {
                    CompiledModule.func_136(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                137,
                (instance, args) -> {
                    CompiledModule.func_137(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                138,
                (instance, args) -> {
                    CompiledModule.func_138(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                139,
                (instance, args) -> {
                    CompiledModule.func_139(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                140,
                (instance, args) -> {
                    CompiledModule.func_140(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                141,
                (instance, args) -> {
                    CompiledModule.func_141(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                142,
                (instance, args) -> {
                    CompiledModule.func_142(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                143,
                (instance, args) -> {
                    CompiledModule.func_143(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                144,
                (instance, args) -> {
                    CompiledModule.func_144(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                145,
                (instance, args) -> {
                    CompiledModule.func_145(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                146,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_146(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                147,
                (instance, args) -> {
                    CompiledModule.func_147(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                148,
                (instance, args) -> {
                    CompiledModule.func_148(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                149,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_149(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                150,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_150(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                151,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_151(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                152,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_152(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                153,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_153(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                154,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_154(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                155,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_155(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                156,
                (instance, args) -> {
                    CompiledModule.func_156(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                157,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_157(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                158,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_158(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                159,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_159(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                160,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_160(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                161,
                (instance, args) -> {
                    CompiledModule.func_161(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                162,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_162(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                163,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_163(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                164,
                (instance, args) -> {
                    CompiledModule.func_164(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                165,
                (instance, args) -> {
                    CompiledModule.func_165(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                166,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_166(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                167,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_167(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                168,
                (instance, args) -> {
                    CompiledModule.func_168(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                169,
                (instance, args) -> {
                    CompiledModule.func_169(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                170,
                (instance, args) -> {
                    CompiledModule.func_170(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                171,
                (instance, args) -> {
                    CompiledModule.func_171(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                172,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_172(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                173,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_173(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                174,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_174(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                175,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_175(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                176,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_176(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                177,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_177(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                178,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_178(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                179,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_179(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                180,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_180(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                181,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_181(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                182,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_182(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                183,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_183(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                184,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_184(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                185,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_185(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                186,
                (instance, args) -> {
                    CompiledModule.func_186(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                187,
                (instance, args) -> {
                    CompiledModule.func_187(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                188,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_188(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                189,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_189(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                190,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_190(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                191,
                (instance, args) -> {
                    CompiledModule.func_191(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                192,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_192(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                193,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_193(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                194,
                (instance, args) -> {
                    CompiledModule.func_194(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                195,
                (instance, args) -> {
                    CompiledModule.func_195(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                196,
                (instance, args) -> {
                    CompiledModule.func_196(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                197,
                (instance, args) -> {
                    CompiledModule.func_197(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                198,
                (instance, args) -> {
                    CompiledModule.func_198(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                199,
                (instance, args) -> {
                    CompiledModule.func_199(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                200,
                (instance, args) -> {
                    CompiledModule.func_200(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                201,
                (instance, args) -> {
                    CompiledModule.func_201(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                202,
                (instance, args) -> {
                    CompiledModule.func_202(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                203,
                (instance, args) -> {
                    CompiledModule.func_203(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                204,
                (instance, args) -> {
                    CompiledModule.func_204(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                205,
                (instance, args) -> {
                    CompiledModule.func_205(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                206,
                (instance, args) -> {
                    CompiledModule.func_206(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                207,
                (instance, args) -> {
                    CompiledModule.func_207(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                208,
                (instance, args) -> {
                    CompiledModule.func_208(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                209,
                (instance, args) -> {
                    CompiledModule.func_209(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                210,
                (instance, args) -> {
                    CompiledModule.func_210(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                211,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_211(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                212,
                (instance, args) -> {
                    CompiledModule.func_212(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                213,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_213(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                214,
                (instance, args) -> {
                    CompiledModule.func_214(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                215,
                (instance, args) -> {
                    CompiledModule.func_215(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                216,
                (instance, args) -> {
                    CompiledModule.func_216(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                217,
                (instance, args) -> {
                    CompiledModule.func_217(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            args[6].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                218,
                (instance, args) -> {
                    CompiledModule.func_218(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                219,
                (instance, args) -> {
                    CompiledModule.func_219(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                220,
                (instance, args) -> {
                    CompiledModule.func_220(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                221,
                (instance, args) -> {
                    CompiledModule.func_221(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                222,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_222(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                223,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_223(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                224,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_224(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                225,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_225(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                226,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_226(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                227,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_227(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                228,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_228(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                229,
                (instance, args) -> {
                    CompiledModule.func_229(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                230,
                (instance, args) -> {
                    CompiledModule.func_230(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                231,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_231(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                232,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_232(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                233,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_233(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                234,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(CompiledModule.func_234(instance.memory(), instance))
                    };
                });
        funcs.put(
                235,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_235(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                236,
                (instance, args) -> {
                    CompiledModule.func_236(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                237,
                (instance, args) -> {
                    CompiledModule.func_237(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                238,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_238(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                239,
                (instance, args) -> {
                    CompiledModule.func_239(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                240,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_240(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                241,
                (instance, args) -> {
                    CompiledModule.func_241(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                242,
                (instance, args) -> {
                    CompiledModule.func_242(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                243,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_243(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                244,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_244(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                245,
                (instance, args) -> {
                    CompiledModule.func_245(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                246,
                (instance, args) -> {
                    CompiledModule.func_246(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                247,
                (instance, args) -> {
                    CompiledModule.func_247(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                248,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_248(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                249,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_249(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                250,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_250(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                251,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_251(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                252,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_252(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                253,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_253(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                254,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_254(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                255,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_255(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                256,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_256(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                257,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_257(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                258,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_258(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                259,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_259(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                260,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_260(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                261,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_261(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                262,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_262(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                263,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_263(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                264,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_264(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                265,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_265(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                266,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_266(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                267,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_267(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                268,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_268(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                269,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_269(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                270,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_270(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                271,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_271(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                272,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_272(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                273,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_273(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                274,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_274(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                275,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_275(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                276,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_276(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                277,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_277(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                278,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_278(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                279,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_279(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                280,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_280(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                281,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_281(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                282,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_282(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                283,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_283(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                284,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_284(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                285,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_285(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                286,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_286(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                287,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_287(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                288,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_288(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                289,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_289(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                290,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_290(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                291,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_291(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                292,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_292(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                293,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_293(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                294,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_294(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                295,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_295(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                296,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_296(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                297,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_297(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                298,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_298(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                299,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_299(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                300,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_300(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                301,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_301(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                302,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_302(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                303,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_303(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                304,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_304(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                305,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_305(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                306,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_306(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                307,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_307(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                308,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_308(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                309,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_309(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                310,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_310(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                311,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_311(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                312,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_312(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                313,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_313(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                314,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_314(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                315,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_315(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                316,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_316(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                317,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_317(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                318,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_318(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                319,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_319(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                320,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_320(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                321,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_321(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                322,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_322(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                323,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_323(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                324,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_324(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                325,
                (instance, args) -> {
                    CompiledModule.func_325(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                326,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_326(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                327,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_327(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                328,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_328(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                329,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_329(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                330,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_330(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                331,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_331(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                332,
                (instance, args) -> {
                    CompiledModule.func_332(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                333,
                (instance, args) -> {
                    CompiledModule.func_333(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                334,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_334(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                335,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_335(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                336,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_336(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                337,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_337(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                338,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_338(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                339,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_339(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                340,
                (instance, args) -> {
                    CompiledModule.func_340(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                341,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_341(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                342,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_342(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                343,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_343(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                344,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_344(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                345,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_345(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                346,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_346(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                347,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_347(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                348,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_348(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                349,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_349(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                350,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_350(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                351,
                (instance, args) -> {
                    CompiledModule.func_351(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                352,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_352(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                353,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_353(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                354,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_354(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                355,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_355(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                356,
                (instance, args) -> {
                    CompiledModule.func_356(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                357,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_357(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                358,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_358(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                359,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_359(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                360,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_360(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                361,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_361(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                362,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_362(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                363,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_363(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                364,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_364(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                365,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_365(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                366,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_366(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                367,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_367(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                368,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_368(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                369,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_369(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                370,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_370(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                371,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_371(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                372,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_372(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                373,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_373(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                374,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_374(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                375,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_375(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                376,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_376(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                377,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_377(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                378,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_378(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                379,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_379(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                380,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_380(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                381,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_381(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                382,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_382(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                383,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_383(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                384,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_384(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                385,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_385(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                386,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_386(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                387,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_387(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                388,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_388(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                389,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_389(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                390,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_390(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                391,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_391(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                392,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_392(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                393,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_393(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                394,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_394(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                395,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_395(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                396,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_396(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                397,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_397(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                398,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_398(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                399,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_399(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                400,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_400(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                401,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_401(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                402,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_402(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                403,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_403(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                404,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_404(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                405,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_405(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                406,
                (instance, args) -> {
                    CompiledModule.func_406(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                407,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_407(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                408,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_408(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                409,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_409(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                410,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_410(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                411,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_411(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                412,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_412(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                413,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_413(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                414,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_414(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                415,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_415(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                416,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_416(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                417,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_417(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                418,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_418(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                419,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_419(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                420,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_420(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                421,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_421(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                422,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_422(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                423,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_423(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                424,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_424(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                425,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_425(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                426,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_426(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                427,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_427(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                428,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_428(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                429,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_429(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                430,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_430(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                431,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_431(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                432,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_432(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                433,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_433(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                434,
                (instance, args) -> {
                    CompiledModule.func_434(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                435,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_435(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                436,
                (instance, args) -> {
                    CompiledModule.func_436(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                437,
                (instance, args) -> {
                    CompiledModule.func_437(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                438,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_438(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                439,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_439(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                440,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_440(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                441,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_441(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                442,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_442(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                443,
                (instance, args) -> {
                    CompiledModule.func_443(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                444,
                (instance, args) -> {
                    CompiledModule.func_444(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                445,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_445(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                446,
                (instance, args) -> {
                    CompiledModule.func_446(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                447,
                (instance, args) -> {
                    CompiledModule.func_447(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            args[6].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                448,
                (instance, args) -> {
                    CompiledModule.func_448(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                449,
                (instance, args) -> {
                    CompiledModule.func_449(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                450,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_450(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                451,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_451(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                452,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_452(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                453,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_453(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                454,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_454(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                455,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_455(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                456,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_456(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                457,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_457(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                458,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_458(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                459,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_459(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                460,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_460(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                461,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_461(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                462,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_462(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                463,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_463(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                464,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_464(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                465,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_465(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                466,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_466(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                467,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_467(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                468,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_468(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                469,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_469(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                470,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_470(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                471,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_471(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                472,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_472(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                473,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_473(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                474,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_474(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                475,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_475(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                476,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_476(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                477,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_477(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                478,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_478(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                479,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_479(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                480,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_480(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                481,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_481(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                482,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_482(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                483,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_483(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                484,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_484(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                485,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_485(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                486,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_486(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                487,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_487(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                488,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_488(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                489,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_489(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                490,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_490(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                491,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_491(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                492,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_492(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                493,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_493(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                494,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_494(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                495,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_495(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                496,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_496(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                497,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_497(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                498,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_498(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                499,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_499(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                500,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_500(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                501,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_501(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                502,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_502(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                503,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_503(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                504,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_504(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                505,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_505(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                506,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_506(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                507,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_507(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                508,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_508(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                509,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_509(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                510,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_510(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                511,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_511(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                512,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_512(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                513,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_513(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                514,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_514(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                515,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_515(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                516,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_516(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                517,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_517(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                518,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_518(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                519,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_519(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                520,
                (instance, args) -> {
                    CompiledModule.func_520(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                521,
                (instance, args) -> {
                    CompiledModule.func_521(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                522,
                (instance, args) -> {
                    CompiledModule.func_522(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                523,
                (instance, args) -> {
                    CompiledModule.func_523(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                524,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_524(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                525,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_525(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                526,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_526(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                527,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_527(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                528,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_528(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                529,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_529(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                530,
                (instance, args) -> {
                    CompiledModule.func_530(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                531,
                (instance, args) -> {
                    CompiledModule.func_531(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                532,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_532(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                533,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_533(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                534,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_534(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                535,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_535(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                536,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_536(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                537,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_537(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                538,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_538(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                539,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_539(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                540,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_540(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                541,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_541(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                542,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_542(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                543,
                (instance, args) -> {
                    CompiledModule.func_543(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                544,
                (instance, args) -> {
                    CompiledModule.func_544(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                545,
                (instance, args) -> {
                    CompiledModule.func_545(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                546,
                (instance, args) -> {
                    CompiledModule.func_546(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                547,
                (instance, args) -> {
                    CompiledModule.func_547(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                548,
                (instance, args) -> {
                    CompiledModule.func_548(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                549,
                (instance, args) -> {
                    CompiledModule.func_549(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                550,
                (instance, args) -> {
                    CompiledModule.func_550(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                551,
                (instance, args) -> {
                    CompiledModule.func_551(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                552,
                (instance, args) -> {
                    CompiledModule.func_552(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                553,
                (instance, args) -> {
                    CompiledModule.func_553(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                554,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_554(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                555,
                (instance, args) -> {
                    CompiledModule.func_555(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                556,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_556(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                557,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_557(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                558,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_558(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                559,
                (instance, args) -> {
                    CompiledModule.func_559(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                560,
                (instance, args) -> {
                    CompiledModule.func_560(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                561,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_561(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                562,
                (instance, args) -> {
                    CompiledModule.func_562(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                563,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_563(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                564,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_564(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                565,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_565(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                566,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_566(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                567,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_567(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                568,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_568(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                569,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_569(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                570,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_570(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                571,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_571(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                572,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_572(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                573,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_573(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                574,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_574(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                575,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_575(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                576,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_576(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                577,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_577(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                578,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_578(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                579,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_579(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                580,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_580(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                581,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_581(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                582,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_582(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                583,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_583(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                584,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_584(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                585,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_585(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                586,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_586(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                587,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_587(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                588,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_588(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                589,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_589(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                590,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_590(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                591,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_591(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                592,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_592(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                593,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_593(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                594,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_594(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                595,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_595(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                596,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_596(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                597,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_597(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                598,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_598(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                599,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_599(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                600,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_600(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                601,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_601(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                602,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_602(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                603,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_603(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                604,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_604(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                605,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_605(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                606,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_606(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                607,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_607(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                608,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_608(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                609,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_609(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                610,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_610(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                611,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_611(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                612,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_612(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                613,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_613(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                614,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_614(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                615,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_615(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                616,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_616(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                617,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_617(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                618,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_618(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                619,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_619(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                620,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_620(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                621,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_621(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                622,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_622(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                623,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_623(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                624,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_624(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                625,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_625(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                626,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_626(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                627,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_627(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                628,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_628(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                629,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_629(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                630,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_630(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                631,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_631(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                632,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_632(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                633,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_633(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                634,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_634(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                635,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_635(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                636,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_636(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                637,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_637(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                638,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_638(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                639,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_639(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                640,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_640(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                641,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_641(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                642,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_642(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                643,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_643(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                644,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_644(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                645,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_645(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                646,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_646(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                647,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_647(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                648,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_648(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                649,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_649(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                650,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_650(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                651,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_651(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                652,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_652(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                653,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_653(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                654,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_654(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                655,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_655(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                656,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_656(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                657,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_657(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                658,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_658(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                659,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_659(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                660,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_660(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                661,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_661(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                662,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_662(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                663,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_663(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                664,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_664(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                665,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_665(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                666,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_666(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                667,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_667(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                668,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_668(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                669,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_669(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                670,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_670(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                671,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_671(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                672,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_672(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                673,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_673(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                674,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_674(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                675,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_675(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                676,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_676(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                677,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_677(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                678,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_678(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                679,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_679(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                680,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_680(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                681,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_681(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                682,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_682(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                683,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_683(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                684,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_684(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                685,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_685(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                686,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_686(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                687,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_687(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                688,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_688(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                689,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_689(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                690,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_690(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                691,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_691(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                692,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_692(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                693,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_693(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                694,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_694(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                695,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_695(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                696,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_696(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                697,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_697(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                698,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_698(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                699,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_699(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                700,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_700(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                701,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_701(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                702,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_702(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                703,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_703(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                704,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_704(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                705,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_705(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                706,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_706(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                707,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_707(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                708,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_708(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                709,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_709(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                710,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_710(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                711,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_711(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                712,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_712(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                713,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_713(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                714,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_714(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                715,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_715(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                716,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_716(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                717,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_717(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                718,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_718(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                719,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_719(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                720,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_720(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                721,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_721(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                722,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_722(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                723,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_723(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                724,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_724(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                725,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_725(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                726,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_726(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                727,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_727(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                728,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_728(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                729,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_729(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                730,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_730(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                731,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_731(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                732,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_732(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                733,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_733(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                734,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_734(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                735,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_735(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                736,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_736(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                737,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_737(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                738,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_738(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                739,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_739(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                740,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_740(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                741,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_741(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                742,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_742(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                743,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_743(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                744,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_744(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                745,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_745(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                746,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_746(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                747,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_747(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                748,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_748(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                749,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_749(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                750,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_750(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                751,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_751(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                752,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_752(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                753,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_753(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                754,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_754(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                755,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_755(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                756,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_756(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                757,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_757(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                758,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_758(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                759,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_759(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                760,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_760(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                761,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_761(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                762,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_762(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                763,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_763(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                764,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_764(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                765,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_765(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                766,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_766(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                767,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_767(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                768,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_768(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                769,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_769(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                770,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_770(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                771,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_771(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                772,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_772(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                773,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_773(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                774,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_774(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                775,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_775(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                776,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_776(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                777,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_777(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                778,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_778(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                779,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_779(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                780,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_780(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                781,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_781(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                782,
                (instance, args) -> {
                    CompiledModule.func_782(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                783,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_783(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                784,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_784(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                785,
                (instance, args) -> {
                    CompiledModule.func_785(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                786,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_786(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                787,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_787(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                788,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_788(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                789,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_789(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                790,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_790(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                791,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_791(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                792,
                (instance, args) -> {
                    CompiledModule.func_792(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                793,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_793(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                794,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_794(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                795,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_795(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                796,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_796(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                797,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_797(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                798,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_798(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                799,
                (instance, args) -> {
                    CompiledModule.func_799(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                800,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_800(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                801,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_801(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                802,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_802(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                803,
                (instance, args) -> {
                    CompiledModule.func_803(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                804,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_804(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                805,
                (instance, args) -> {
                    CompiledModule.func_805(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                806,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_806(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                807,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_807(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                808,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_808(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                809,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_809(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                810,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_810(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        args[7].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                811,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_811(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                812,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_812(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                813,
                (instance, args) -> {
                    CompiledModule.func_813(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                814,
                (instance, args) -> {
                    CompiledModule.func_814(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                815,
                (instance, args) -> {
                    CompiledModule.func_815(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                816,
                (instance, args) -> {
                    CompiledModule.func_816(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                817,
                (instance, args) -> {
                    CompiledModule.func_817(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                818,
                (instance, args) -> {
                    CompiledModule.func_818(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                819,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_819(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                820,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_820(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                821,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_821(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                822,
                (instance, args) -> {
                    CompiledModule.func_822(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                823,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_823(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                824,
                (instance, args) -> {
                    CompiledModule.func_824(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                825,
                (instance, args) -> {
                    CompiledModule.func_825(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                826,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_826(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                827,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_827(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                828,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_828(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                829,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_829(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                830,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_830(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                831,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_831(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                832,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_832(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                833,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_833(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                834,
                (instance, args) -> {
                    CompiledModule.func_834(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                835,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_835(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                836,
                (instance, args) -> {
                    CompiledModule.func_836(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                837,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_837(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                838,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_838(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                839,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_839(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                840,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_840(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                841,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_841(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                842,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_842(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                843,
                (instance, args) -> {
                    CompiledModule.func_843(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                844,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_844(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                845,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_845(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                846,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_846(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                847,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_847(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                848,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_848(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                849,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_849(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                850,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_850(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                851,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_851(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                852,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_852(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                853,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_853(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                854,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_854(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                855,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_855(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                856,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_856(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                857,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_857(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                858,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_858(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                859,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_859(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                860,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_860(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                861,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_861(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                862,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_862(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                863,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_863(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                864,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_864(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                865,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_865(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                866,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_866(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                867,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_867(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                868,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_868(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                869,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_869(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                870,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_870(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                871,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_871(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                872,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_872(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                873,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_873(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                874,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_874(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                875,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_875(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                876,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_876(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                877,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_877(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                878,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_878(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                879,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_879(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                880,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_880(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                881,
                (instance, args) -> {
                    CompiledModule.func_881(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                882,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_882(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                883,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_883(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                884,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_884(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                885,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_885(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                886,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_886(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                887,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_887(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                888,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_888(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                889,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_889(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                890,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_890(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                891,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_891(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                892,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_892(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                893,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_893(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                894,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_894(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                895,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_895(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                896,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_896(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                897,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_897(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                898,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_898(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                899,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_899(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                900,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_900(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                901,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_901(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                902,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_902(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                903,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_903(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                904,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_904(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                905,
                (instance, args) -> {
                    CompiledModule.func_905(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                906,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_906(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                907,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_907(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                908,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_908(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                909,
                (instance, args) -> {
                    CompiledModule.func_909(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                910,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_910(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                911,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_911(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                912,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_912(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                913,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_913(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                914,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_914(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                915,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_915(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                916,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_916(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                917,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_917(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                918,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_918(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                919,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_919(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                920,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_920(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                921,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_921(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                922,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_922(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                923,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_923(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                924,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_924(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                925,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_925(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                926,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_926(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                927,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_927(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                928,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_928(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                929,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_929(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                930,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_930(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                931,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_931(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                932,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_932(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                933,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_933(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                934,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_934(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                935,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_935(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                936,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_936(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                937,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_937(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                938,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_938(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                939,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_939(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                940,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_940(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                941,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_941(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                942,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_942(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                943,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_943(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                944,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_944(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                945,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_945(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                946,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_946(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                947,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_947(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                948,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_948(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                949,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_949(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                950,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_950(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                951,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_951(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                952,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_952(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                953,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_953(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                954,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_954(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                955,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_955(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                956,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_956(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                957,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_957(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                958,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_958(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                959,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_959(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                960,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_960(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                961,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_961(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                962,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_962(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                963,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_963(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                964,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_964(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                965,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_965(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                966,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_966(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                967,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_967(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                968,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_968(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                969,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_969(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                970,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_970(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                971,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_971(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                972,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_972(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                973,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_973(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                974,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_974(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                975,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_975(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                976,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_976(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                977,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_977(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                978,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_978(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                979,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_979(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                980,
                (instance, args) -> {
                    CompiledModule.func_980(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                981,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_981(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                982,
                (instance, args) -> {
                    CompiledModule.func_982(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                983,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_983(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                984,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_984(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                985,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_985(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                986,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_986(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                987,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_987(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                988,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_988(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                989,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_989(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                990,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_990(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                991,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_991(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                992,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_992(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                993,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_993(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                994,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_994(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                995,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_995(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                996,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_996(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                997,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_997(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                998,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_998(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                999,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_999(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1000,
                (instance, args) -> {
                    CompiledModule.func_1000(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1001,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1001(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1002,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1002(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1003,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1003(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1004,
                (instance, args) -> {
                    CompiledModule.func_1004(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1005,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1005(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1006,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1006(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1007,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1007(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1008,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1008(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1009,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1009(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1010,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1010(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1011,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1011(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1012,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1012(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1013,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1013(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1014,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1014(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1015,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1015(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1016,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1016(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1017,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1017(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1018,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1018(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1019,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1019(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1020,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1020(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1021,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1021(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1022,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1022(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1023,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1023(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1024,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1024(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1025,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1025(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1026,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1026(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1027,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1027(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1028,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1028(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1029,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1029(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1030,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1030(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1031,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1031(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1032,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1032(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1033,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1033(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1034,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1034(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1035,
                (instance, args) -> {
                    CompiledModule.func_1035(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1036,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1036(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1037,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1037(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1038,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1038(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1039,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1039(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1040,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1040(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1041,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1041(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1042,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1042(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1043,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1043(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1044,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1044(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1045,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1045(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1046,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1046(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1047,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1047(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1048,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1048(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1049,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1049(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1050,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1050(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1051,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1051(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1052,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1052(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1053,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1053(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1054,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1054(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1055,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1055(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1056,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1056(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1057,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1057(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1058,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1058(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1059,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1059(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1060,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1060(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1061,
                (instance, args) -> {
                    CompiledModule.func_1061(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1062,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1062(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1063,
                (instance, args) -> {
                    CompiledModule.func_1063(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1064,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1064(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1065,
                (instance, args) -> {
                    CompiledModule.func_1065(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1066,
                (instance, args) -> {
                    CompiledModule.func_1066(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1067,
                (instance, args) -> {
                    CompiledModule.func_1067(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1068,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1068(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1069,
                (instance, args) -> {
                    CompiledModule.func_1069(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1070,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1070(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1071,
                (instance, args) -> {
                    CompiledModule.func_1071(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1072,
                (instance, args) -> {
                    CompiledModule.func_1072(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1073,
                (instance, args) -> {
                    CompiledModule.func_1073(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1074,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1074(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1075,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1075(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1076,
                (instance, args) -> {
                    CompiledModule.func_1076(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1077,
                (instance, args) -> {
                    CompiledModule.func_1077(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1078,
                (instance, args) -> {
                    CompiledModule.func_1078(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1079,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1079(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1080,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1080(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1081,
                (instance, args) -> {
                    CompiledModule.func_1081(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1082,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1082(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1083,
                (instance, args) -> {
                    CompiledModule.func_1083(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1084,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1084(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1085,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1085(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1086,
                (instance, args) -> {
                    CompiledModule.func_1086(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1087,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1087(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1088,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1088(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1089,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1089(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1090,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1090(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1091,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1091(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1092,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1092(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1093,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1093(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1094,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1094(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1095,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1095(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1096,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1096(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1097,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1097(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1098,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1098(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1099,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1099(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1100,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1100(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1101,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1101(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1102,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1102(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1103,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1103(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1104,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1104(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1105,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1105(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1106,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1106(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1107,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1107(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1108,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1108(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1109,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1109(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1110,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1110(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1111,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1111(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1112,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1112(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1113,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1113(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1114,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1114(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1115,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1115(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1116,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1116(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1117,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1117(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1118,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1118(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1119,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1119(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1120,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1120(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1121,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1121(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1122,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1122(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1123,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1123(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1124,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1124(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1125,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1125(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1126,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1126(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1127,
                (instance, args) -> {
                    CompiledModule.func_1127(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1128,
                (instance, args) -> {
                    CompiledModule.func_1128(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1129,
                (instance, args) -> {
                    CompiledModule.func_1129(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1130,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1130(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1131,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1131(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1132,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1132(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1133,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1133(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1134,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1134(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1135,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1135(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1136,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1136(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1137,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1137(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1138,
                (instance, args) -> {
                    CompiledModule.func_1138(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1139,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1139(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1140,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1140(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1141,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1141(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1142,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1142(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1143,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1143(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1144,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1144(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1145,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1145(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1146,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1146(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1147,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1147(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1148,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1148(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1149,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1149(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1150,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1150(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1151,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1151(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1152,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1152(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1153,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1153(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1154,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1154(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1155,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1155(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1156,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1156(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1157,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1157(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1158,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1158(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1159,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1159(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1160,
                (instance, args) -> {
                    CompiledModule.func_1160(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1161,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1161(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1162,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1162(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1163,
                (instance, args) -> {
                    CompiledModule.func_1163(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1164,
                (instance, args) -> {
                    CompiledModule.func_1164(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1165,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1165(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1166,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1166(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1167,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1167(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1168,
                (instance, args) -> {
                    CompiledModule.func_1168(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1169,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1169(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1170,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1170(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1171,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1171(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1172,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1172(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1173,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1173(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1174,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1174(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1175,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1175(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1176,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1176(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1177,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1177(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1178,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1178(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1179,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1179(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1180,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1180(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1181,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1181(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1182,
                (instance, args) -> {
                    CompiledModule.func_1182(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1183,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1183(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1184,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1184(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1185,
                (instance, args) -> {
                    CompiledModule.func_1185(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1186,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1186(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1187,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1187(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1188,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1188(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1189,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1189(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1190,
                (instance, args) -> {
                    CompiledModule.func_1190(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1191,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1191(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1192,
                (instance, args) -> {
                    CompiledModule.func_1192(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1193,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1193(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1194,
                (instance, args) -> {
                    CompiledModule.func_1194(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1195,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1195(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1196,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1196(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1197,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1197(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1198,
                (instance, args) -> {
                    CompiledModule.func_1198(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1199,
                (instance, args) -> {
                    CompiledModule.func_1199(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1200,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1200(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1201,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1201(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1202,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1202(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1203,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1203(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1204,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1204(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1205,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1205(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1206,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1206(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1207,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1207(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1208,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1208(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1209,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1209(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1210,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1210(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1211,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1211(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1212,
                (instance, args) -> {
                    CompiledModule.func_1212(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1213,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1213(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1214,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1214(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1215,
                (instance, args) -> {
                    CompiledModule.func_1215(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1216,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1216(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1217,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1217(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1218,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1218(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1219,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1219(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1220,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1220(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1221,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1221(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1222,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1222(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1223,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1223(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1224,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1224(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1225,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1225(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1226,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1226(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1227,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1227(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1228,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1228(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1229,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1229(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1230,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1230(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1231,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1231(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1232,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1232(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1233,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1233(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1234,
                (instance, args) -> {
                    CompiledModule.func_1234(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1235,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1235(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1236,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1236(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1237,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1237(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1238,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1238(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1239,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1239(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1240,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1240(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1241,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1241(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1242,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1242(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1243,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1243(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1244,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1244(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1245,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1245(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1246,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1246(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1247,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1247(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1248,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1248(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1249,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1249(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1250,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1250(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1251,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1251(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1252,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1252(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1253,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1253(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1254,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1254(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1255,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1255(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1256,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1256(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1257,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1257(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1258,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1258(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1259,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1259(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1260,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1260(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1261,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1261(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1262,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1262(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1263,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1263(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1264,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1264(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1265,
                (instance, args) -> {
                    CompiledModule.func_1265(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1266,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1266(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1267,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1267(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1268,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1268(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1269,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1269(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1270,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1270(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1271,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1271(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1272,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1272(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1273,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1273(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1274,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1274(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1275,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1275(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1276,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1276(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1277,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1277(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1278,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1278(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1279,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1279(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1280,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1280(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1281,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1281(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1282,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1282(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1283,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1283(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1284,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1284(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1285,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1285(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1286,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1286(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1287,
                (instance, args) -> {
                    CompiledModule.func_1287(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1288,
                (instance, args) -> {
                    CompiledModule.func_1288(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1289,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1289(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1290,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1290(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1291,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1291(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1292,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1292(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1293,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1293(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1294,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1294(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1295,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1295(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1296,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1296(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1297,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1297(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1298,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1298(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1299,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1299(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1300,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1300(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1301,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1301(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1302,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1302(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1303,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1303(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1304,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1304(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1305,
                (instance, args) -> {
                    CompiledModule.func_1305(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1306,
                (instance, args) -> {
                    CompiledModule.func_1306(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1307,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1307(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1308,
                (instance, args) -> {
                    CompiledModule.func_1308(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1309,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1309(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1310,
                (instance, args) -> {
                    CompiledModule.func_1310(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1311,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1311(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1312,
                (instance, args) -> {
                    CompiledModule.func_1312(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1313,
                (instance, args) -> {
                    CompiledModule.func_1313(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1314,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1314(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1315,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1315(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1316,
                (instance, args) -> {
                    CompiledModule.func_1316(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1317,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1317(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1318,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1318(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1319,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1319(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1320,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1320(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1321,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1321(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1322,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1322(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1323,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1323(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1324,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1324(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1325,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1325(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1326,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1326(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1327,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1327(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1328,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1328(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1329,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1329(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1330,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1330(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1331,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1331(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1332,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1332(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1333,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1333(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1334,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1334(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1335,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1335(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1336,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1336(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1337,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1337(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1338,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1338(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1339,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1339(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1340,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1340(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1341,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1341(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1342,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1342(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1343,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1343(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1344,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1344(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1345,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1345(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1346,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1346(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1347,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1347(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1348,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1348(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1349,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1349(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1350,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1350(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1351,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1351(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1352,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1352(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1353,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1353(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1354,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1354(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1355,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1355(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1356,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1356(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1357,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1357(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1358,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1358(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1359,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1359(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1360,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1360(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1361,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1361(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1362,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1362(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1363,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1363(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1364,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1364(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1365,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1365(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1366,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1366(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1367,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1367(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1368,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1368(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1369,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1369(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1370,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1370(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1371,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1371(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1372,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1372(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1373,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1373(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1374,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1374(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1375,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1375(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1376,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1376(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1377,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1377(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1378,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1378(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1379,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1379(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1380,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1380(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1381,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1381(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1382,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1382(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1383,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1383(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1384,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1384(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1385,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1385(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1386,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1386(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1387,
                (instance, args) -> {
                    CompiledModule.func_1387(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1388,
                (instance, args) -> {
                    CompiledModule.func_1388(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1389,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1389(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1390,
                (instance, args) -> {
                    CompiledModule.func_1390(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1391,
                (instance, args) -> {
                    CompiledModule.func_1391(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1392,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1392(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1393,
                (instance, args) -> {
                    CompiledModule.func_1393(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1394,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1394(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1395,
                (instance, args) -> {
                    CompiledModule.func_1395(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1396,
                (instance, args) -> {
                    CompiledModule.func_1396(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1397,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1397(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1398,
                (instance, args) -> {
                    CompiledModule.func_1398(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1399,
                (instance, args) -> {
                    CompiledModule.func_1399(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1400,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1400(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1401,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1401(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1402,
                (instance, args) -> {
                    CompiledModule.func_1402(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1403,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1403(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1404,
                (instance, args) -> {
                    CompiledModule.func_1404(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1405,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1405(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1406,
                (instance, args) -> {
                    CompiledModule.func_1406(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1407,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1407(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1408,
                (instance, args) -> {
                    CompiledModule.func_1408(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1409,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1409(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1410,
                (instance, args) -> {
                    CompiledModule.func_1410(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1411,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1411(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1412,
                (instance, args) -> {
                    CompiledModule.func_1412(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1413,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1413(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1414,
                (instance, args) -> {
                    CompiledModule.func_1414(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1415,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1415(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1416,
                (instance, args) -> {
                    CompiledModule.func_1416(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1417,
                (instance, args) -> {
                    CompiledModule.func_1417(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1418,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1418(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1419,
                (instance, args) -> {
                    CompiledModule.func_1419(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1420,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1420(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1421,
                (instance, args) -> {
                    CompiledModule.func_1421(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1422,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1422(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1423,
                (instance, args) -> {
                    CompiledModule.func_1423(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1424,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1424(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1425,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1425(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1426,
                (instance, args) -> {
                    CompiledModule.func_1426(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1427,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1427(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1428,
                (instance, args) -> {
                    CompiledModule.func_1428(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1429,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1429(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1430,
                (instance, args) -> {
                    CompiledModule.func_1430(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1431,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1431(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1432,
                (instance, args) -> {
                    CompiledModule.func_1432(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1433,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1433(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1434,
                (instance, args) -> {
                    CompiledModule.func_1434(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1435,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1435(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1436,
                (instance, args) -> {
                    CompiledModule.func_1436(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1437,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1437(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1438,
                (instance, args) -> {
                    CompiledModule.func_1438(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1439,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1439(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1440,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1440(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1441,
                (instance, args) -> {
                    CompiledModule.func_1441(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1442,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1442(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1443,
                (instance, args) -> {
                    CompiledModule.func_1443(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1444,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1444(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1445,
                (instance, args) -> {
                    CompiledModule.func_1445(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1446,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1446(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1447,
                (instance, args) -> {
                    CompiledModule.func_1447(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1448,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1448(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1449,
                (instance, args) -> {
                    CompiledModule.func_1449(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1450,
                (instance, args) -> {
                    CompiledModule.func_1450(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1451,
                (instance, args) -> {
                    CompiledModule.func_1451(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1452,
                (instance, args) -> {
                    CompiledModule.func_1452(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1453,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1453(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1454,
                (instance, args) -> {
                    CompiledModule.func_1454(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1455,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1455(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1456,
                (instance, args) -> {
                    CompiledModule.func_1456(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1457,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1457(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1458,
                (instance, args) -> {
                    CompiledModule.func_1458(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1459,
                (instance, args) -> {
                    CompiledModule.func_1459(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1460,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1460(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1461,
                (instance, args) -> {
                    CompiledModule.func_1461(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1462,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1462(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1463,
                (instance, args) -> {
                    CompiledModule.func_1463(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1464,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1464(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1465,
                (instance, args) -> {
                    CompiledModule.func_1465(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1466,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1466(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1467,
                (instance, args) -> {
                    CompiledModule.func_1467(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1468,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1468(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1469,
                (instance, args) -> {
                    CompiledModule.func_1469(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1470,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1470(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1471,
                (instance, args) -> {
                    CompiledModule.func_1471(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1472,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1472(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1473,
                (instance, args) -> {
                    CompiledModule.func_1473(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1474,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1474(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1475,
                (instance, args) -> {
                    CompiledModule.func_1475(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1476,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1476(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1477,
                (instance, args) -> {
                    CompiledModule.func_1477(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1478,
                (instance, args) -> {
                    CompiledModule.func_1478(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1479,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1479(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1480,
                (instance, args) -> {
                    CompiledModule.func_1480(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1481,
                (instance, args) -> {
                    CompiledModule.func_1481(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1482,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1482(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1483,
                (instance, args) -> {
                    CompiledModule.func_1483(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1484,
                (instance, args) -> {
                    CompiledModule.func_1484(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1485,
                (instance, args) -> {
                    CompiledModule.func_1485(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1486,
                (instance, args) -> {
                    CompiledModule.func_1486(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1487,
                (instance, args) -> {
                    CompiledModule.func_1487(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1488,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1488(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1489,
                (instance, args) -> {
                    CompiledModule.func_1489(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1490,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1490(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1491,
                (instance, args) -> {
                    CompiledModule.func_1491(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1492,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1492(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1493,
                (instance, args) -> {
                    CompiledModule.func_1493(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1494,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1494(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1495,
                (instance, args) -> {
                    CompiledModule.func_1495(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1496,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1496(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1497,
                (instance, args) -> {
                    CompiledModule.func_1497(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1498,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1498(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1499,
                (instance, args) -> {
                    CompiledModule.func_1499(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1500,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1500(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1501,
                (instance, args) -> {
                    CompiledModule.func_1501(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1502,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1502(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1503,
                (instance, args) -> {
                    CompiledModule.func_1503(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1504,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1504(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1505,
                (instance, args) -> {
                    CompiledModule.func_1505(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1506,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1506(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1507,
                (instance, args) -> {
                    CompiledModule.func_1507(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1508,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1508(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1509,
                (instance, args) -> {
                    CompiledModule.func_1509(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1510,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1510(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1511,
                (instance, args) -> {
                    CompiledModule.func_1511(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1512,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1512(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1513,
                (instance, args) -> {
                    CompiledModule.func_1513(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1514,
                (instance, args) -> {
                    CompiledModule.func_1514(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1515,
                (instance, args) -> {
                    CompiledModule.func_1515(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1516,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1516(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1517,
                (instance, args) -> {
                    CompiledModule.func_1517(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1518,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1518(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1519,
                (instance, args) -> {
                    CompiledModule.func_1519(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1520,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1520(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1521,
                (instance, args) -> {
                    CompiledModule.func_1521(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1522,
                (instance, args) -> {
                    CompiledModule.func_1522(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1523,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1523(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1524,
                (instance, args) -> {
                    CompiledModule.func_1524(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1525,
                (instance, args) -> {
                    CompiledModule.func_1525(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1526,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1526(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1527,
                (instance, args) -> {
                    CompiledModule.func_1527(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1528,
                (instance, args) -> {
                    CompiledModule.func_1528(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1529,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1529(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1530,
                (instance, args) -> {
                    CompiledModule.func_1530(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1531,
                (instance, args) -> {
                    CompiledModule.func_1531(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1532,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1532(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1533,
                (instance, args) -> {
                    CompiledModule.func_1533(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1534,
                (instance, args) -> {
                    CompiledModule.func_1534(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1535,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1535(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1536,
                (instance, args) -> {
                    CompiledModule.func_1536(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1537,
                (instance, args) -> {
                    CompiledModule.func_1537(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1538,
                (instance, args) -> {
                    CompiledModule.func_1538(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1539,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1539(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1540,
                (instance, args) -> {
                    CompiledModule.func_1540(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1541,
                (instance, args) -> {
                    CompiledModule.func_1541(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1542,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1542(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1543,
                (instance, args) -> {
                    CompiledModule.func_1543(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1544,
                (instance, args) -> {
                    CompiledModule.func_1544(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1545,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1545(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1546,
                (instance, args) -> {
                    CompiledModule.func_1546(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1547,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1547(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1548,
                (instance, args) -> {
                    CompiledModule.func_1548(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1549,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1549(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1550,
                (instance, args) -> {
                    CompiledModule.func_1550(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1551,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1551(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1552,
                (instance, args) -> {
                    CompiledModule.func_1552(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1553,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1553(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1554,
                (instance, args) -> {
                    CompiledModule.func_1554(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1555,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1555(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1556,
                (instance, args) -> {
                    CompiledModule.func_1556(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1557,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1557(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1558,
                (instance, args) -> {
                    CompiledModule.func_1558(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1559,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1559(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1560,
                (instance, args) -> {
                    CompiledModule.func_1560(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1561,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1561(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1562,
                (instance, args) -> {
                    CompiledModule.func_1562(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1563,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1563(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1564,
                (instance, args) -> {
                    CompiledModule.func_1564(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1565,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1565(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1566,
                (instance, args) -> {
                    CompiledModule.func_1566(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1567,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1567(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1568,
                (instance, args) -> {
                    CompiledModule.func_1568(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1569,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1569(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1570,
                (instance, args) -> {
                    CompiledModule.func_1570(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1571,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1571(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1572,
                (instance, args) -> {
                    CompiledModule.func_1572(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1573,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1573(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1574,
                (instance, args) -> {
                    CompiledModule.func_1574(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1575,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1575(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1576,
                (instance, args) -> {
                    CompiledModule.func_1576(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1577,
                (instance, args) -> {
                    CompiledModule.func_1577(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1578,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1578(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1579,
                (instance, args) -> {
                    CompiledModule.func_1579(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1580,
                (instance, args) -> {
                    CompiledModule.func_1580(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1581,
                (instance, args) -> {
                    CompiledModule.func_1581(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1582,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1582(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1583,
                (instance, args) -> {
                    CompiledModule.func_1583(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1584,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1584(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1585,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1585(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1586,
                (instance, args) -> {
                    CompiledModule.func_1586(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1587,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1587(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1588,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1588(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1589,
                (instance, args) -> {
                    CompiledModule.func_1589(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1590,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1590(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1591,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1591(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1592,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1592(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1593,
                (instance, args) -> {
                    CompiledModule.func_1593(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1594,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1594(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1595,
                (instance, args) -> {
                    CompiledModule.func_1595(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1596,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1596(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1597,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1597(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1598,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1598(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1599,
                (instance, args) -> {
                    CompiledModule.func_1599(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1600,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1600(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1601,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1601(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1602,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1602(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1603,
                (instance, args) -> {
                    CompiledModule.func_1603(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1604,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1604(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1605,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1605(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1606,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1606(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1607,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1607(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1608,
                (instance, args) -> {
                    CompiledModule.func_1608(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1609,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1609(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1610,
                (instance, args) -> {
                    CompiledModule.func_1610(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1611,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1611(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1612,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1612(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1613,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1613(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1614,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1614(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1615,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1615(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1616,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1616(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1617,
                (instance, args) -> {
                    CompiledModule.func_1617(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            args[6].asInt(),
                            args[7].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1618,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1618(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1619,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1619(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1620,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1620(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1621,
                (instance, args) -> {
                    CompiledModule.func_1621(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1622,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1622(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1623,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1623(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1624,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1624(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1625,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1625(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1626,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1626(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1627,
                (instance, args) -> {
                    CompiledModule.func_1627(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1628,
                (instance, args) -> {
                    CompiledModule.func_1628(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1629,
                (instance, args) -> {
                    CompiledModule.func_1629(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1630,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1630(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1631,
                (instance, args) -> {
                    CompiledModule.func_1631(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1632,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1632(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1633,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1633(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1634,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1634(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1635,
                (instance, args) -> {
                    CompiledModule.func_1635(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1636,
                (instance, args) -> {
                    CompiledModule.func_1636(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1637,
                (instance, args) -> {
                    CompiledModule.func_1637(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1638,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1638(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1639,
                (instance, args) -> {
                    CompiledModule.func_1639(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1640,
                (instance, args) -> {
                    CompiledModule.func_1640(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1641,
                (instance, args) -> {
                    CompiledModule.func_1641(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1642,
                (instance, args) -> {
                    CompiledModule.func_1642(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1643,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1643(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1644,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1644(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1645,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1645(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1646,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1646(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1647,
                (instance, args) -> {
                    CompiledModule.func_1647(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1648,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1648(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1649,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1649(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1650,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(CompiledModule.func_1650(instance.memory(), instance))
                    };
                });
        funcs.put(
                1651,
                (instance, args) -> {
                    CompiledModule.func_1651(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1652,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1652(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1653,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1653(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1654,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1654(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1655,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1655(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1656,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1656(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1657,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1657(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1658,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1658(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1659,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1659(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1660,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1660(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1661,
                (instance, args) -> {
                    CompiledModule.func_1661(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1662,
                (instance, args) -> {
                    CompiledModule.func_1662(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            args[6].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1663,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1663(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1664,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1664(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1665,
                (instance, args) -> {
                    CompiledModule.func_1665(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1666,
                (instance, args) -> {
                    CompiledModule.func_1666(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1667,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1667(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1668,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1668(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1669,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1669(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1670,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1670(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1671,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1671(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1672,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1672(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1673,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1673(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1674,
                (instance, args) -> {
                    CompiledModule.func_1674(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1675,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1675(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1676,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1676(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1677,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1677(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1678,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1678(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1679,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1679(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1680,
                (instance, args) -> {
                    CompiledModule.func_1680(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1681,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1681(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1682,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1682(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1683,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1683(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1684,
                (instance, args) -> {
                    CompiledModule.func_1684(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1685,
                (instance, args) -> {
                    CompiledModule.func_1685(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1686,
                (instance, args) -> {
                    CompiledModule.func_1686(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1687,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1687(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1688,
                (instance, args) -> {
                    CompiledModule.func_1688(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1689,
                (instance, args) -> {
                    CompiledModule.func_1689(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1690,
                (instance, args) -> {
                    CompiledModule.func_1690(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1691,
                (instance, args) -> {
                    CompiledModule.func_1691(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1692,
                (instance, args) -> {
                    CompiledModule.func_1692(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1693,
                (instance, args) -> {
                    CompiledModule.func_1693(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1694,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1694(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1695,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1695(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1696,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1696(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1697,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1697(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1698,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1698(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1699,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1699(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1700,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(CompiledModule.func_1700(instance.memory(), instance))
                    };
                });
        funcs.put(
                1701,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1701(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1702,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1702(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1703,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1703(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1704,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1704(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1705,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1705(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1706,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1706(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1707,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1707(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1708,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1708(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1709,
                (instance, args) -> {
                    CompiledModule.func_1709(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1710,
                (instance, args) -> {
                    CompiledModule.func_1710(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1711,
                (instance, args) -> {
                    CompiledModule.func_1711(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1712,
                (instance, args) -> {
                    CompiledModule.func_1712(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1713,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1713(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1714,
                (instance, args) -> {
                    CompiledModule.func_1714(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1715,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1715(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1716,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1716(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1717,
                (instance, args) -> {
                    CompiledModule.func_1717(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1718,
                (instance, args) -> {
                    CompiledModule.func_1718(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1719,
                (instance, args) -> {
                    CompiledModule.func_1719(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1720,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1720(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1721,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1721(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1722,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1722(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1723,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1723(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1724,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1724(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1725,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1725(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1726,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1726(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1727,
                (instance, args) -> {
                    CompiledModule.func_1727(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1728,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1728(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1729,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1729(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1730,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1730(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1731,
                (instance, args) -> {
                    CompiledModule.func_1731(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1732,
                (instance, args) -> {
                    CompiledModule.func_1732(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1733,
                (instance, args) -> {
                    CompiledModule.func_1733(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1734,
                (instance, args) -> {
                    CompiledModule.func_1734(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1735,
                (instance, args) -> {
                    CompiledModule.func_1735(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1736,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1736(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1737,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1737(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1738,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1738(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1739,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1739(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1740,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1740(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1741,
                (instance, args) -> {
                    CompiledModule.func_1741(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1742,
                (instance, args) -> {
                    CompiledModule.func_1742(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1743,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1743(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1744,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1744(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1745,
                (instance, args) -> {
                    CompiledModule.func_1745(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1746,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1746(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1747,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1747(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1748,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1748(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1749,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1749(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1750,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1750(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1751,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1751(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1752,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1752(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1753,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1753(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1754,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1754(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1755,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1755(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1756,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1756(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1757,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1757(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1758,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1758(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1759,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1759(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1760,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1760(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1761,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1761(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1762,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1762(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1763,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1763(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1764,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1764(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1765,
                (instance, args) -> {
                    CompiledModule.func_1765(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1766,
                (instance, args) -> {
                    CompiledModule.func_1766(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1767,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1767(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1768,
                (instance, args) -> {
                    CompiledModule.func_1768(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1769,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1769(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1770,
                (instance, args) -> {
                    CompiledModule.func_1770(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1771,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1771(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1772,
                (instance, args) -> {
                    CompiledModule.func_1772(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1773,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1773(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1774,
                (instance, args) -> {
                    CompiledModule.func_1774(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1775,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1775(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1776,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1776(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1777,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1777(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1778,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1778(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1779,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1779(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1780,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1780(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1781,
                (instance, args) -> {
                    CompiledModule.func_1781(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1782,
                (instance, args) -> {
                    CompiledModule.func_1782(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1783,
                (instance, args) -> {
                    CompiledModule.func_1783(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1784,
                (instance, args) -> {
                    CompiledModule.func_1784(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1785,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1785(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1786,
                (instance, args) -> {
                    CompiledModule.func_1786(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1787,
                (instance, args) -> {
                    CompiledModule.func_1787(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1788,
                (instance, args) -> {
                    CompiledModule.func_1788(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1789,
                (instance, args) -> {
                    CompiledModule.func_1789(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1790,
                (instance, args) -> {
                    CompiledModule.func_1790(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1791,
                (instance, args) -> {
                    CompiledModule.func_1791(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1792,
                (instance, args) -> {
                    CompiledModule.func_1792(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1793,
                (instance, args) -> {
                    CompiledModule.func_1793(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1794,
                (instance, args) -> {
                    CompiledModule.func_1794(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1795,
                (instance, args) -> {
                    CompiledModule.func_1795(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1796,
                (instance, args) -> {
                    CompiledModule.func_1796(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            args[5].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1797,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1797(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1798,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(CompiledModule.func_1798(instance.memory(), instance))
                    };
                });
        funcs.put(
                1799,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1799(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1800,
                (instance, args) -> {
                    CompiledModule.func_1800(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1801,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1801(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1802,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1802(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1803,
                (instance, args) -> {
                    CompiledModule.func_1803(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1804,
                (instance, args) -> {
                    CompiledModule.func_1804(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1805,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1805(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1806,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1806(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1807,
                (instance, args) -> {
                    CompiledModule.func_1807(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1808,
                (instance, args) -> {
                    CompiledModule.func_1808(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1809,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1809(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1810,
                (instance, args) -> {
                    CompiledModule.func_1810(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1811,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1811(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1812,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1812(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1813,
                (instance, args) -> {
                    CompiledModule.func_1813(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1814,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(CompiledModule.func_1814(instance.memory(), instance))
                    };
                });
        funcs.put(
                1815,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1815(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1816,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1816(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1817,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1817(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1818,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1818(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1819,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1819(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1820,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1820(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1821,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1821(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1822,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1822(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1823,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1823(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1824,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1824(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1825,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1825(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1826,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1826(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1827,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1827(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1828,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1828(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        args[5].asInt(),
                                        args[6].asInt(),
                                        args[7].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1829,
                (instance, args) -> {
                    CompiledModule.func_1829(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1830,
                (instance, args) -> {
                    CompiledModule.func_1830(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1831,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1831(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1832,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1832(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1833,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1833(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1834,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1834(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1835,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1835(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1836,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1836(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1837,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1837(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1838,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1838(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1839,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1839(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1840,
                (instance, args) -> {
                    CompiledModule.func_1840(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1841,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1841(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1842,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1842(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1843,
                (instance, args) -> {
                    CompiledModule.func_1843(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1844,
                (instance, args) -> {
                    CompiledModule.func_1844(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1845,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1845(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1846,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1846(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1847,
                (instance, args) -> {
                    CompiledModule.func_1847(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1848,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1848(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1849,
                (instance, args) -> {
                    CompiledModule.func_1849(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1850,
                (instance, args) -> {
                    CompiledModule.func_1850(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1851,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1851(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1852,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1852(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1853,
                (instance, args) -> {
                    CompiledModule.func_1853(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1854,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1854(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1855,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1855(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1856,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1856(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1857,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1857(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1858,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1858(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1859,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1859(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1860,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1860(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1861,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1861(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1862,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1862(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1863,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1863(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1864,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1864(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1865,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1865(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1866,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1866(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1867,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1867(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1868,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1868(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1869,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1869(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1870,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1870(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1871,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1871(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1872,
                (instance, args) -> {
                    CompiledModule.func_1872(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1873,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1873(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1874,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1874(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1875,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1875(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1876,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1876(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1877,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1877(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1878,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1878(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1879,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1879(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1880,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(CompiledModule.func_1880(instance.memory(), instance))
                    };
                });
        funcs.put(
                1881,
                (instance, args) -> {
                    CompiledModule.func_1881(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1882,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1882(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1883,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1883(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1884,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1884(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1885,
                (instance, args) -> {
                    CompiledModule.func_1885(args[0].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1886,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1886(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1887,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1887(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1888,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1888(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1889,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1889(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1890,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1890(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1891,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1891(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1892,
                (instance, args) -> {
                    return new Value[] {
                        Value.fromDouble(
                                CompiledModule.func_1892(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1893,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1893(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1894,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1894(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1895,
                (instance, args) -> {
                    CompiledModule.func_1895(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1896,
                (instance, args) -> {
                    CompiledModule.func_1896(
                            args[0].asInt(),
                            args[1].asInt(),
                            args[2].asInt(),
                            args[3].asInt(),
                            args[4].asInt(),
                            instance.memory(),
                            instance);
                    return new Value[] {};
                });
        funcs.put(
                1897,
                (instance, args) -> {
                    CompiledModule.func_1897(instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1898,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1898(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1899,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1899(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1900,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1900(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1901,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1901(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1902,
                (instance, args) -> {
                    CompiledModule.func_1902(
                            args[0].asInt(), args[1].asInt(), instance.memory(), instance);
                    return new Value[] {};
                });
        funcs.put(
                1903,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1903(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1904,
                (instance, args) -> {
                    return new Value[] {
                        Value.fromDouble(
                                CompiledModule.func_1904(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1905,
                (instance, args) -> {
                    return new Value[] {
                        Value.fromDouble(
                                CompiledModule.func_1905(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1906,
                (instance, args) -> {
                    return new Value[] {
                        Value.fromDouble(
                                CompiledModule.func_1906(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1907,
                (instance, args) -> {
                    return new Value[] {
                        Value.fromDouble(
                                CompiledModule.func_1907(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        args[3].asInt(),
                                        args[4].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1908,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1908(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1909,
                (instance, args) -> {
                    return new Value[] {
                        Value.fromDouble(
                                CompiledModule.func_1909(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1910,
                (instance, args) -> {
                    return new Value[] {
                        Value.fromDouble(
                                CompiledModule.func_1910(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1911,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1911(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1912,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1912(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1913,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1913(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1914,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1914(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1915,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1915(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1916,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1916(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1917,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1917(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1918,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1918(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1919,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1919(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1920,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1920(
                                        args[0].asInt(), instance.memory(), instance))
                    };
                });
        funcs.put(
                1921,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1921(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        args[2].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
        funcs.put(
                1922,
                (instance, args) -> {
                    return new Value[] {
                        Value.i32(
                                CompiledModule.func_1922(
                                        args[0].asInt(),
                                        args[1].asInt(),
                                        instance.memory(),
                                        instance))
                    };
                });
    }

    @Override
    public Value[] call(int funcId, Value[] args) throws ChicoryException {
        // generated with:
        //        private String adaptParams(int funcId, FunctionType t) {
        //            var base = "CompiledModule.func_" + funcId + "(";
        //            for (var i = 0; i < t.params().size(); i++) {
        //                base += "args[" + i + "].asInt(),";
        //            }
        //            base += " instance.memory(), instance)";
        //            return base;
        //        }
        //
        //        private String generateReturn(int funcId, FunctionType t) {
        //            if (t.returns().size() > 0) {
        //                return "return new Value[] { " + adaptParams(funcId, t) + "};";
        //            } else {
        //                return adaptParams(funcId, t) + ";\nreturn new Value[] {};";
        //            }
        //        }
        //        for (var i = 0; i < instance.functionCount(); i++) {
        //            System.out.println(
        //                    "case "
        //                            + i
        //                            + ":\n"
        //                            + generateReturn(i, instance.type(instance.functionType(i))));
        //        }
        return funcs.get(funcId).apply(instance, args);
    }
}
