package com.dylibso.chicory.maven.wast;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WasmValue {

    @JsonProperty("type")
    private WasmValueType type;

    @JsonProperty("value")
    private String value;

    public WasmValueType type() {
        return type;
    }

    public String value() {
        return value;
    }

    public String toResultValue(String result) {
        switch (type) {
            case I64:
                return result;
            case I32:
                return "(int) " + result;
            case F32:
                return "Float.intBitsToFloat((int) " + result + "), 0.0";
            case F64:
                return "Double.longBitsToDouble(" + result + "), 0.0";
            case EXTERN_REF:
            case FUNC_REF:
                if (result.equals("null")) {
                    return "Value.REF_NULL_VALUE";
                }
                return result;
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }

    public String toExpectedValue() {
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
            case FUNC_REF:
                if (value.equals("null")) {
                    return "Value.REF_NULL_VALUE";
                }
                return value;
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }

    public String toArgsValue() {
        switch (type) {
            case I32:
            case F32:
                if (value != null) {
                    switch (value) {
                        case "nan:canonical":
                        case "nan:arithmetic":
                            return "(int) Float.NaN";
                        default:
                            return "Integer.parseUnsignedInt(\"" + value + "\")";
                    }
                } else {
                    return "null";
                }
            case I64:
            case F64:
                if (value != null) {
                    switch (value) {
                        case "nan:canonical":
                        case "nan:arithmetic":
                            return "(long) Double.NaN";
                        default:
                            return "Long.parseUnsignedLong(\"" + value + "\")";
                    }
                } else {
                    return "null";
                }
            case EXTERN_REF:
            case FUNC_REF:
                if (value.toString().equals("null")) {
                    return "Value.REF_NULL_VALUE";
                }
                return value;
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }
}
