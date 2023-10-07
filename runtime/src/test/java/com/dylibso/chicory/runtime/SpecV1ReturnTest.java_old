
  package com.dylibso.chicory.runtime;

  import java.math.BigInteger;
  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertThrows;
  import com.dylibso.chicory.wasm.types.Value;
  import com.dylibso.chicory.wasm.types.ValueType;
  import org.junit.Test;

  public class SpecV1ReturnTest {

    public static long longVal(String v) {
      return new BigInteger(v).longValue();
    }

    public static float floatVal(String s) {
      return Float.intBitsToFloat(Integer.parseInt(s));
    }

    public static double doubleVal(String s) {
      return Double.longBitsToDouble(longVal(s));
    }

  @Test
  public void testReturn0Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.0.wasm").instantiate();
    var typei32 = instance.getExport("type-i32");
    assertEquals(null, typei32.apply());
    var typei64 = instance.getExport("type-i64");
    assertEquals(null, typei64.apply());
    var typef32 = instance.getExport("type-f32");
    assertEquals(null, typef32.apply());
    var typef64 = instance.getExport("type-f64");
    assertEquals(null, typef64.apply());
    var typei32value = instance.getExport("type-i32-value");
    assertEquals((int)(1L & 0xFFFFFFFFL), typei32value.apply().asInt());
    var typei64value = instance.getExport("type-i64-value");
    assertEquals(longVal("2"), typei64value.apply().asLong());
    var typef32value = instance.getExport("type-f32-value");
    assertEquals(floatVal("1077936128"), typef32value.apply().asFloat(), 0.0);
    var typef64value = instance.getExport("type-f64-value");
    assertEquals(doubleVal("4616189618054758400"), typef64value.apply().asDouble(), 0.0);
    var nullary = instance.getExport("nullary");
    assertEquals(null, nullary.apply());
    var unary = instance.getExport("unary");
    assertEquals(doubleVal("4613937818241073152"), unary.apply().asDouble(), 0.0);
    var asfuncfirst = instance.getExport("as-func-first");
    assertEquals((int)(1L & 0xFFFFFFFFL), asfuncfirst.apply().asInt());
    var asfuncmid = instance.getExport("as-func-mid");
    assertEquals((int)(2L & 0xFFFFFFFFL), asfuncmid.apply().asInt());
    var asfunclast = instance.getExport("as-func-last");
    assertEquals(null, asfunclast.apply());
    var asfuncvalue = instance.getExport("as-func-value");
    assertEquals((int)(3L & 0xFFFFFFFFL), asfuncvalue.apply().asInt());
    var asblockfirst = instance.getExport("as-block-first");
    assertEquals(null, asblockfirst.apply());
    var asblockmid = instance.getExport("as-block-mid");
    assertEquals(null, asblockmid.apply());
    var asblocklast = instance.getExport("as-block-last");
    assertEquals(null, asblocklast.apply());
    var asblockvalue = instance.getExport("as-block-value");
    assertEquals((int)(2L & 0xFFFFFFFFL), asblockvalue.apply().asInt());
    var asloopfirst = instance.getExport("as-loop-first");
    assertEquals((int)(3L & 0xFFFFFFFFL), asloopfirst.apply().asInt());
    var asloopmid = instance.getExport("as-loop-mid");
    assertEquals((int)(4L & 0xFFFFFFFFL), asloopmid.apply().asInt());
    var aslooplast = instance.getExport("as-loop-last");
    assertEquals((int)(5L & 0xFFFFFFFFL), aslooplast.apply().asInt());
    var asbrvalue = instance.getExport("as-br-value");
    assertEquals((int)(9L & 0xFFFFFFFFL), asbrvalue.apply().asInt());
    var asbrifcond = instance.getExport("as-br_if-cond");
    assertEquals(null, asbrifcond.apply());
    var asbrifvalue = instance.getExport("as-br_if-value");
    assertEquals((int)(8L & 0xFFFFFFFFL), asbrifvalue.apply().asInt());
    var asbrifvaluecond = instance.getExport("as-br_if-value-cond");
    assertEquals((int)(9L & 0xFFFFFFFFL), asbrifvaluecond.apply().asInt());
    var asbrtableindex = instance.getExport("as-br_table-index");
    assertEquals(longVal("9"), asbrtableindex.apply().asLong());
    var asbrtablevalue = instance.getExport("as-br_table-value");
    assertEquals((int)(10L & 0xFFFFFFFFL), asbrtablevalue.apply().asInt());
    var asbrtablevalueindex = instance.getExport("as-br_table-value-index");
    assertEquals((int)(11L & 0xFFFFFFFFL), asbrtablevalueindex.apply().asInt());
    var asreturnvalue = instance.getExport("as-return-value");
    assertEquals(longVal("7"), asreturnvalue.apply().asLong());
    var asifcond = instance.getExport("as-if-cond");
    assertEquals((int)(2L & 0xFFFFFFFFL), asifcond.apply().asInt());
    var asifthen = instance.getExport("as-if-then");
    assertEquals((int)(3L & 0xFFFFFFFFL), asifthen.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
    assertEquals((int)(6L & 0xFFFFFFFFL), asifthen.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
    var asifelse = instance.getExport("as-if-else");
    assertEquals((int)(4L & 0xFFFFFFFFL), asifelse.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
    assertEquals((int)(6L & 0xFFFFFFFFL), asifelse.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
    var asselectfirst = instance.getExport("as-select-first");
    assertEquals((int)(5L & 0xFFFFFFFFL), asselectfirst.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
    assertEquals((int)(5L & 0xFFFFFFFFL), asselectfirst.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
    var asselectsecond = instance.getExport("as-select-second");
    assertEquals((int)(6L & 0xFFFFFFFFL), asselectsecond.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
    assertEquals((int)(6L & 0xFFFFFFFFL), asselectsecond.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
    var asselectcond = instance.getExport("as-select-cond");
    assertEquals((int)(7L & 0xFFFFFFFFL), asselectcond.apply().asInt());
    var ascallfirst = instance.getExport("as-call-first");
    assertEquals((int)(12L & 0xFFFFFFFFL), ascallfirst.apply().asInt());
    var ascallmid = instance.getExport("as-call-mid");
    assertEquals((int)(13L & 0xFFFFFFFFL), ascallmid.apply().asInt());
    var ascalllast = instance.getExport("as-call-last");
    assertEquals((int)(14L & 0xFFFFFFFFL), ascalllast.apply().asInt());
    var ascallindirectfunc = instance.getExport("as-call_indirect-func");
    assertEquals((int)(20L & 0xFFFFFFFFL), ascallindirectfunc.apply().asInt());
    var ascallindirectfirst = instance.getExport("as-call_indirect-first");
    assertEquals((int)(21L & 0xFFFFFFFFL), ascallindirectfirst.apply().asInt());
    var ascallindirectmid = instance.getExport("as-call_indirect-mid");
    assertEquals((int)(22L & 0xFFFFFFFFL), ascallindirectmid.apply().asInt());
    var ascallindirectlast = instance.getExport("as-call_indirect-last");
    assertEquals((int)(23L & 0xFFFFFFFFL), ascallindirectlast.apply().asInt());
    var aslocalsetvalue = instance.getExport("as-local.set-value");
    assertEquals((int)(17L & 0xFFFFFFFFL), aslocalsetvalue.apply().asInt());
    var aslocalteevalue = instance.getExport("as-local.tee-value");
    assertEquals((int)(1L & 0xFFFFFFFFL), aslocalteevalue.apply().asInt());
    var asglobalsetvalue = instance.getExport("as-global.set-value");
    assertEquals((int)(1L & 0xFFFFFFFFL), asglobalsetvalue.apply().asInt());
    var asloadaddress = instance.getExport("as-load-address");
    assertEquals(floatVal("1071225242"), asloadaddress.apply().asFloat(), 0.0);
    var asloadNaddress = instance.getExport("as-loadN-address");
    assertEquals(longVal("30"), asloadNaddress.apply().asLong());
    var asstoreaddress = instance.getExport("as-store-address");
    assertEquals((int)(30L & 0xFFFFFFFFL), asstoreaddress.apply().asInt());
    var asstorevalue = instance.getExport("as-store-value");
    assertEquals((int)(31L & 0xFFFFFFFFL), asstorevalue.apply().asInt());
    var asstoreNaddress = instance.getExport("as-storeN-address");
    assertEquals((int)(32L & 0xFFFFFFFFL), asstoreNaddress.apply().asInt());
    var asstoreNvalue = instance.getExport("as-storeN-value");
    assertEquals((int)(33L & 0xFFFFFFFFL), asstoreNvalue.apply().asInt());
    var asunaryoperand = instance.getExport("as-unary-operand");
    assertEquals(floatVal("1079613850"), asunaryoperand.apply().asFloat(), 0.0);
    var asbinaryleft = instance.getExport("as-binary-left");
    assertEquals((int)(3L & 0xFFFFFFFFL), asbinaryleft.apply().asInt());
    var asbinaryright = instance.getExport("as-binary-right");
    assertEquals(longVal("45"), asbinaryright.apply().asLong());
    var astestoperand = instance.getExport("as-test-operand");
    assertEquals((int)(44L & 0xFFFFFFFFL), astestoperand.apply().asInt());
    var ascompareleft = instance.getExport("as-compare-left");
    assertEquals((int)(43L & 0xFFFFFFFFL), ascompareleft.apply().asInt());
    var ascompareright = instance.getExport("as-compare-right");
    assertEquals((int)(42L & 0xFFFFFFFFL), ascompareright.apply().asInt());
    var asconvertoperand = instance.getExport("as-convert-operand");
    assertEquals((int)(41L & 0xFFFFFFFFL), asconvertoperand.apply().asInt());
    var asmemorygrowsize = instance.getExport("as-memory.grow-size");
    assertEquals((int)(40L & 0xFFFFFFFFL), asmemorygrowsize.apply().asInt());
  }
  @Test
  public void testReturn1Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.1.wasm").instantiate();
  }
  @Test
  public void testReturn2Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.2.wasm").instantiate();
  }
  @Test
  public void testReturn3Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.3.wasm").instantiate();
  }
  @Test
  public void testReturn4Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.4.wasm").instantiate();
  }
  @Test
  public void testReturn5Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.5.wasm").instantiate();
  }
  @Test
  public void testReturn6Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.6.wasm").instantiate();
  }
  @Test
  public void testReturn7Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.7.wasm").instantiate();
  }
  @Test
  public void testReturn8Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.8.wasm").instantiate();
  }
  @Test
  public void testReturn9Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.9.wasm").instantiate();
  }
  @Test
  public void testReturn10Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.10.wasm").instantiate();
  }
  @Test
  public void testReturn11Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.11.wasm").instantiate();
  }
  @Test
  public void testReturn12Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.12.wasm").instantiate();
  }
  @Test
  public void testReturn13Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.13.wasm").instantiate();
  }
  @Test
  public void testReturn14Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.14.wasm").instantiate();
  }
  @Test
  public void testReturn15Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.15.wasm").instantiate();
  }
  @Test
  public void testReturn16Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.16.wasm").instantiate();
  }
  @Test
  public void testReturn17Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.17.wasm").instantiate();
  }
  @Test
  public void testReturn18Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.18.wasm").instantiate();
  }
  @Test
  public void testReturn19Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.19.wasm").instantiate();
  }
  @Test
  public void testReturn20Wasm() {
    var instance = Module.build("src/test/resources/wasm/specv1/return.20.wasm").instantiate();
  }
}
