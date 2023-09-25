
  package com.dylibso.chicory.runtime;

  import java.math.BigInteger;
  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertThrows;
  import com.dylibso.chicory.wasm.types.Value;
  import com.dylibso.chicory.wasm.types.ValueType;
  import org.junit.Test;

  public class SpecV1LocalGetTest {

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
	public void testLocalGet0Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.0.wasm").instantiate();
//		var typelocali32 = instance.getExport("type-local-i32");
//		assertEquals((int)(0L & 0xFFFFFFFFL), typelocali32.apply().asInt());
//		var typelocali64 = instance.getExport("type-local-i64");
//		assertEquals(longVal("0"), typelocali64.apply().asLong());
//		var typelocalf32 = instance.getExport("type-local-f32");
//		assertEquals(floatVal("0"), typelocalf32.apply().asFloat(), 0.0);
//		var typelocalf64 = instance.getExport("type-local-f64");
//		assertEquals(doubleVal("0"), typelocalf64.apply().asDouble(), 0.0);
//		var typeparami32 = instance.getExport("type-param-i32");
//		assertEquals((int)(2L & 0xFFFFFFFFL), typeparami32.apply(Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
//		var typeparami64 = instance.getExport("type-param-i64");
//		assertEquals(longVal("3"), typeparami64.apply(Value.i64(longVal("3"))).asLong());
//		var typeparamf32 = instance.getExport("type-param-f32");
//		assertEquals(floatVal("1082969293"), typeparamf32.apply(Value.f32(longVal("1082969293"))).asFloat(), 0.0);
//		var typeparamf64 = instance.getExport("type-param-f64");
//		assertEquals(doubleVal("4617878467915022336"), typeparamf64.apply(Value.f64(longVal("4617878467915022336"))).asDouble(), 0.0);
//		var asblockvalue = instance.getExport("as-block-value");
//		assertEquals((int)(6L & 0xFFFFFFFFL), asblockvalue.apply(Value.i32((int)(6L & 0xFFFFFFFFL))).asInt());
//		var asloopvalue = instance.getExport("as-loop-value");
//		assertEquals((int)(7L & 0xFFFFFFFFL), asloopvalue.apply(Value.i32((int)(7L & 0xFFFFFFFFL))).asInt());
//		var asbrvalue = instance.getExport("as-br-value");
//		assertEquals((int)(8L & 0xFFFFFFFFL), asbrvalue.apply(Value.i32((int)(8L & 0xFFFFFFFFL))).asInt());
//		var asbrifvalue = instance.getExport("as-br_if-value");
//		assertEquals((int)(9L & 0xFFFFFFFFL), asbrifvalue.apply(Value.i32((int)(9L & 0xFFFFFFFFL))).asInt());
//		var asbrifvaluecond = instance.getExport("as-br_if-value-cond");
//		assertEquals((int)(10L & 0xFFFFFFFFL), asbrifvaluecond.apply(Value.i32((int)(10L & 0xFFFFFFFFL))).asInt());
//		var asbrtablevalue = instance.getExport("as-br_table-value");
//		assertEquals((int)(2L & 0xFFFFFFFFL), asbrtablevalue.apply(Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
//		var asreturnvalue = instance.getExport("as-return-value");
//		assertEquals((int)(0L & 0xFFFFFFFFL), asreturnvalue.apply(Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
//		var asifthen = instance.getExport("as-if-then");
//		assertEquals((int)(1L & 0xFFFFFFFFL), asifthen.apply(Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
//		var asifelse = instance.getExport("as-if-else");
//		assertEquals((int)(0L & 0xFFFFFFFFL), asifelse.apply(Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		var typemixed = instance.getExport("type-mixed");
		assertEquals(null, typemixed.apply(Value.i64(longVal("1")), Value.f32(longVal("1074580685")), Value.f64(longVal("4614613358185178726")), Value.i32((int)(4L & 0xFFFFFFFFL)), Value.i32((int)(5L & 0xFFFFFFFFL))));
//		var read = instance.getExport("read");
//		assertEquals(doubleVal("4630094481904264806"), read.apply(Value.i64(longVal("1")), Value.f32(longVal("1073741824")), Value.f64(longVal("4614613358185178726")), Value.i32((int)(4L & 0xFFFFFFFFL)), Value.i32((int)(5L & 0xFFFFFFFFL))).asDouble(), 0.0);
	}
	@Test
	public void testLocalGet1Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.1.wasm").instantiate();
	}
	@Test
	public void testLocalGet2Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.2.wasm").instantiate();
	}
	@Test
	public void testLocalGet3Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.3.wasm").instantiate();
	}
	@Test
	public void testLocalGet4Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.4.wasm").instantiate();
	}
	@Test
	public void testLocalGet5Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.5.wasm").instantiate();
	}
	@Test
	public void testLocalGet6Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.6.wasm").instantiate();
	}
	@Test
	public void testLocalGet7Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.7.wasm").instantiate();
	}
	@Test
	public void testLocalGet8Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.8.wasm").instantiate();
	}
	@Test
	public void testLocalGet9Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.9.wasm").instantiate();
	}
	@Test
	public void testLocalGet10Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.10.wasm").instantiate();
	}
	@Test
	public void testLocalGet11Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.11.wasm").instantiate();
	}
	@Test
	public void testLocalGet12Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.12.wasm").instantiate();
	}
	@Test
	public void testLocalGet13Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.13.wasm").instantiate();
	}
	@Test
	public void testLocalGet14Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.14.wasm").instantiate();
	}
	@Test
	public void testLocalGet15Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.15.wasm").instantiate();
	}
	@Test
	public void testLocalGet16Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/local_get.16.wasm").instantiate();
	}
}
