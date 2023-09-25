
  package com.dylibso.chicory.runtime;

  import java.math.BigInteger;
  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertThrows;
  import com.dylibso.chicory.wasm.types.Value;
  import com.dylibso.chicory.wasm.types.ValueType;
  import org.junit.Test;

  public class SpecV1I64Test {

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
	public void testI640Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.0.wasm").instantiate();
		var add = instance.getExport("add");
		assertEquals(longVal("2"), add.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), add.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("18446744073709551614"), add.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), add.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("9223372036854775808"), add.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("9223372036854775807"), add.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), add.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("1073741824"), add.apply(Value.i64(longVal("1073741823")), Value.i64(longVal("1"))).asLong());
		var sub = instance.getExport("sub");
		assertEquals(longVal("0"), sub.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), sub.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("0"), sub.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("9223372036854775808"), sub.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("9223372036854775807"), sub.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), sub.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("1073741824"), sub.apply(Value.i64(longVal("1073741823")), Value.i64(longVal("18446744073709551615"))).asLong());
		var mul = instance.getExport("mul");
		assertEquals(longVal("1"), mul.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), mul.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("1"), mul.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), mul.apply(Value.i64(longVal("1152921504606846976")), Value.i64(longVal("4096"))).asLong());
		assertEquals(longVal("0"), mul.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("9223372036854775808"), mul.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("9223372036854775809"), mul.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("2465395958572223728"), mul.apply(Value.i64(longVal("81985529216486895")), Value.i64(longVal("18364758544493064720"))).asLong());
		assertEquals(longVal("1"), mul.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asLong());
		var divs = instance.getExport("div_s");
		assertEquals(longVal("1"), divs.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), divs.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), divs.apply(Value.i64(longVal("0")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("1"), divs.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("13835058055282163712"), divs.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("18437520701672696841"), divs.apply(Value.i64(longVal("9223372036854775809")), Value.i64(longVal("1000"))).asLong());
		assertEquals(longVal("2"), divs.apply(Value.i64(longVal("5")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("18446744073709551614"), divs.apply(Value.i64(longVal("18446744073709551611")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("18446744073709551614"), divs.apply(Value.i64(longVal("5")), Value.i64(longVal("18446744073709551614"))).asLong());
		assertEquals(longVal("2"), divs.apply(Value.i64(longVal("18446744073709551611")), Value.i64(longVal("18446744073709551614"))).asLong());
		assertEquals(longVal("2"), divs.apply(Value.i64(longVal("7")), Value.i64(longVal("3"))).asLong());
		assertEquals(longVal("18446744073709551614"), divs.apply(Value.i64(longVal("18446744073709551609")), Value.i64(longVal("3"))).asLong());
		assertEquals(longVal("18446744073709551614"), divs.apply(Value.i64(longVal("7")), Value.i64(longVal("18446744073709551613"))).asLong());
		assertEquals(longVal("2"), divs.apply(Value.i64(longVal("18446744073709551609")), Value.i64(longVal("18446744073709551613"))).asLong());
		assertEquals(longVal("2"), divs.apply(Value.i64(longVal("11")), Value.i64(longVal("5"))).asLong());
		assertEquals(longVal("2"), divs.apply(Value.i64(longVal("17")), Value.i64(longVal("7"))).asLong());
		var divu = instance.getExport("div_u");
		assertEquals(longVal("1"), divu.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), divu.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), divu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), divu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("4611686018427387904"), divu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("2414874607"), divu.apply(Value.i64(longVal("10371807465568210928")), Value.i64(longVal("4294967297"))).asLong());
		assertEquals(longVal("9223372036854775"), divu.apply(Value.i64(longVal("9223372036854775809")), Value.i64(longVal("1000"))).asLong());
		assertEquals(longVal("2"), divu.apply(Value.i64(longVal("5")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("9223372036854775805"), divu.apply(Value.i64(longVal("18446744073709551611")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("0"), divu.apply(Value.i64(longVal("5")), Value.i64(longVal("18446744073709551614"))).asLong());
		assertEquals(longVal("0"), divu.apply(Value.i64(longVal("18446744073709551611")), Value.i64(longVal("18446744073709551614"))).asLong());
		assertEquals(longVal("2"), divu.apply(Value.i64(longVal("7")), Value.i64(longVal("3"))).asLong());
		assertEquals(longVal("2"), divu.apply(Value.i64(longVal("11")), Value.i64(longVal("5"))).asLong());
		assertEquals(longVal("2"), divu.apply(Value.i64(longVal("17")), Value.i64(longVal("7"))).asLong());
		var rems = instance.getExport("rem_s");
		assertEquals(longVal("0"), rems.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), rems.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), rems.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), rems.apply(Value.i64(longVal("0")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), rems.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), rems.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), rems.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("18446744073709550809"), rems.apply(Value.i64(longVal("9223372036854775809")), Value.i64(longVal("1000"))).asLong());
		assertEquals(longVal("1"), rems.apply(Value.i64(longVal("5")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("18446744073709551615"), rems.apply(Value.i64(longVal("18446744073709551611")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("1"), rems.apply(Value.i64(longVal("5")), Value.i64(longVal("18446744073709551614"))).asLong());
		assertEquals(longVal("18446744073709551615"), rems.apply(Value.i64(longVal("18446744073709551611")), Value.i64(longVal("18446744073709551614"))).asLong());
		assertEquals(longVal("1"), rems.apply(Value.i64(longVal("7")), Value.i64(longVal("3"))).asLong());
		assertEquals(longVal("18446744073709551615"), rems.apply(Value.i64(longVal("18446744073709551609")), Value.i64(longVal("3"))).asLong());
		assertEquals(longVal("1"), rems.apply(Value.i64(longVal("7")), Value.i64(longVal("18446744073709551613"))).asLong());
		assertEquals(longVal("18446744073709551615"), rems.apply(Value.i64(longVal("18446744073709551609")), Value.i64(longVal("18446744073709551613"))).asLong());
		assertEquals(longVal("1"), rems.apply(Value.i64(longVal("11")), Value.i64(longVal("5"))).asLong());
		assertEquals(longVal("3"), rems.apply(Value.i64(longVal("17")), Value.i64(longVal("7"))).asLong());
		var remu = instance.getExport("rem_u");
		assertEquals(longVal("0"), remu.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), remu.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), remu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("9223372036854775808"), remu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), remu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("2147483649"), remu.apply(Value.i64(longVal("10371807465568210928")), Value.i64(longVal("4294967297"))).asLong());
		assertEquals(longVal("809"), remu.apply(Value.i64(longVal("9223372036854775809")), Value.i64(longVal("1000"))).asLong());
		assertEquals(longVal("1"), remu.apply(Value.i64(longVal("5")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("1"), remu.apply(Value.i64(longVal("18446744073709551611")), Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("5"), remu.apply(Value.i64(longVal("5")), Value.i64(longVal("18446744073709551614"))).asLong());
		assertEquals(longVal("18446744073709551611"), remu.apply(Value.i64(longVal("18446744073709551611")), Value.i64(longVal("18446744073709551614"))).asLong());
		assertEquals(longVal("1"), remu.apply(Value.i64(longVal("7")), Value.i64(longVal("3"))).asLong());
		assertEquals(longVal("1"), remu.apply(Value.i64(longVal("11")), Value.i64(longVal("5"))).asLong());
		assertEquals(longVal("3"), remu.apply(Value.i64(longVal("17")), Value.i64(longVal("7"))).asLong());
		var and = instance.getExport("and");
		assertEquals(longVal("0"), and.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("0"), and.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), and.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), and.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("0"), and.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("9223372036854775807"), and.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("4042322160"), and.apply(Value.i64(longVal("4042326015")), Value.i64(longVal("4294963440"))).asLong());
		assertEquals(longVal("18446744073709551615"), and.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		var or = instance.getExport("or");
		assertEquals(longVal("1"), or.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("1"), or.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), or.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), or.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("18446744073709551615"), or.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("9223372036854775808"), or.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("4294967295"), or.apply(Value.i64(longVal("4042326015")), Value.i64(longVal("4294963440"))).asLong());
		assertEquals(longVal("18446744073709551615"), or.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		var xor = instance.getExport("xor");
		assertEquals(longVal("1"), xor.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("1"), xor.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), xor.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), xor.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("18446744073709551615"), xor.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("9223372036854775808"), xor.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("9223372036854775807"), xor.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("9223372036854775808"), xor.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775807"))).asLong());
		assertEquals(longVal("252645135"), xor.apply(Value.i64(longVal("4042326015")), Value.i64(longVal("4294963440"))).asLong());
		assertEquals(longVal("0"), xor.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		var shl = instance.getExport("shl");
		assertEquals(longVal("2"), shl.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), shl.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("18446744073709551614"), shl.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("18446744073709551614"), shl.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("0"), shl.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("9223372036854775808"), shl.apply(Value.i64(longVal("4611686018427387904")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("9223372036854775808"), shl.apply(Value.i64(longVal("1")), Value.i64(longVal("63"))).asLong());
		assertEquals(longVal("1"), shl.apply(Value.i64(longVal("1")), Value.i64(longVal("64"))).asLong());
		assertEquals(longVal("2"), shl.apply(Value.i64(longVal("1")), Value.i64(longVal("65"))).asLong());
		assertEquals(longVal("9223372036854775808"), shl.apply(Value.i64(longVal("1")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("9223372036854775808"), shl.apply(Value.i64(longVal("1")), Value.i64(longVal("9223372036854775807"))).asLong());
		var shrs = instance.getExport("shr_s");
		assertEquals(longVal("0"), shrs.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), shrs.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("18446744073709551615"), shrs.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("4611686018427387903"), shrs.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("13835058055282163712"), shrs.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("2305843009213693952"), shrs.apply(Value.i64(longVal("4611686018427387904")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), shrs.apply(Value.i64(longVal("1")), Value.i64(longVal("64"))).asLong());
		assertEquals(longVal("0"), shrs.apply(Value.i64(longVal("1")), Value.i64(longVal("65"))).asLong());
		assertEquals(longVal("0"), shrs.apply(Value.i64(longVal("1")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), shrs.apply(Value.i64(longVal("1")), Value.i64(longVal("9223372036854775807"))).asLong());
		assertEquals(longVal("1"), shrs.apply(Value.i64(longVal("1")), Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("18446744073709551615"), shrs.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("63"))).asLong());
		assertEquals(longVal("18446744073709551615"), shrs.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("64"))).asLong());
		assertEquals(longVal("18446744073709551615"), shrs.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("65"))).asLong());
		assertEquals(longVal("18446744073709551615"), shrs.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("18446744073709551615"), shrs.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775807"))).asLong());
		assertEquals(longVal("18446744073709551615"), shrs.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asLong());
		var shru = instance.getExport("shr_u");
		assertEquals(longVal("0"), shru.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), shru.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("9223372036854775807"), shru.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("4611686018427387903"), shru.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("4611686018427387904"), shru.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("2305843009213693952"), shru.apply(Value.i64(longVal("4611686018427387904")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), shru.apply(Value.i64(longVal("1")), Value.i64(longVal("64"))).asLong());
		assertEquals(longVal("0"), shru.apply(Value.i64(longVal("1")), Value.i64(longVal("65"))).asLong());
		assertEquals(longVal("0"), shru.apply(Value.i64(longVal("1")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), shru.apply(Value.i64(longVal("1")), Value.i64(longVal("9223372036854775807"))).asLong());
		assertEquals(longVal("1"), shru.apply(Value.i64(longVal("1")), Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("1"), shru.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("63"))).asLong());
		assertEquals(longVal("18446744073709551615"), shru.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("64"))).asLong());
		assertEquals(longVal("9223372036854775807"), shru.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("65"))).asLong());
		assertEquals(longVal("1"), shru.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("1"), shru.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775807"))).asLong());
		assertEquals(longVal("18446744073709551615"), shru.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asLong());
		var rotl = instance.getExport("rotl");
		assertEquals(longVal("2"), rotl.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), rotl.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("18446744073709551615"), rotl.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), rotl.apply(Value.i64(longVal("1")), Value.i64(longVal("64"))).asLong());
		assertEquals(longVal("6312693092936652189"), rotl.apply(Value.i64(longVal("12379718583323101902")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("16140901123551657999"), rotl.apply(Value.i64(longVal("18302628889324683264")), Value.i64(longVal("4"))).asLong());
		assertEquals(longVal("87109505680009935"), rotl.apply(Value.i64(longVal("12379570969274382345")), Value.i64(longVal("53"))).asLong());
		assertEquals(longVal("6190357836324913230"), rotl.apply(Value.i64(longVal("12380715672649826460")), Value.i64(longVal("63"))).asLong());
		assertEquals(longVal("87109505680009935"), rotl.apply(Value.i64(longVal("12379570969274382345")), Value.i64(longVal("245"))).asLong());
		assertEquals(longVal("14916262237559758314"), rotl.apply(Value.i64(longVal("12379676934707509257")), Value.i64(longVal("18446744073709551597"))).asLong());
		assertEquals(longVal("6190357836324913230"), rotl.apply(Value.i64(longVal("12380715672649826460")), Value.i64(longVal("9223372036854775871"))).asLong());
		assertEquals(longVal("9223372036854775808"), rotl.apply(Value.i64(longVal("1")), Value.i64(longVal("63"))).asLong());
		assertEquals(longVal("1"), rotl.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("1"))).asLong());
		var rotr = instance.getExport("rotr");
		assertEquals(longVal("9223372036854775808"), rotr.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), rotr.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("18446744073709551615"), rotr.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1"), rotr.apply(Value.i64(longVal("1")), Value.i64(longVal("64"))).asLong());
		assertEquals(longVal("6189859291661550951"), rotr.apply(Value.i64(longVal("12379718583323101902")), Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("1143914305582792704"), rotr.apply(Value.i64(longVal("18302628889324683264")), Value.i64(longVal("4"))).asLong());
		assertEquals(longVal("7534987797011123550"), rotr.apply(Value.i64(longVal("12379570969274382345")), Value.i64(longVal("53"))).asLong());
		assertEquals(longVal("6314687271590101305"), rotr.apply(Value.i64(longVal("12380715672649826460")), Value.i64(longVal("63"))).asLong());
		assertEquals(longVal("7534987797011123550"), rotr.apply(Value.i64(longVal("12379570969274382345")), Value.i64(longVal("245"))).asLong());
		assertEquals(longVal("10711665151168044651"), rotr.apply(Value.i64(longVal("12379676934707509257")), Value.i64(longVal("18446744073709551597"))).asLong());
		assertEquals(longVal("6314687271590101305"), rotr.apply(Value.i64(longVal("12380715672649826460")), Value.i64(longVal("9223372036854775871"))).asLong());
		assertEquals(longVal("2"), rotr.apply(Value.i64(longVal("1")), Value.i64(longVal("63"))).asLong());
		assertEquals(longVal("1"), rotr.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("63"))).asLong());
		var clz = instance.getExport("clz");
		assertEquals(longVal("0"), clz.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("64"), clz.apply(Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("48"), clz.apply(Value.i64(longVal("32768"))).asLong());
		assertEquals(longVal("56"), clz.apply(Value.i64(longVal("255"))).asLong());
		assertEquals(longVal("0"), clz.apply(Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("63"), clz.apply(Value.i64(longVal("1"))).asLong());
		assertEquals(longVal("62"), clz.apply(Value.i64(longVal("2"))).asLong());
		assertEquals(longVal("1"), clz.apply(Value.i64(longVal("9223372036854775807"))).asLong());
		var ctz = instance.getExport("ctz");
		assertEquals(longVal("0"), ctz.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("64"), ctz.apply(Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("15"), ctz.apply(Value.i64(longVal("32768"))).asLong());
		assertEquals(longVal("16"), ctz.apply(Value.i64(longVal("65536"))).asLong());
		assertEquals(longVal("63"), ctz.apply(Value.i64(longVal("9223372036854775808"))).asLong());
		assertEquals(longVal("0"), ctz.apply(Value.i64(longVal("9223372036854775807"))).asLong());
		var popcnt = instance.getExport("popcnt");
		assertEquals(longVal("64"), popcnt.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		assertEquals(longVal("0"), popcnt.apply(Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("1"), popcnt.apply(Value.i64(longVal("32768"))).asLong());
		assertEquals(longVal("4"), popcnt.apply(Value.i64(longVal("9223512776490647552"))).asLong());
		assertEquals(longVal("63"), popcnt.apply(Value.i64(longVal("9223372036854775807"))).asLong());
		assertEquals(longVal("32"), popcnt.apply(Value.i64(longVal("12297829381041378645"))).asLong());
		assertEquals(longVal("32"), popcnt.apply(Value.i64(longVal("11068046444512062122"))).asLong());
		assertEquals(longVal("48"), popcnt.apply(Value.i64(longVal("16045690984833335023"))).asLong());
		var extend8s = instance.getExport("extend8_s");
		assertEquals(longVal("0"), extend8s.apply(Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("127"), extend8s.apply(Value.i64(longVal("127"))).asLong());
		assertEquals(longVal("18446744073709551488"), extend8s.apply(Value.i64(longVal("128"))).asLong());
		assertEquals(longVal("18446744073709551615"), extend8s.apply(Value.i64(longVal("255"))).asLong());
		assertEquals(longVal("0"), extend8s.apply(Value.i64(longVal("81985529216486656"))).asLong());
		assertEquals(longVal("18446744073709551488"), extend8s.apply(Value.i64(longVal("18364758544493064832"))).asLong());
		assertEquals(longVal("18446744073709551615"), extend8s.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		var extend16s = instance.getExport("extend16_s");
		assertEquals(longVal("0"), extend16s.apply(Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("32767"), extend16s.apply(Value.i64(longVal("32767"))).asLong());
		assertEquals(longVal("18446744073709518848"), extend16s.apply(Value.i64(longVal("32768"))).asLong());
		assertEquals(longVal("18446744073709551615"), extend16s.apply(Value.i64(longVal("65535"))).asLong());
		assertEquals(longVal("0"), extend16s.apply(Value.i64(longVal("1311768467463733248"))).asLong());
		assertEquals(longVal("18446744073709518848"), extend16s.apply(Value.i64(longVal("18364758544493084672"))).asLong());
		assertEquals(longVal("18446744073709551615"), extend16s.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		var extend32s = instance.getExport("extend32_s");
		assertEquals(longVal("0"), extend32s.apply(Value.i64(longVal("0"))).asLong());
		assertEquals(longVal("32767"), extend32s.apply(Value.i64(longVal("32767"))).asLong());
		assertEquals(longVal("32768"), extend32s.apply(Value.i64(longVal("32768"))).asLong());
		assertEquals(longVal("65535"), extend32s.apply(Value.i64(longVal("65535"))).asLong());
		assertEquals(longVal("2147483647"), extend32s.apply(Value.i64(longVal("2147483647"))).asLong());
		assertEquals(longVal("18446744071562067968"), extend32s.apply(Value.i64(longVal("2147483648"))).asLong());
		assertEquals(longVal("18446744073709551615"), extend32s.apply(Value.i64(longVal("4294967295"))).asLong());
		assertEquals(longVal("0"), extend32s.apply(Value.i64(longVal("81985526906748928"))).asLong());
		assertEquals(longVal("18446744071562067968"), extend32s.apply(Value.i64(longVal("18364758544655319040"))).asLong());
		assertEquals(longVal("18446744073709551615"), extend32s.apply(Value.i64(longVal("18446744073709551615"))).asLong());
		var eqz = instance.getExport("eqz");
		assertEquals((int)(1L & 0xFFFFFFFFL), eqz.apply(Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eqz.apply(Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eqz.apply(Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eqz.apply(Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eqz.apply(Value.i64(longVal("18446744073709551615"))).asInt());
		var eq = instance.getExport("eq");
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), eq.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var ne = instance.getExport("ne");
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ne.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var lts = instance.getExport("lt_s");
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), lts.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var ltu = instance.getExport("lt_u");
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ltu.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var les = instance.getExport("le_s");
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), les.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var leu = instance.getExport("le_u");
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), leu.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var gts = instance.getExport("gt_s");
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gts.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var gtu = instance.getExport("gt_u");
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), gtu.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var ges = instance.getExport("ge_s");
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), ges.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
		var geu = instance.getExport("ge_u");
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("0")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("1")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("1")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("0")), Value.i64(longVal("1"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("0"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("0")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("18446744073709551615"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("18446744073709551615")), Value.i64(longVal("9223372036854775808"))).asInt());
		assertEquals((int)(1L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("9223372036854775808")), Value.i64(longVal("9223372036854775807"))).asInt());
		assertEquals((int)(0L & 0xFFFFFFFFL), geu.apply(Value.i64(longVal("9223372036854775807")), Value.i64(longVal("9223372036854775808"))).asInt());
	}
	@Test
	public void testI641Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.1.wasm").instantiate();
	}
	@Test
	public void testI642Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.2.wasm").instantiate();
	}
	@Test
	public void testI643Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.3.wasm").instantiate();
	}
	@Test
	public void testI644Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.4.wasm").instantiate();
	}
	@Test
	public void testI645Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.5.wasm").instantiate();
	}
	@Test
	public void testI646Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.6.wasm").instantiate();
	}
	@Test
	public void testI647Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.7.wasm").instantiate();
	}
	@Test
	public void testI648Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.8.wasm").instantiate();
	}
	@Test
	public void testI649Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.9.wasm").instantiate();
	}
	@Test
	public void testI6410Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.10.wasm").instantiate();
	}
	@Test
	public void testI6411Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.11.wasm").instantiate();
	}
	@Test
	public void testI6412Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.12.wasm").instantiate();
	}
	@Test
	public void testI6413Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.13.wasm").instantiate();
	}
	@Test
	public void testI6414Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.14.wasm").instantiate();
	}
	@Test
	public void testI6415Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.15.wasm").instantiate();
	}
	@Test
	public void testI6416Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.16.wasm").instantiate();
	}
	@Test
	public void testI6417Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.17.wasm").instantiate();
	}
	@Test
	public void testI6418Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.18.wasm").instantiate();
	}
	@Test
	public void testI6419Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.19.wasm").instantiate();
	}
	@Test
	public void testI6420Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.20.wasm").instantiate();
	}
	@Test
	public void testI6421Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.21.wasm").instantiate();
	}
	@Test
	public void testI6422Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.22.wasm").instantiate();
	}
	@Test
	public void testI6423Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.23.wasm").instantiate();
	}
	@Test
	public void testI6424Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.24.wasm").instantiate();
	}
	@Test
	public void testI6425Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.25.wasm").instantiate();
	}
	@Test
	public void testI6426Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.26.wasm").instantiate();
	}
	@Test
	public void testI6427Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.27.wasm").instantiate();
	}
	@Test
	public void testI6428Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.28.wasm").instantiate();
	}
	@Test
	public void testI6429Wasm() {
		var instance = Module.build("src/test/resources/wasm/specv1/i64.29.wasm").instantiate();
	}
}
