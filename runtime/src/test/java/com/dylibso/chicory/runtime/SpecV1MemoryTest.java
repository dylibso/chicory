
  package com.dylibso.chicory.runtime;

  import java.math.BigInteger;
  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertThrows;
  import com.dylibso.chicory.wasm.types.Value;
  import com.dylibso.chicory.wasm.types.ValueType;
  import org.junit.Test;

  public class SpecV1MemoryTest {

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
	public void testMemory0Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.0.wasm").instantiate();
	}
	@Test
	public void testMemory1Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.1.wasm").instantiate();
	}
	@Test
	public void testMemory2Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.2.wasm").instantiate();
	}
	@Test
	public void testMemory3Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.3.wasm").instantiate();
	}
	@Test
	public void testMemory4Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.4.wasm").instantiate();
	}
	@Test
	public void testMemory5Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.5.wasm").instantiate();
	}
	@Test
	public void testMemory6Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.6.wasm").instantiate();
	}
	@Test
	public void testMemory7Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.7.wasm").instantiate();
	}
	@Test
	public void testMemory8Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.8.wasm").instantiate();
		var memsize = instance.getExport("memsize");
		assertEquals((int)(0L & 0xFFFFFFFFL), memsize.apply().asInt());
	}
	@Test
	public void testMemory9Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.9.wasm").instantiate();
		var memsize = instance.getExport("memsize");
		assertEquals((int)(0L & 0xFFFFFFFFL), memsize.apply().asInt());
	}
	@Test
	public void testMemory10Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.10.wasm").instantiate();
		var memsize = instance.getExport("memsize");
		assertEquals((int)(1L & 0xFFFFFFFFL), memsize.apply().asInt());
	}
	@Test
	public void testMemory11Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.11.wasm").instantiate();
	}
	@Test
	public void testMemory12Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.12.wasm").instantiate();
	}
	@Test
	public void testMemory13Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.13.wasm").instantiate();
	}
	@Test
	public void testMemory14Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.14.wasm").instantiate();
	}
	@Test
	public void testMemory15Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.15.wasm").instantiate();
	}
	@Test
	public void testMemory16Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.16.wasm").instantiate();
	}
	@Test
	public void testMemory17Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.17.wasm").instantiate();
	}
	@Test
	public void testMemory18Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.18.wasm").instantiate();
	}
	@Test
	public void testMemory19Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.19.wasm").instantiate();
	}
	@Test
	public void testMemory20Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.20.wasm").instantiate();
	}
	@Test
	public void testMemory21Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.21.wasm").instantiate();
	}
	@Test
	public void testMemory22Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.22.wasm").instantiate();
	}
	@Test
	public void testMemory23Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.23.wasm").instantiate();
	}
	@Test
	public void testMemory24Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.24.wasm").instantiate();
	}
	@Test
	public void testMemory25Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.25.wasm").instantiate();
	}
	@Test
	public void testMemory26Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.26.wasm").instantiate();
	}
	@Test
	public void testMemory30Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/memory.30.wasm").instantiate();
		var data = instance.getExport("data");
		assertEquals((int)(1L & 0xFFFFFFFFL), data.apply().asInt());
////		var cast = instance.getExport("cast");
////		assertEquals(doubleVal("4631107791820423168"), cast.apply().asDouble(), 0.0);
		var i32load8s = instance.getExport("i32_load8_s");
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), i32load8s.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var i32load8u = instance.getExport("i32_load8_u");
		assertEquals((int)(255L & 0xFFFFFFFFL), i32load8u.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var i32load16s = instance.getExport("i32_load16_s");
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), i32load16s.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var i32load16u = instance.getExport("i32_load16_u");
		assertEquals((int)(65535L & 0xFFFFFFFFL), i32load16u.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(100L & 0xFFFFFFFFL), i32load8s.apply(Value.i32((int)(100L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(200L & 0xFFFFFFFFL), i32load8u.apply(Value.i32((int)(200L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(20000L & 0xFFFFFFFFL), i32load16s.apply(Value.i32((int)(20000L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(40000L & 0xFFFFFFFFL), i32load16u.apply(Value.i32((int)(40000L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(67L & 0xFFFFFFFFL), i32load8s.apply(Value.i32((int)(4275856707L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967279L & 0xFFFFFFFFL), i32load8s.apply(Value.i32((int)(878104047L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(67L & 0xFFFFFFFFL), i32load8u.apply(Value.i32((int)(4275856707L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(239L & 0xFFFFFFFFL), i32load8u.apply(Value.i32((int)(878104047L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(25923L & 0xFFFFFFFFL), i32load16s.apply(Value.i32((int)(4275856707L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294954479L & 0xFFFFFFFFL), i32load16s.apply(Value.i32((int)(878104047L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(25923L & 0xFFFFFFFFL), i32load16u.apply(Value.i32((int)(4275856707L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(52719L & 0xFFFFFFFFL), i32load16u.apply(Value.i32((int)(878104047L & 0xFFFFFFFFL))).asInt());
		var i64load8s = instance.getExport("i64_load8_s");
		assertEquals(longVal("18446744073709551615"), i64load8s.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		var i64load8u = instance.getExport("i64_load8_u");
		assertEquals(longVal("255"), i64load8u.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		var i64load16s = instance.getExport("i64_load16_s");
		assertEquals(longVal("18446744073709551615"), i64load16s.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		var i64load16u = instance.getExport("i64_load16_u");
		assertEquals(longVal("65535"), i64load16u.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		var i64load32s = instance.getExport("i64_load32_s");
		assertEquals(longVal("18446744073709551615"), i64load32s.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		var i64load32u = instance.getExport("i64_load32_u");
		assertEquals(longVal("4294967295"), i64load32u.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("100"), i64load8s.apply(Value.i64(longVal("100"))).asLong());
		assertEquals(longVal("200"), i64load8u.apply(Value.i64(longVal("200"))).asLong());
		assertEquals(longVal("20000"), i64load16s.apply(Value.i64(longVal("20000"))).asLong());
		assertEquals(longVal("40000"), i64load16u.apply(Value.i64(longVal("40000"))).asLong());
		assertEquals(longVal("20000"), i64load32s.apply(Value.i64(longVal("20000"))).asLong());
		assertEquals(longVal("40000"), i64load32u.apply(Value.i64(longVal("40000"))).asLong());
		assertEquals(longVal("67"), i64load8s.apply(Value.i64(longVal("18364758543954109763"))).asLong());
		assertEquals(longVal("18446744073709551599"), i64load8s.apply(Value.i64(longVal("3771275841602506223"))).asLong());
		assertEquals(longVal("67"), i64load8u.apply(Value.i64(longVal("18364758543954109763"))).asLong());
		assertEquals(longVal("239"), i64load8u.apply(Value.i64(longVal("3771275841602506223"))).asLong());
		assertEquals(longVal("25923"), i64load16s.apply(Value.i64(longVal("18364758543954109763"))).asLong());
		assertEquals(longVal("18446744073709538799"), i64load16s.apply(Value.i64(longVal("3771275841602506223"))).asLong());
		assertEquals(longVal("25923"), i64load16u.apply(Value.i64(longVal("18364758543954109763"))).asLong());
		assertEquals(longVal("52719"), i64load16u.apply(Value.i64(longVal("3771275841602506223"))).asLong());
		assertEquals(longVal("1446274371"), i64load32s.apply(Value.i64(longVal("18364758543954109763"))).asLong());
		assertEquals(longVal("18446744071976963567"), i64load32s.apply(Value.i64(longVal("3771275841602506223"))).asLong());
		assertEquals(longVal("1446274371"), i64load32u.apply(Value.i64(longVal("18364758543954109763"))).asLong());
		assertEquals(longVal("2562379247"), i64load32u.apply(Value.i64(longVal("3771275841602506223"))).asLong());
	}
}
