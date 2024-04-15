// package com.dylibso.chicory.runtime;
//
// import com.dylibso.chicory.testing.ChicoryTest;
// import com.dylibso.chicory.testing.TestModule;
// import com.dylibso.chicory.wasm.exceptions.ChicoryException;
// import com.dylibso.chicory.wasm.types.Value;
// import org.junit.jupiter.api.MethodOrderer;
// import org.junit.jupiter.api.Order;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.TestInstance;
// import org.junit.jupiter.api.TestMethodOrder;
//
// import java.io.File;
//
// import static org.junit.jupiter.api.Assertions.*;
//
// @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
// @ChicoryTest()
// public class DummyTest2 {
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
//    public static TestModule Ot = TestModule.of(new
// File("target/compiled-wast/linking/spec.17.wasm")).build().instantiate(SpecV1DummyHostFuncs.Ot());
//
//    public static Instance OtInstance = Ot.instance();
//    @Test()
//    @Order(19)
//    public void test19() {
//        ExportFunction varCall = MtInstance.export("call");
//        var results = varCall.apply(Value.i32(Integer.parseUnsignedInt("2")));
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
// }
