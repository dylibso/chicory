package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WasmValue {

    @JsonProperty("type")
    private WasmValueType type;

    @JsonProperty("value")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private String[] value;

    @JsonProperty("lane_type")
    private LaneType laneType;

    public WasmValueType type() {
        return type;
    }

    public LaneType laneType() {
        return laneType;
    }

    public String toJavaValue() {
        switch (type) {
            case I32:
                return "Integer.parseUnsignedInt(\"" + value[0] + "\")";
            case I64:
                return "Long.parseUnsignedLong(\"" + value[0] + "\")";
            case F32:
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
                if (value[0].toString().equals("null")) {
                    return "Value.EXTREF_NULL";
                }
                return value[0];
            case FUNC_REF:
                if (value[0].toString().equals("null")) {
                    return "Value.FUNCREF_NULL";
                }
                return value[0];
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }

    public String toWasmValue() {
        switch (type) {
            case I32:
                return "Value.i32(" + toJavaValue() + ")";
            case I64:
                return "Value.i64(" + toJavaValue() + ")";
            case F32:
                return "Value.f32(Integer.parseUnsignedInt(\"" + value[0] + "\"))";
            case F64:
                return "Value.f64(Long.parseUnsignedLong(\"" + value[0] + "\"))";
            case EXTERN_REF:
                if (value[0].toString().equals("null")) {
                    return "Value.EXTREF_NULL";
                }
                return "Value.externRef(" + value[0] + ")";
            case FUNC_REF:
                if (value[0].toString().equals("null")) {
                    return "Value.FUNCREF_NULL";
                }
                return "Value.funcRef(" + value[0] + ")";
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }

    public String extractType() {
        if (value[0] == null || value[0].equals("null")) {
            return "";
        } else {
            switch (type) {
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
