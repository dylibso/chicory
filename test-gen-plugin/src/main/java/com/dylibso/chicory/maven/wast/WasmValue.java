package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.stream.Collectors;

public class WasmValue {

    @JsonProperty("type")
    WasmValueType type;

    @JsonProperty("value")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    String[] value;

    @JsonProperty("lane_type")
    WasmValueType laneType;

    public WasmValueType type() {
        return type;
    }

    private WasmValue fromValue(String val, WasmValueType typ) {
        var ret = new WasmValue();
        ret.type = typ;
        ret.value = value;
        return ret;
    }

    public String[] value() {
        return value;
    }

    public String toJavaValue() {
        switch (type) {
            case I8:
            case I16:
            case I32:
                assert (value.length == 1);
                return "Integer.parseUnsignedInt(\"" + value[0] + "\")";
            case I64:
                assert (value.length == 1);
                return "Long.parseUnsignedLong(\"" + value[0] + "\")";
            case F32:
                assert (value.length == 1);
                if (value[0] != null) {
                    switch (value[0]) {
                        case "nan:canonical":
                        case "nan:arithmetic":
                            return "Float.NaN";
                        default:
                            return "Float.intBitsToFloat(Integer.parseUnsignedInt(\""
                                    + value[0]
                                    + "\"))";
                    }
                } else {
                    return "null";
                }
            case F64:
                assert (value.length == 1);
                if (value[0] != null) {
                    switch (value[0]) {
                        case "nan:canonical":
                        case "nan:arithmetic":
                            return "Double.NaN";
                        default:
                            return "Double.longBitsToDouble(Long.parseUnsignedLong(\""
                                    + value[0]
                                    + "\"))";
                    }
                } else {
                    return "null";
                }
            case EXTERN_REF:
                assert (value.length == 1);
                if (value[0].toString().equals("null")) {
                    return "Value.EXTREF_NULL";
                }
                return value[0];
            case FUNC_REF:
                assert (value.length == 1);
                if (value[0].toString().equals("null")) {
                    return "Value.FUNCREF_NULL";
                }
                return value[0];
            case VEC_REF:
                if (value.length == 1 && value.toString().equals("null")) {
                    return "Value.VECREF_NULL";
                }

                return "new Value[]{ "
                        + Arrays.stream(value)
                                .map(v -> fromValue(v, laneType).toWasmValue())
                                .collect(Collectors.joining(", "))
                        + "}";
            default:
                throw new IllegalArgumentException("Type not recognized " + laneType);
        }
    }

    public String toWasmValue() {
        switch (type) {
            case I8:
            case I16:
            case I32:
                assert (value.length == 1);
                return "Value.i32(" + toJavaValue() + ")";
            case I64:
                assert (value.length == 1);
                return "Value.i64(" + toJavaValue() + ")";
            case F32:
                assert (value.length == 1);
                return "Value.f32(Integer.parseUnsignedInt(\"" + value[0] + "\"))";
            case F64:
                assert (value.length == 1);
                return "Value.f64(Long.parseUnsignedLong(\"" + value[0] + "\"))";
            case EXTERN_REF:
                assert (value.length == 1);
                if (value[0].toString().equals("null")) {
                    return "Value.EXTREF_NULL";
                }
                return "Value.externRef(" + value[0] + ")";
            case FUNC_REF:
                assert (value.length == 1);
                if (value[0].toString().equals("null")) {
                    return "Value.FUNCREF_NULL";
                }
                return "Value.funcRef(" + value[0] + ")";
            case VEC_REF:
                if (value[0].toString().equals("null")) {
                    return "new Value[]{}";
                }

                return "new Value[]{ "
                        + Arrays.stream(value)
                                .map(v -> fromValue(v, laneType).toJavaValue())
                                .collect(Collectors.joining(", "))
                        + "}";
            default:
                throw new IllegalArgumentException("Type not recognized " + laneType);
        }
    }

    public String extractType() {
        if (value == null || value.equals("null")) {
            return "";
        } else {
            switch (type) {
                case I8:
                case I16:
                case I32:
                    return ".asInt()";
                case I64:
                    return ".asLong()";
                case F32:
                    return ".asFloat()";
                case F64:
                    return ".asDouble()";
                case EXTERN_REF:
                    return ".asExtRef()";
                case FUNC_REF:
                    return ".asFuncRef()";
                case VEC_REF:
                    return ".asVecRef()";
                default:
                    throw new IllegalArgumentException("Type not recognized " + type);
            }
        }
    }

    public String delta() {
        switch (type) {
            case F32:
            case F64:
                return ", 0.0";
            default:
                return "";
        }
    }
}
