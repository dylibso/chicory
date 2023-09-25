
  package com.dylibso.chicory.runtime;

  import java.math.BigInteger;
  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertThrows;
  import com.dylibso.chicory.wasm.types.Value;
  import com.dylibso.chicory.wasm.types.ValueType;
  import org.junit.Test;

  public class SpecV1I32Test {

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
	public void testI320Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.0.wasm").instantiate();
		var add = instance.getExport("add");
		assertEquals((int)(2L & 0xFFFFFFFFL), add.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), add.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967294L & 0xFFFFFFFFL), add.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), add.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), add.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483647L & 0xFFFFFFFFL), add.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), add.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1073741824L & 0xFFFFFFFFL), add.apply(Value.i32((int)(1073741823L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		var sub = instance.getExport("sub");
		assertEquals((int)(0L & 0xFFFFFFFFL), sub.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), sub.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), sub.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), sub.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483647L & 0xFFFFFFFFL), sub.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), sub.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1073741824L & 0xFFFFFFFFL), sub.apply(Value.i32((int)(1073741823L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var mul = instance.getExport("mul");
		assertEquals((int)(1L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(268435456L & 0xFFFFFFFFL)), Value.i32((int)(4096L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483649L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(898528368L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(19088743L & 0xFFFFFFFFL)), Value.i32((int)(1985229328L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), mul.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		var divs = instance.getExport("div_s");
		assertEquals((int)(1L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(3221225472L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4292819813L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(2147483649L & 0xFFFFFFFFL)), Value.i32((int)(1000L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(5L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967294L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(4294967291L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967294L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(5L & 0xFFFFFFFFL)), Value.i32((int)(4294967294L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(4294967291L & 0xFFFFFFFFL)), Value.i32((int)(4294967294L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(7L & 0xFFFFFFFFL)), Value.i32((int)(3L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967294L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(4294967289L & 0xFFFFFFFFL)), Value.i32((int)(3L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967294L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(7L & 0xFFFFFFFFL)), Value.i32((int)(4294967293L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(4294967289L & 0xFFFFFFFFL)), Value.i32((int)(4294967293L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(11L & 0xFFFFFFFFL)), Value.i32((int)(5L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divs.apply(Value.i32((int)(17L & 0xFFFFFFFFL)), Value.i32((int)(7L & 0xFFFFFFFFL))).asInt());
		var divu = instance.getExport("div_u");
		assertEquals((int)(1L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1073741824L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(36847L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(2414874608L & 0xFFFFFFFFL)), Value.i32((int)(65537L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(2147483649L & 0xFFFFFFFFL)), Value.i32((int)(1000L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(5L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483645L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(4294967291L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(5L & 0xFFFFFFFFL)), Value.i32((int)(4294967294L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(4294967291L & 0xFFFFFFFFL)), Value.i32((int)(4294967294L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(7L & 0xFFFFFFFFL)), Value.i32((int)(3L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(11L & 0xFFFFFFFFL)), Value.i32((int)(5L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), divu.apply(Value.i32((int)(17L & 0xFFFFFFFFL)), Value.i32((int)(7L & 0xFFFFFFFFL))).asInt());
		var rems = instance.getExport("rem_s");
		assertEquals((int)(0L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294966649L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(2147483649L & 0xFFFFFFFFL)), Value.i32((int)(1000L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(5L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(4294967291L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(5L & 0xFFFFFFFFL)), Value.i32((int)(4294967294L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(4294967291L & 0xFFFFFFFFL)), Value.i32((int)(4294967294L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(7L & 0xFFFFFFFFL)), Value.i32((int)(3L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(4294967289L & 0xFFFFFFFFL)), Value.i32((int)(3L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(7L & 0xFFFFFFFFL)), Value.i32((int)(4294967293L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(4294967289L & 0xFFFFFFFFL)), Value.i32((int)(4294967293L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(11L & 0xFFFFFFFFL)), Value.i32((int)(5L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(3L & 0xFFFFFFFFL), rems.apply(Value.i32((int)(17L & 0xFFFFFFFFL)), Value.i32((int)(7L & 0xFFFFFFFFL))).asInt());
		var remu = instance.getExport("rem_u");
		assertEquals((int)(0L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(32769L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(2414874608L & 0xFFFFFFFFL)), Value.i32((int)(65537L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(649L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(2147483649L & 0xFFFFFFFFL)), Value.i32((int)(1000L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(5L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(4294967291L & 0xFFFFFFFFL)), Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(5L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(5L & 0xFFFFFFFFL)), Value.i32((int)(4294967294L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967291L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(4294967291L & 0xFFFFFFFFL)), Value.i32((int)(4294967294L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(7L & 0xFFFFFFFFL)), Value.i32((int)(3L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(11L & 0xFFFFFFFFL)), Value.i32((int)(5L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(3L & 0xFFFFFFFFL), remu.apply(Value.i32((int)(17L & 0xFFFFFFFFL)), Value.i32((int)(7L & 0xFFFFFFFFL))).asInt());
		var and = instance.getExport("and");
		assertEquals((int)(0L & 0xFFFFFFFFL), and.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), and.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), and.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), and.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), and.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483647L & 0xFFFFFFFFL), and.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4042322160L & 0xFFFFFFFFL), and.apply(Value.i32((int)(4042326015L & 0xFFFFFFFFL)), Value.i32((int)(4294963440L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), and.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var or = instance.getExport("or");
		assertEquals((int)(1L & 0xFFFFFFFFL), or.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), or.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), or.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), or.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), or.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), or.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), or.apply(Value.i32((int)(4042326015L & 0xFFFFFFFFL)), Value.i32((int)(4294963440L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), or.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var xor = instance.getExport("xor");
		assertEquals((int)(1L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483647L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(252645135L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(4042326015L & 0xFFFFFFFFL)), Value.i32((int)(4294963440L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), xor.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var shl = instance.getExport("shl");
		assertEquals((int)(2L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967294L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967294L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(1073741824L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(31L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(32L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(33L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), shl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		var shrs = instance.getExport("shr_s");
		assertEquals((int)(0L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1073741823L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(3221225472L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(536870912L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(1073741824L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(32L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(33L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(31L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(32L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(33L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shrs.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var shru = instance.getExport("shr_u");
		assertEquals((int)(0L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483647L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1073741823L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1073741824L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(536870912L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(1073741824L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(32L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(33L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(31L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(32L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483647L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(33L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), shru.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var rotl = instance.getExport("rotl");
		assertEquals((int)(2L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(32L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1469788397L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(2882377846L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(3758997519L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(4261469184L & 0xFFFFFFFFL)), Value.i32((int)(4L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(406477942L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(2965492451L & 0xFFFFFFFFL)), Value.i32((int)(5L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1048576L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(32768L & 0xFFFFFFFFL)), Value.i32((int)(37L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(406477942L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(2965492451L & 0xFFFFFFFFL)), Value.i32((int)(65285L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1469837011L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(1989852383L & 0xFFFFFFFFL)), Value.i32((int)(4294967277L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1469837011L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(1989852383L & 0xFFFFFFFFL)), Value.i32((int)(2147483661L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(31L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rotl.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		var rotr = instance.getExport("rotr");
		assertEquals((int)(2147483648L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(32L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2139121152L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(4278242304L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(32768L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(524288L & 0xFFFFFFFFL)), Value.i32((int)(4L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(495324823L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(2965492451L & 0xFFFFFFFFL)), Value.i32((int)(5L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1024L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(32768L & 0xFFFFFFFFL)), Value.i32((int)(37L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(495324823L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(2965492451L & 0xFFFFFFFFL)), Value.i32((int)(65285L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(3875255509L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(1989852383L & 0xFFFFFFFFL)), Value.i32((int)(4294967277L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(3875255509L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(1989852383L & 0xFFFFFFFFL)), Value.i32((int)(2147483661L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(31L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), rotr.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(31L & 0xFFFFFFFFL))).asInt());
		var clz = instance.getExport("clz");
		assertEquals((int)(0L & 0xFFFFFFFFL), clz.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(32L & 0xFFFFFFFFL), clz.apply(Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(16L & 0xFFFFFFFFL), clz.apply(Value.i32((int)(32768L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(24L & 0xFFFFFFFFL), clz.apply(Value.i32((int)(255L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), clz.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(31L & 0xFFFFFFFFL), clz.apply(Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(30L & 0xFFFFFFFFL), clz.apply(Value.i32((int)(2L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), clz.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		var ctz = instance.getExport("ctz");
		assertEquals((int)(0L & 0xFFFFFFFFL), ctz.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(32L & 0xFFFFFFFFL), ctz.apply(Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(15L & 0xFFFFFFFFL), ctz.apply(Value.i32((int)(32768L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(16L & 0xFFFFFFFFL), ctz.apply(Value.i32((int)(65536L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(31L & 0xFFFFFFFFL), ctz.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ctz.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		var popcnt = instance.getExport("popcnt");
		assertEquals((int)(32L & 0xFFFFFFFFL), popcnt.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), popcnt.apply(Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), popcnt.apply(Value.i32((int)(32768L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(2L & 0xFFFFFFFFL), popcnt.apply(Value.i32((int)(2147516416L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(31L & 0xFFFFFFFFL), popcnt.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(16L & 0xFFFFFFFFL), popcnt.apply(Value.i32((int)(2863311530L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(16L & 0xFFFFFFFFL), popcnt.apply(Value.i32((int)(1431655765L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(24L & 0xFFFFFFFFL), popcnt.apply(Value.i32((int)(3735928559L & 0xFFFFFFFFL))).asInt());
		var extend8s = instance.getExport("extend8_s");
		assertEquals((int)(0L & 0xFFFFFFFFL), extend8s.apply(Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(127L & 0xFFFFFFFFL), extend8s.apply(Value.i32((int)(127L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967168L & 0xFFFFFFFFL), extend8s.apply(Value.i32((int)(128L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), extend8s.apply(Value.i32((int)(255L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), extend8s.apply(Value.i32((int)(19088640L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967168L & 0xFFFFFFFFL), extend8s.apply(Value.i32((int)(4275878528L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), extend8s.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var extend16s = instance.getExport("extend16_s");
		assertEquals((int)(0L & 0xFFFFFFFFL), extend16s.apply(Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(32767L & 0xFFFFFFFFL), extend16s.apply(Value.i32((int)(32767L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294934528L & 0xFFFFFFFFL), extend16s.apply(Value.i32((int)(32768L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), extend16s.apply(Value.i32((int)(65535L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), extend16s.apply(Value.i32((int)(19070976L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294934528L & 0xFFFFFFFFL), extend16s.apply(Value.i32((int)(4275863552L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(4294967295L & 0xFFFFFFFFL), extend16s.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var eqz = instance.getExport("eqz");
		assertEquals((int)(1L & 0xFFFFFFFFL), eqz.apply(Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eqz.apply(Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eqz.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eqz.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eqz.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		var eq = instance.getExport("eq");
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var ne = instance.getExport("ne");
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var lts = instance.getExport("lt_s");
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var ltu = instance.getExport("lt_u");
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ltu.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var les = instance.getExport("le_s");
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), les.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), les.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), les.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), les.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var leu = instance.getExport("le_u");
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var gts = instance.getExport("gt_s");
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gts.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var gtu = instance.getExport("gt_u");
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var ges = instance.getExport("ge_s");
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		var geu = instance.getExport("ge_u");
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(1L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(1L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(0L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(0L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(4294967295L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(4294967295L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(2147483648L & 0xFFFFFFFFL)), Value.i32((int)(2147483647L & 0xFFFFFFFFL))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), geu.apply(Value.i32((int)(2147483647L & 0xFFFFFFFFL)), Value.i32((int)(2147483648L & 0xFFFFFFFFL))).asInt());
	}
	@Test
	public void testI321Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.1.wasm").instantiate();
	}
	@Test
	public void testI322Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.2.wasm").instantiate();
	}
	@Test
	public void testI323Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.3.wasm").instantiate();
	}
	@Test
	public void testI324Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.4.wasm").instantiate();
	}
	@Test
	public void testI325Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.5.wasm").instantiate();
	}
	@Test
	public void testI326Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.6.wasm").instantiate();
	}
	@Test
	public void testI327Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.7.wasm").instantiate();
	}
	@Test
	public void testI328Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.8.wasm").instantiate();
	}
	@Test
	public void testI329Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.9.wasm").instantiate();
	}
	@Test
	public void testI3210Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.10.wasm").instantiate();
	}
	@Test
	public void testI3211Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.11.wasm").instantiate();
	}
	@Test
	public void testI3212Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.12.wasm").instantiate();
	}
	@Test
	public void testI3213Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.13.wasm").instantiate();
	}
	@Test
	public void testI3214Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.14.wasm").instantiate();
	}
	@Test
	public void testI3215Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.15.wasm").instantiate();
	}
	@Test
	public void testI3216Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.16.wasm").instantiate();
	}
	@Test
	public void testI3217Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.17.wasm").instantiate();
	}
	@Test
	public void testI3218Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.18.wasm").instantiate();
	}
	@Test
	public void testI3219Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.19.wasm").instantiate();
	}
	@Test
	public void testI3220Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.20.wasm").instantiate();
	}
	@Test
	public void testI3221Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.21.wasm").instantiate();
	}
	@Test
	public void testI3222Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.22.wasm").instantiate();
	}
	@Test
	public void testI3223Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.23.wasm").instantiate();
	}
	@Test
	public void testI3224Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.24.wasm").instantiate();
	}
	@Test
	public void testI3225Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.25.wasm").instantiate();
	}
	@Test
	public void testI3226Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.26.wasm").instantiate();
	}
	@Test
	public void testI3227Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.27.wasm").instantiate();
	}
	@Test
	public void testI3228Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.28.wasm").instantiate();
	}
	@Test
	public void testI3229Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.29.wasm").instantiate();
	}
	@Test
	public void testI3230Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.30.wasm").instantiate();
	}
	@Test
	public void testI3231Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.31.wasm").instantiate();
	}
	@Test
	public void testI3232Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.32.wasm").instantiate();
	}
	@Test
	public void testI3233Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.33.wasm").instantiate();
	}
	@Test
	public void testI3234Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.34.wasm").instantiate();
	}
	@Test
	public void testI3235Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.35.wasm").instantiate();
	}
	@Test
	public void testI3236Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.36.wasm").instantiate();
	}
	@Test
	public void testI3237Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.37.wasm").instantiate();
	}
	@Test
	public void testI3238Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.38.wasm").instantiate();
	}
	@Test
	public void testI3239Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.39.wasm").instantiate();
	}
	@Test
	public void testI3240Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.40.wasm").instantiate();
	}
	@Test
	public void testI3241Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.41.wasm").instantiate();
	}
	@Test
	public void testI3242Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.42.wasm").instantiate();
	}
	@Test
	public void testI3243Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.43.wasm").instantiate();
	}
	@Test
	public void testI3244Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.44.wasm").instantiate();
	}
	@Test
	public void testI3245Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.45.wasm").instantiate();
	}
	@Test
	public void testI3246Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.46.wasm").instantiate();
	}
	@Test
	public void testI3247Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.47.wasm").instantiate();
	}
	@Test
	public void testI3248Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.48.wasm").instantiate();
	}
	@Test
	public void testI3249Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.49.wasm").instantiate();
	}
	@Test
	public void testI3250Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.50.wasm").instantiate();
	}
	@Test
	public void testI3251Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.51.wasm").instantiate();
	}
	@Test
	public void testI3252Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.52.wasm").instantiate();
	}
	@Test
	public void testI3253Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.53.wasm").instantiate();
	}
	@Test
	public void testI3254Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.54.wasm").instantiate();
	}
	@Test
	public void testI3255Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.55.wasm").instantiate();
	}
	@Test
	public void testI3256Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.56.wasm").instantiate();
	}
	@Test
	public void testI3257Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.57.wasm").instantiate();
	}
	@Test
	public void testI3258Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.58.wasm").instantiate();
	}
	@Test
	public void testI3259Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.59.wasm").instantiate();
	}
	@Test
	public void testI3260Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.60.wasm").instantiate();
	}
	@Test
	public void testI3261Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.61.wasm").instantiate();
	}
	@Test
	public void testI3262Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.62.wasm").instantiate();
	}
	@Test
	public void testI3263Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.63.wasm").instantiate();
	}
	@Test
	public void testI3264Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.64.wasm").instantiate();
	}
	@Test
	public void testI3265Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.65.wasm").instantiate();
	}
	@Test
	public void testI3266Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.66.wasm").instantiate();
	}
	@Test
	public void testI3267Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.67.wasm").instantiate();
	}
	@Test
	public void testI3268Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.68.wasm").instantiate();
	}
	@Test
	public void testI3269Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.69.wasm").instantiate();
	}
	@Test
	public void testI3270Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.70.wasm").instantiate();
	}
	@Test
	public void testI3271Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.71.wasm").instantiate();
	}
	@Test
	public void testI3272Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.72.wasm").instantiate();
	}
	@Test
	public void testI3273Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.73.wasm").instantiate();
	}
	@Test
	public void testI3274Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.74.wasm").instantiate();
	}
	@Test
	public void testI3275Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.75.wasm").instantiate();
	}
	@Test
	public void testI3276Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.76.wasm").instantiate();
	}
	@Test
	public void testI3277Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.77.wasm").instantiate();
	}
	@Test
	public void testI3278Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.78.wasm").instantiate();
	}
	@Test
	public void testI3279Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.79.wasm").instantiate();
	}
	@Test
	public void testI3280Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.80.wasm").instantiate();
	}
	@Test
	public void testI3281Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.81.wasm").instantiate();
	}
	@Test
	public void testI3282Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.82.wasm").instantiate();
	}
	@Test
	public void testI3283Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i32.83.wasm").instantiate();
	}
}
