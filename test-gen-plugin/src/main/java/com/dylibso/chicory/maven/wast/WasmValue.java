package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WasmValue {

    @JsonProperty("type")
    private WasmValueType type;

    @JsonProperty("value")
    private String value;

    public WasmValueType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String toJavaValue() {
        switch (type) {
            case I32:
                return "Integer.parseUnsignedInt(\"" + value + "\")";
            case I64:
                return "Long.parseUnsignedLong(\"" + value + "\")";
            case F32:
                if (value != null) {
                    switch (value) {
                        case "nan:canonical":
                        case "nan:arithmetic":
                            return "Float.NaN";
                        default:
                            return "Float.intBitsToFloat(Integer.parseUnsignedInt(\""
                                    + value
                                    + "\"))";
                    }
                } else {
                    return "null";
                }
            case F64:
                if (value != null) {
                    switch (value) {
                        case "nan:canonical":
                        case "nan:arithmetic":
                            return "Double.NaN";
                        default:
                            return "Double.longBitsToDouble(Long.parseUnsignedLong(\""
                                    + value
                                    + "\"))";
                    }
                } else {
                    return "null";
                }
            case EXTERN_REF:
                if (value.toString().equals("null")) {
                    return "Value.EXTREF_NULL";
                }
                return value;
            case FUNC_REF:
                if (value.toString().equals("null")) {
                    return "Value.FUNCREF_NULL";
                }
                return value;
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
                return "Value.f32(Integer.parseUnsignedInt(\"" + value + "\"))";
            case F64:
                return "Value.f64(Long.parseUnsignedLong(\"" + value + "\"))";
            case EXTERN_REF:
                if (value.toString().equals("null")) {
                    return "Value.EXTREF_NULL";
                }
                return "Value.externRef(" + value + ")";
            case FUNC_REF:
                if (value.toString().equals("null")) {
                    return "Value.FUNCREF_NULL";
                }
                return "Value.funcRef(" + value + ")";
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }

    public String extractType() {
        if (value == null || value.equals("null")) {
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

    public String getDelta() {
        switch (type) {
            case F32:
            case F64:
                return ", 0.0";
            default:
                return "";
        }
    }
}
