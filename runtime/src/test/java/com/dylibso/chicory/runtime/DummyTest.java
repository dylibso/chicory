// package com.dylibso.chicory.runtime;
//
// import java.io.File;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.MethodOrderer;
// import org.junit.jupiter.api.TestMethodOrder;
// import org.junit.jupiter.api.Order;
// import org.junit.jupiter.api.TestInstance;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import com.dylibso.chicory.testing.ChicoryTest;
// import com.dylibso.chicory.testing.TestModule;
// import com.dylibso.chicory.wasm.exceptions.ChicoryException;
// import com.dylibso.chicory.wasm.types.Value;
//
// @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
// @ChicoryTest()
// public class DummyTest {
//
//    public static TestModule Mf = TestModule.of(new
// File("target/compiled-wast/linking/spec.0.wasm")).build().instantiate(SpecV1DummyHostFuncs.Mf());
//
//    public static Instance MfInstance = Mf.instance();
//
//    public static TestModule Nf = TestModule.of(new
// File("target/compiled-wast/linking/spec.1.wasm")).build().instantiate(SpecV1DummyHostFuncs.Nf());
//
//    public static Instance NfInstance = Nf.instance();
//
//    @Test()
//    @Order(0)
//    public void test0() {
//        ExportFunction varCall = MfInstance.export("call");
//        var results = varCall.apply();
//        assertEquals(Integer.parseUnsignedInt("2"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(1)
//    public void test1() {
//        ExportFunction varMfCall = NfInstance.export("Mf.call");
//        var results = varMfCall.apply();
//        assertEquals(Integer.parseUnsignedInt("2"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(2)
//    public void test2() {
//        ExportFunction varCall = NfInstance.export("call");
//        var results = varCall.apply();
//        assertEquals(Integer.parseUnsignedInt("3"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(3)
//    public void test3() {
//        ExportFunction varCallMfCall = NfInstance.export("call Mf.call");
//        var results = varCallMfCall.apply();
//        assertEquals(Integer.parseUnsignedInt("2"), results[0].asInt());
//    }
//
//    public static TestModule testModule2 = TestModule.of(new
// File("target/compiled-wast/linking/spec.2.wasm")).build().instantiate(SpecV1DummyHostFuncs.fallback());
//
//    public static Instance testModule2Instance = testModule2.instance();
//
//    public static TestModule Mg = TestModule.of(new
// File("target/compiled-wast/linking/spec.5.wasm")).build().instantiate(SpecV1DummyHostFuncs.Mg());
//
//    public static Instance MgInstance = Mg.instance();
//
//    public static TestModule Ng = TestModule.of(new
// File("target/compiled-wast/linking/spec.6.wasm")).build().instantiate(SpecV1DummyHostFuncs.Ng());
//
//    public static Instance NgInstance = Ng.instance();
//
//    @Test()
//    @Order(4)
//    public void test4() {
//        ExportFunction varGlob = MgInstance.export("glob");
//        var results = varGlob.apply();
//        assertEquals(Integer.parseUnsignedInt("42"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(5)
//    public void test5() {
//        ExportFunction varMgGlob = NgInstance.export("Mg.glob");
//        var results = varMgGlob.apply();
//        assertEquals(Integer.parseUnsignedInt("42"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(6)
//    public void test6() {
//        ExportFunction varGlob = NgInstance.export("glob");
//        var results = varGlob.apply();
//        assertEquals(Integer.parseUnsignedInt("43"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(7)
//    public void test7() {
//        ExportFunction varGet = MgInstance.export("get");
//        var results = varGet.apply();
//        assertEquals(Integer.parseUnsignedInt("42"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(8)
//    public void test8() {
//        ExportFunction varMgGet = NgInstance.export("Mg.get");
//        var results = varMgGet.apply();
//        assertEquals(Integer.parseUnsignedInt("42"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(9)
//    public void test9() {
//        ExportFunction varGet = NgInstance.export("get");
//        var results = varGet.apply();
//        assertEquals(Integer.parseUnsignedInt("43"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(10)
//    public void test10() {
//        ExportFunction varMutGlob = MgInstance.export("mut_glob");
//        var results = varMutGlob.apply();
//        assertEquals(Integer.parseUnsignedInt("142"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(11)
//    public void test11() {
//        ExportFunction varMgMutGlob = NgInstance.export("Mg.mut_glob");
//        var results = varMgMutGlob.apply();
//        assertEquals(Integer.parseUnsignedInt("142"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(12)
//    public void test12() {
//        ExportFunction varGetMut = MgInstance.export("get_mut");
//        var results = varGetMut.apply();
//        assertEquals(Integer.parseUnsignedInt("142"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(13)
//    public void test13() {
//        ExportFunction varMgGetMut = NgInstance.export("Mg.get_mut");
//        var results = varMgGetMut.apply();
//        assertEquals(Integer.parseUnsignedInt("142"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(14)
//    public void test14() {
//        ExportFunction varSetMut = MgInstance.export("set_mut");
//        var results = varSetMut.apply(Value.i32(Integer.parseUnsignedInt("241")));
//    }
//
//    @Test()
//    @Order(15)
//    public void test15() {
//        ExportFunction varMutGlob = MgInstance.export("mut_glob");
//        var results = varMutGlob.apply();
//        assertEquals(Integer.parseUnsignedInt("241"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(16)
//    public void test16() {
//        ExportFunction varMgMutGlob = NgInstance.export("Mg.mut_glob");
//        var results = varMgMutGlob.apply();
//        assertEquals(Integer.parseUnsignedInt("241"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(17)
//    public void test17() {
//        ExportFunction varGetMut = MgInstance.export("get_mut");
//        var results = varGetMut.apply();
//        assertEquals(Integer.parseUnsignedInt("241"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(18)
//    public void test18() {
//        ExportFunction varMgGetMut = NgInstance.export("Mg.get_mut");
//        var results = varMgGetMut.apply();
//        assertEquals(Integer.parseUnsignedInt("241"), results[0].asInt());
//    }
//
//    public static TestModule Mref_ex = TestModule.of(new
// File("target/compiled-wast/linking/spec.9.wasm")).build().instantiate(SpecV1DummyHostFuncs.fallback());
//
//    public static Instance Mref_exInstance = Mref_ex.instance();
//
//    public static TestModule Mref_im = TestModule.of(new
// File("target/compiled-wast/linking/spec.10.wasm")).build().instantiate(SpecV1DummyHostFuncs.fallback());
//
//    public static Instance Mref_imInstance = Mref_im.instance();
//
//    public static TestModule Mt = TestModule.of(new
// File("target/compiled-wast/linking/spec.15.wasm")).build().instantiate(SpecV1DummyHostFuncs.Mt());
//
//    public static Instance MtInstance = Mt.instance();
//
//    public static TestModule Nt = TestModule.of(new
// File("target/compiled-wast/linking/spec.16.wasm")).build().instantiate(SpecV1DummyHostFuncs.Nt());
//
//    public static Instance NtInstance = Nt.instance();
//
//    @Test()
//    @Order(19)
//    public void test19() {
//        ExportFunction varCall = MtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("4"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(20)
//    public void test20() {
//        ExportFunction varMtCall = NtInstance.export("Mt.call");
//        var results = varMtCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("4"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(21)
//    public void test21() {
//        ExportFunction varCall = NtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(22)
//    public void test22() {
//        ExportFunction varCallMtCall = NtInstance.export("call Mt.call");
//        var results = varCallMtCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("4"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(23)
//    public void test23() {
//        ExportFunction varCall = MtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("1"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(24)
//    public void test24() {
//        ExportFunction varMtCall = NtInstance.export("Mt.call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varMtCall.apply(Value.i32(Integer.parseUnsignedInt("1"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(25)
//    public void test25() {
//        ExportFunction varCall = NtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(26)
//    public void test26() {
//        ExportFunction varCallMtCall = NtInstance.export("call Mt.call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCallMtCall.apply(Value.i32(Integer.parseUnsignedInt("1"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(27)
//    public void test27() {
//        ExportFunction varCall = MtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("0"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(28)
//    public void test28() {
//        ExportFunction varMtCall = NtInstance.export("Mt.call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varMtCall.apply(Value.i32(Integer.parseUnsignedInt("0"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(29)
//    public void test29() {
//        ExportFunction varCall = NtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(30)
//    public void test30() {
//        ExportFunction varCallMtCall = NtInstance.export("call Mt.call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCallMtCall.apply(Value.i32(Integer.parseUnsignedInt("0"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(31)
//    public void test31() {
//        ExportFunction varCall = MtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("20"))));
//        assertTrue(exception.getMessage().contains("undefined element"), "'" +
// exception.getMessage() + "' doesn't contains: 'undefined element");
//    }
//
//    @Test()
//    @Order(32)
//    public void test32() {
//        ExportFunction varMtCall = NtInstance.export("Mt.call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varMtCall.apply(Value.i32(Integer.parseUnsignedInt("20"))));
//        assertTrue(exception.getMessage().contains("undefined element"), "'" +
// exception.getMessage() + "' doesn't contains: 'undefined element");
//    }
//
//    @Test()
//    @Order(33)
//    public void test33() {
//        ExportFunction varCall = NtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("7"))));
//        assertTrue(exception.getMessage().contains("undefined element"), "'" +
// exception.getMessage() + "' doesn't contains: 'undefined element");
//    }
//
//    @Test()
//    @Order(34)
//    public void test34() {
//        ExportFunction varCallMtCall = NtInstance.export("call Mt.call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCallMtCall.apply(Value.i32(Integer.parseUnsignedInt("20"))));
//        assertTrue(exception.getMessage().contains("undefined element"), "'" +
// exception.getMessage() + "' doesn't contains: 'undefined element");
//    }
//
//    @Test()
//    @Order(35)
//    public void test35() {
//        ExportFunction varCall = NtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("3")));
//        assertEquals(Integer.parseUnsignedInt("4294967292"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(36)
//    public void test36() {
//        ExportFunction varCall = NtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("4"))));
//        assertTrue(exception.getMessage().contains("indirect call type mismatch"), "'" +
// exception.getMessage() + "' doesn't contains: 'indirect call type mismatch");
//    }
//
//    public static TestModule Ot = TestModule.of(new
// File("target/compiled-wast/linking/spec.17.wasm")).build().instantiate(SpecV1DummyHostFuncs.Ot());
//
//    public static Instance OtInstance = null;
//
//    @Order(36)
//    public void instantiateOt() {
//        OtInstance = Ot.instance().initialize(true);
//    }
//
//
//    @Test()
//    @Order(37)
//    public void test37() {
//        ExportFunction varCall = MtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("3")));
//        assertEquals(Integer.parseUnsignedInt("4"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(38)
//    public void test38() {
//        ExportFunction varMtCall = NtInstance.export("Mt.call");
//        var results = varMtCall.apply(Value.i32(Integer.parseUnsignedInt("3")));
//        assertEquals(Integer.parseUnsignedInt("4"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(39)
//    public void test39() {
//        ExportFunction varCallMtCall = NtInstance.export("call Mt.call");
//        var results = varCallMtCall.apply(Value.i32(Integer.parseUnsignedInt("3")));
//        assertEquals(Integer.parseUnsignedInt("4"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(40)
//    public void test40() {
//        // Ot instantiation breaks MtInstance initialization, we should be able to order:
//        // - creation of the instance
//        // - initialization
//        // and order them properly in the tests
//        // we can generate a mock assertion just to fill the variable for the remaining tests
//
//        ExportFunction varCall = OtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("3")));
//        assertEquals(Integer.parseUnsignedInt("4"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(41)
//    public void test41() {
//        ExportFunction varCall = MtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("4294967292"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(42)
//    public void test42() {
//        ExportFunction varMtCall = NtInstance.export("Mt.call");
//        var results = varMtCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("4294967292"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(43)
//    public void test43() {
//        ExportFunction varCall = NtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(44)
//    public void test44() {
//        ExportFunction varCallMtCall = NtInstance.export("call Mt.call");
//        var results = varCallMtCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("4294967292"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(45)
//    public void test45() {
//        ExportFunction varCall = OtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("4294967292"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(46)
//    public void test46() {
//        ExportFunction varCall = MtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("6"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(47)
//    public void test47() {
//        ExportFunction varMtCall = NtInstance.export("Mt.call");
//        var results = varMtCall.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("6"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(48)
//    public void test48() {
//        ExportFunction varCall = NtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(49)
//    public void test49() {
//        ExportFunction varCallMtCall = NtInstance.export("call Mt.call");
//        var results = varCallMtCall.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("6"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(50)
//    public void test50() {
//        ExportFunction varCall = OtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("6"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(51)
//    public void test51() {
//        ExportFunction varCall = MtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("0"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(52)
//    public void test52() {
//        ExportFunction varMtCall = NtInstance.export("Mt.call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varMtCall.apply(Value.i32(Integer.parseUnsignedInt("0"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(53)
//    public void test53() {
//        ExportFunction varCall = NtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(54)
//    public void test54() {
//        ExportFunction varCallMtCall = NtInstance.export("call Mt.call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCallMtCall.apply(Value.i32(Integer.parseUnsignedInt("0"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(55)
//    public void test55() {
//        ExportFunction varCall = OtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("0"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(56)
//    public void test56() {
//        ExportFunction varCall = OtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("20"))));
//        assertTrue(exception.getMessage().contains("undefined element"), "'" +
// exception.getMessage() + "' doesn't contains: 'undefined element");
//    }
//
//    public static TestModule testModule10 = TestModule.of(new
// File("target/compiled-wast/linking/spec.18.wasm")).build().instantiate(SpecV1DummyHostFuncs.testModule10());
//
//    public static Instance testModule10Instance = testModule10.instance();
//
//    public static TestModule G1 = TestModule.of(new
// File("target/compiled-wast/linking/spec.19.wasm")).build().instantiate(SpecV1DummyHostFuncs.fallback());
//
//    public static Instance G1Instance = G1.instance();
//
//    public static TestModule G2 = TestModule.of(new
// File("target/compiled-wast/linking/spec.20.wasm")).build().instantiate(SpecV1DummyHostFuncs.G2());
//
//    public static Instance G2Instance = G2.instance();
//
//    @Test()
//    @Order(57)
//    public void test57() {
//        ExportFunction varG = G2Instance.export("g");
//        var results = varG.apply();
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(59)
//    public void test59() {
//        ExportFunction varCall = MtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("7"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(61)
//    public void test61() {
//        ExportFunction varCall = MtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("7")));
//        assertEquals(Integer.parseUnsignedInt("0"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(62)
//    public void test62() {
//        ExportFunction varCall = MtInstance.export("call");
//        var exception = assertThrows(ChicoryException.class, () ->
// varCall.apply(Value.i32(Integer.parseUnsignedInt("8"))));
//        assertTrue(exception.getMessage().contains("uninitialized element"), "'" +
// exception.getMessage() + "' doesn't contains: 'uninitialized element");
//    }
//
//    @Test()
//    @Order(64)
//    public void test64() {
//        ExportFunction varCall = MtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("7")));
//        assertEquals(Integer.parseUnsignedInt("0"), results[0].asInt());
//    }
//
//    public static TestModule Mtable_ex = TestModule.of(new
// File("target/compiled-wast/linking/spec.25.wasm")).build().instantiate(SpecV1DummyHostFuncs.fallback());
//
//    public static Instance Mtable_exInstance = Mtable_ex.instance();
//
//    public static TestModule testModule14 = TestModule.of(new
// File("target/compiled-wast/linking/spec.26.wasm")).build().instantiate(SpecV1DummyHostFuncs.fallback());
//
//    public static Instance testModule14Instance = testModule14.instance();
//
//    public static TestModule Mm = TestModule.of(new
// File("target/compiled-wast/linking/spec.29.wasm")).build().instantiate(SpecV1DummyHostFuncs.fallback());
//
//    public static Instance MmInstance = Mm.instance();
//
//    public static TestModule Nm = TestModule.of(new
// File("target/compiled-wast/linking/spec.30.wasm")).build().instantiate(SpecV1DummyHostFuncs.Nm());
//
//    public static Instance NmInstance = Nm.instance();
//
//    @Test()
//    @Order(65)
//    public void test65() {
//        ExportFunction varLoad = MmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("12")));
//        assertEquals(Integer.parseUnsignedInt("2"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(66)
//    public void test66() {
//        ExportFunction varMmLoad = NmInstance.export("Mm.load");
//        var results = varMmLoad.apply(Value.i32(Integer.parseUnsignedInt("12")));
//        assertEquals(Integer.parseUnsignedInt("2"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(67)
//    public void test67() {
//        ExportFunction varLoad = NmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("12")));
//        assertEquals(Integer.parseUnsignedInt("242"), results[0].asInt());
//    }
//
//    public static TestModule Om = TestModule.of(new
// File("target/compiled-wast/linking/spec.31.wasm")).build().instantiate(SpecV1DummyHostFuncs.Om());
//
//    public static Instance OmInstance = Om.instance();
//
//    @Test()
//    @Order(68)
//    public void test68() {
//        ExportFunction varLoad = MmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("12")));
//        assertEquals(Integer.parseUnsignedInt("167"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(69)
//    public void test69() {
//        ExportFunction varMmLoad = NmInstance.export("Mm.load");
//        var results = varMmLoad.apply(Value.i32(Integer.parseUnsignedInt("12")));
//        assertEquals(Integer.parseUnsignedInt("167"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(70)
//    public void test70() {
//        ExportFunction varLoad = NmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("12")));
//        assertEquals(Integer.parseUnsignedInt("242"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(71)
//    public void test71() {
//        ExportFunction varLoad = OmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("12")));
//        assertEquals(Integer.parseUnsignedInt("167"), results[0].asInt());
//    }
//
//    public static TestModule testModule18 = TestModule.of(new
// File("target/compiled-wast/linking/spec.32.wasm")).build().instantiate(SpecV1DummyHostFuncs.testModule18());
//
//    public static Instance testModule18Instance = testModule18.instance();
//
//    public static TestModule Pm = TestModule.of(new
// File("target/compiled-wast/linking/spec.34.wasm")).build().instantiate(SpecV1DummyHostFuncs.Pm());
//
//    public static Instance PmInstance = Pm.instance();
//
//    @Test()
//    @Order(73)
//    public void test73() {
//        ExportFunction varGrow = PmInstance.export("grow");
//        var results = varGrow.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("1"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(74)
//    public void test74() {
//        ExportFunction varGrow = PmInstance.export("grow");
//        var results = varGrow.apply(Value.i32(Integer.parseUnsignedInt("2")));
//        assertEquals(Integer.parseUnsignedInt("1"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(75)
//    public void test75() {
//        ExportFunction varGrow = PmInstance.export("grow");
//        var results = varGrow.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("3"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(76)
//    public void test76() {
//        ExportFunction varGrow = PmInstance.export("grow");
//        var results = varGrow.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("3"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(77)
//    public void test77() {
//        ExportFunction varGrow = PmInstance.export("grow");
//        var results = varGrow.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("4"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(78)
//    public void test78() {
//        ExportFunction varGrow = PmInstance.export("grow");
//        var results = varGrow.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(79)
//    public void test79() {
//        ExportFunction varGrow = PmInstance.export("grow");
//        var results = varGrow.apply(Value.i32(Integer.parseUnsignedInt("1")));
//        assertEquals(Integer.parseUnsignedInt("4294967295"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(80)
//    public void test80() {
//        ExportFunction varGrow = PmInstance.export("grow");
//        var results = varGrow.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("5"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(81)
//    public void test81() {
//        ExportFunction varLoad = MmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("0"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(83)
//    public void test83() {
//        ExportFunction varLoad = MmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("97"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(84)
//    public void test84() {
//        ExportFunction varLoad = MmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("327670")));
//        assertEquals(Integer.parseUnsignedInt("0"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(86)
//    public void test86() {
//        ExportFunction varLoad = MmInstance.export("load");
//        var results = varLoad.apply(Value.i32(Integer.parseUnsignedInt("0")));
//        assertEquals(Integer.parseUnsignedInt("97"), results[0].asInt());
//    }
//
//    public static TestModule Ms = TestModule.of(new
// File("target/compiled-wast/linking/spec.38.wasm")).build().instantiate(SpecV1DummyHostFuncs.fallback());
//
//    public static Instance MsInstance = Ms.instance();
//
//    @Test()
//    @Order(88)
//    public void test88() {
//        ExportFunction varGetMemory0 = MsInstance.export("get memory[0]");
//        var results = varGetMemory0.apply();
//        assertEquals(Integer.parseUnsignedInt("104"), results[0].asInt());
//    }
//
//    @Test()
//    @Order(89)
//    public void test89() {
//        ExportFunction varGetTable0 = MsInstance.export("get table[0]");
//        var results = varGetTable0.apply();
//        assertEquals(Integer.parseUnsignedInt("57005"), results[0].asInt());
//    }
// }
