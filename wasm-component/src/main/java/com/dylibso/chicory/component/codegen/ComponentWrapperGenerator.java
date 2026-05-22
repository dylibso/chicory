package com.dylibso.chicory.component.codegen;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.annotation.WitComponent;
import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.VariantType;
import com.dylibso.chicory.component.types.WitType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates a typed component wrapper class from the component definition.
 *
 * <p>Produces a Java class that provides type-safe methods for all component exports,
 * with automatic marshalling/unmarshalling of parameters and return values.
 *
 * <p>Generated wrapper provides:
 * <ul>
 *   <li>One typed method per export function
 *   <li>Automatic parameter marshalling (Java objects to WASM format)
 *   <li>Automatic return value unmarshalling (WASM format to Java objects)
 *   <li>Support for all complex types (records, variants, lists)
 * </ul>
 */
public class ComponentWrapperGenerator {
    private final ComponentDefinition componentDefinition;
    private final String packageName;

    public ComponentWrapperGenerator(ComponentDefinition componentDefinition, String packageName) {
        this.componentDefinition = componentDefinition;
        this.packageName = packageName;
    }

    /**
     * Generate the typed component wrapper class.
     *
     * @param outputDirectory directory where the wrapper class will be written
     * @throws IOException if unable to write file
     */
    public void generate(Path outputDirectory) throws IOException {
        String className = generateClassName();
        String classCode = generateClassCode(className);

        // Write to file
        Path packageDir = outputDirectory;
        for (String part : packageName.split("\\.")) {
            packageDir = packageDir.resolve(part);
        }
        Files.createDirectories(packageDir);

        Path classFile = packageDir.resolve(className + ".java");
        Files.write(classFile, classCode.getBytes(StandardCharsets.UTF_8));
    }

    private String generateClassName() {
        String interfaceName = componentDefinition.interfaceName();
        return CodeFormatter.capitalize(toCamelCase(interfaceName)) + "Component";
    }

    private String generateClassCode(String className) {
        StringBuilder sb = new StringBuilder();

        // Package and imports
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import ").append(WitComponent.class.getName()).append(";\n");
        sb.append("import com.dylibso.chicory.component.ComponentModel;\n");
        sb.append("import com.dylibso.chicory.runtime.Memory;\n");
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.List;\n\n");

        // Class declaration
        sb.append("@WitComponent(\"").append(componentDefinition.interfaceName()).append("\")\n");
        sb.append("public class ").append(className).append(" {\n");
        sb.append(CodeFormatter.indent(1))
                .append("private final ComponentModel componentModel;\n\n");

        // Constructor
        sb.append(CodeFormatter.indent(1))
                .append("public ")
                .append(className)
                .append("(ComponentModel componentModel) {\n");
        sb.append(CodeFormatter.indent(2)).append("this.componentModel = componentModel;\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // Export methods
        for (ComponentDefinition.FunctionSignature export : componentDefinition.exports()) {
            generateExportMethod(sb, export);
        }

        // Class closing brace
        sb.append("}\n");

        return sb.toString();
    }

    private void generateExportMethod(
            StringBuilder sb, ComponentDefinition.FunctionSignature export) {
        String methodName = toCamelCase(export.name());

        // Return type
        String returnType;
        if (export.returns().isEmpty()) {
            returnType = "void";
        } else if (export.returns().size() == 1) {
            returnType = witTypeToJavaType(export.returns().get(0));
        } else {
            returnType = "Object[]";
        }

        sb.append(CodeFormatter.indent(1))
                .append("public ")
                .append(returnType)
                .append(" ")
                .append(methodName)
                .append("(");

        // Parameters
        for (int i = 0; i < export.parameters().size(); i++) {
            if (i > 0) sb.append(", ");
            ComponentDefinition.FunctionSignature.Parameter param = export.parameters().get(i);
            String javaType = witTypeToJavaType(param.type);
            sb.append(javaType).append(" ").append(param.name);
        }

        sb.append(") throws Exception {\n");

        // Method body - marshal parameters
        for (ComponentDefinition.FunctionSignature.Parameter param : export.parameters()) {
            if (isComplexType(param.type)) {
                // Complex types need encoding
                if (param.type instanceof RecordType) {
                    sb.append(CodeFormatter.indent(2))
                            .append("long[] __")
                            .append(param.name)
                            .append("_encoded = ")
                            .append(param.name)
                            .append(".encode(componentModel.getMemory());\n");
                }
            }
        }

        // Call export function
        sb.append(CodeFormatter.indent(2))
                .append("Object result = componentModel.callExport(\"")
                .append(export.name())
                .append("\"");
        for (ComponentDefinition.FunctionSignature.Parameter param : export.parameters()) {
            sb.append(", ");
            if (isComplexType(param.type) && param.type instanceof RecordType) {
                sb.append("__").append(param.name).append("_encoded");
            } else {
                sb.append(param.name);
            }
        }
        sb.append(");\n");

        // Unmarshal return value
        if (export.returns().isEmpty()) {
            // Void return
        } else if (export.returns().size() == 1) {
            WitType returnWitType = export.returns().get(0);
            if (isComplexType(returnWitType) && returnWitType instanceof RecordType) {
                RecordType recordType = (RecordType) returnWitType;
                String recordClass =
                        CodeFormatter.capitalize(toCamelCase(recordType.displayName()));
                sb.append(CodeFormatter.indent(2))
                        .append("return ")
                        .append(recordClass)
                        .append(".decode((long[]) result, componentModel.getMemory());\n");
            } else if (isComplexType(returnWitType) && returnWitType instanceof VariantType) {
                VariantType variantType = (VariantType) returnWitType;
                String variantClass =
                        CodeFormatter.capitalize(toCamelCase(variantType.displayName()));
                sb.append(CodeFormatter.indent(2))
                        .append("return ")
                        .append(variantClass)
                        .append(".decode((long[]) result, componentModel.getMemory());\n");
            } else if (returnWitType instanceof PrimitiveType) {
                PrimitiveType prim = (PrimitiveType) returnWitType;
                if (prim == PrimitiveType.STRING) {
                    sb.append(CodeFormatter.indent(2)).append("return (String) result;\n");
                } else if (prim == PrimitiveType.I32) {
                    sb.append(CodeFormatter.indent(2))
                            .append("return ((Number) result).intValue();\n");
                } else if (prim == PrimitiveType.I64) {
                    sb.append(CodeFormatter.indent(2))
                            .append("return ((Number) result).longValue();\n");
                } else if (prim == PrimitiveType.F32) {
                    sb.append(CodeFormatter.indent(2))
                            .append("return ((Number) result).floatValue();\n");
                } else if (prim == PrimitiveType.F64) {
                    sb.append(CodeFormatter.indent(2))
                            .append("return ((Number) result).doubleValue();\n");
                } else if (prim == PrimitiveType.BOOL) {
                    sb.append(CodeFormatter.indent(2)).append("return (Boolean) result;\n");
                } else {
                    sb.append(CodeFormatter.indent(2)).append("return result;\n");
                }
            } else if (returnWitType instanceof ListType) {
                sb.append(CodeFormatter.indent(2)).append("return (List<?>) result;\n");
            } else {
                sb.append(CodeFormatter.indent(2)).append("return result;\n");
            }
        } else {
            sb.append(CodeFormatter.indent(2)).append("return (Object[]) result;\n");
        }

        sb.append(CodeFormatter.indent(1)).append("}\n\n");
    }

    private boolean isComplexType(WitType type) {
        return type instanceof RecordType
                || type instanceof VariantType
                || type instanceof ListType;
    }

    private String witTypeToJavaType(WitType type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) type;
            switch (prim) {
                case I32:
                    return "int";
                case I64:
                    return "long";
                case F32:
                    return "float";
                case F64:
                    return "double";
                case BOOL:
                    return "boolean";
                case CHAR:
                    return "char";
                case STRING:
                    return "String";
                default:
                    return "Object";
            }
        }
        if (type instanceof RecordType) {
            return CodeFormatter.capitalize(toCamelCase(((RecordType) type).displayName()));
        }
        if (type instanceof VariantType) {
            return CodeFormatter.capitalize(toCamelCase(((VariantType) type).displayName()));
        }
        if (type instanceof ListType) {
            ListType listType = (ListType) type;
            return "List<" + witTypeToJavaType(listType.elementType()) + ">";
        }
        return "Object";
    }

    private String toCamelCase(String snakeCase) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
