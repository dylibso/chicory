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
            case V128:
                {
                    var sb = new StringBuilder();
                    switch (laneType) {
                        case I8:
                            sb.append("new byte[] {");
                            break;
                        case I16:
                            sb.append("new int[] {");
                            break;
                        case I32:
                            sb.append("new long[] {");
                            break;
                        case I64:
                            sb.append("new long[] {");
                            break;
                        case F32:
                            sb.append("new float[] {");
                            break;
                        case F64:
                            sb.append("new double[] {");
                            break;
                    }
                    var first = true;
                    for (var v : value) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }

                        switch (laneType) {
                            case I8:
                                sb.append("Byte.parseByte(\"" + v + "\")");
                                break;
                            case I16:
                                sb.append("Integer.parseUnsignedInt(\"" + v + "\")");
                                break;
                            case I32:
                                sb.append("Long.parseUnsignedLong(\"" + v + "\")");
                                break;
                            case I64:
                                sb.append("Long.parseUnsignedLong(\"" + v + "\")");
                                break;
                            case F32:
                                sb.append(
                                        "Float.intBitsToFloat(Integer.parseUnsignedInt(\""
                                                + v
                                                + "\"))");
                                break;
                            case F64:
                                sb.append(
                                        "Double.longBitsToDouble(Long.parseUnsignedLong(\""
                                                + v
                                                + "\"))");
                                break;
                        }
                    }
                    sb.append(" }");
                    return sb.toString();
                }
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }

    public String toExpectedValue() {
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
            case FUNC_REF:
                if (value[0].equals("null")) {
                    return "Value.REF_NULL_VALUE";
                }
                return value[0];
            case V128:
                {
                    var sb = new StringBuilder();
                    switch (laneType) {
                        case I8:
                            sb.append("new byte[] {");
                            break;
                        case I16:
                            sb.append("new int[] {");
                            break;
                        case I32:
                            sb.append("new long[] {");
                            break;
                        case I64:
                            sb.append("new long[] {");
                            break;
                        case F32:
                            sb.append("new float[] {");
                            break;
                        case F64:
                            sb.append("new double[] {");
                            break;
                    }
                    var first = true;
                    for (var v : value) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }

                        switch (laneType) {
                            case I8:
                                sb.append("Byte.parseByte(\"" + v + "\")");
                                break;
                            case I16:
                            case F32:
                                sb.append("Integer.parseUnsignedInt(\"" + v + "\")");
                                break;
                            case I32:
                            case I64:
                            case F64:
                                sb.append("Long.parseUnsignedLong(\"" + v + "\")");
                                break;
                        }
                    }
                    sb.append(" }");
                    return sb.toString();
                }
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }

    public String toArgsValue() {
        switch (type) {
            case I32:
            case F32:
                if (value[0] != null) {
                    switch (value[0]) {
                        case "nan:canonical":
                        case "nan:arithmetic":
                            return "(int) Float.NaN";
                        default:
                            return "Integer.parseUnsignedInt(\"" + value[0] + "\")";
                    }
                } else {
                    return "null";
                }
            case I64:
            case F64:
                if (value[0] != null) {
                    switch (value[0]) {
                        case "nan:canonical":
                        case "nan:arithmetic":
                            return "(long) Double.NaN";
                        default:
                            return "Long.parseUnsignedLong(\"" + value[0] + "\")";
                    }
                } else {
                    return "null";
                }
            case EXTERN_REF:
            case FUNC_REF:
                if (value[0].toString().equals("null")) {
                    return "Value.REF_NULL_VALUE";
                }
                return value[0];
            case V128:
                var sb = new StringBuilder();

                switch (laneType) {
                    case I8:
                        sb.append("i8ToVec( ");
                        break;
                    case I16:
                        sb.append("i16ToVec( ");
                        break;
                    case I32:
                        sb.append("i32ToVec( ");
                        break;
                    case I64:
                        sb.append("i64ToVec( ");
                        break;
                    case F32:
                        sb.append("f32ToVec( ");
                        break;
                    case F64:
                        sb.append("f64ToVec( ");
                }

                sb.append("new long[] { ");
                var first = true;
                for (var v : value) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }

                    switch (laneType) {
                        case I8:
                            sb.append("Byte.parseByte(\"" + v + "\")");
                            break;
                        case I16:
                        case F32:
                            sb.append("Integer.parseUnsignedInt(\"" + v + "\")");
                            break;
                        case I32:
                        case I64:
                        case F64:
                            sb.append("Long.parseUnsignedLong(\"" + v + "\")");
                            break;
                    }
                }
                sb.append(" })");
                return sb.toString();
            default:
                throw new IllegalArgumentException("Type not recognized " + type);
        }
    }
}
