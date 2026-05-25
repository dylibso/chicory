package com.dylibso.chicory.component.codegen;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.annotation.WitField;
import com.dylibso.chicory.component.annotation.WitRecord;
import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.WitType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates Record POJOs with encode/decode methods from WIT record definitions.
 *
 * <p>Produces Java classes that represent WIT records with:
 * <ul>
 *   <li>Field declarations with proper types
 *   <li>Constructors and getters/setters
 *   <li>encode() and decode() methods for marshalling
 *   <li>@WitRecord annotations for reflection
 * </ul>
 */
public class RecordGenerator {
    private final ComponentDefinition componentDefinition;
    private final String packageName;

    public RecordGenerator(ComponentDefinition componentDefinition, String packageName) {
        this.componentDefinition = componentDefinition;
        this.packageName = packageName;
    }

    /**
     * Generate all record classes for types in the component definition.
     *
     * @param outputDirectory directory where record classes will be written
     * @throws IOException if unable to write files
     */
    public void generateAll(Path outputDirectory) throws IOException {
        for (RecordType recordType : componentDefinition.typeRegistry().getAllRecordTypes()) {
            generate(recordType, outputDirectory);
        }
    }

    /**
     * Generate a single record class for the given record type.
     *
     * @param recordType record type to generate
     * @param outputDirectory directory where the class will be written
     * @throws IOException if unable to write file
     */
    public void generate(RecordType recordType, Path outputDirectory) throws IOException {
        String className = CodeFormatter.capitalize(toCamelCase(recordType.displayName()));
        String classCode = generateClassCode(className, recordType);

        // Write to file
        Path packageDir = outputDirectory;
        for (String part : packageName.split("\\.")) {
            packageDir = packageDir.resolve(part);
        }
        Files.createDirectories(packageDir);

        Path classFile = packageDir.resolve(className + ".java");
        Files.write(classFile, classCode.getBytes(StandardCharsets.UTF_8));
    }

    private String generateClassCode(String className, RecordType recordType) {
        StringBuilder sb = new StringBuilder();

        // Package and imports
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import ").append(WitRecord.class.getName()).append(";\n");
        sb.append("import ").append(WitField.class.getName()).append(";\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.Map;\n");
        sb.append("import com.dylibso.chicory.runtime.Memory;\n");
        sb.append("import com.dylibso.chicory.component.CanonicalAbi;\n");
        sb.append("import com.dylibso.chicory.component.RecordLayout;\n");
        sb.append("import com.dylibso.chicory.component.types.RecordType;\n\n");

        // Class declaration
        sb.append("@WitRecord(\"").append(recordType.displayName()).append("\")\n");
        sb.append("public class ").append(className).append(" {\n");

        // Field declarations
        for (RecordType.Field field : recordType.fields()) {
            String javaType = witTypeToJavaType(field.type);
            String fieldName = field.name;
            sb.append(CodeFormatter.indent(1))
                    .append("@WitField(order = ")
                    .append(recordType.fields().indexOf(field))
                    .append(")\n");
            sb.append(CodeFormatter.indent(1))
                    .append("private ")
                    .append(javaType)
                    .append(" ")
                    .append(fieldName)
                    .append(";\n\n");
        }

        // No-arg constructor
        sb.append(CodeFormatter.indent(1)).append("public ").append(className).append("() {}\n\n");

        // All-args constructor
        sb.append(CodeFormatter.indent(1)).append("public ").append(className).append("(");
        for (int i = 0; i < recordType.fields().size(); i++) {
            if (i > 0) sb.append(", ");
            RecordType.Field field = recordType.fields().get(i);
            String javaType = witTypeToJavaType(field.type);
            sb.append(javaType).append(" ").append(field.name);
        }
        sb.append(") {\n");
        for (RecordType.Field field : recordType.fields()) {
            sb.append(CodeFormatter.indent(2))
                    .append("this.")
                    .append(field.name)
                    .append(" = ")
                    .append(field.name)
                    .append(";\n");
        }
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // Getters and setters
        for (RecordType.Field field : recordType.fields()) {
            String javaType = witTypeToJavaType(field.type);
            String fieldNameCamel = toCamelCase(field.name);

            // Getter
            sb.append(CodeFormatter.indent(1))
                    .append("public ")
                    .append(javaType)
                    .append(" get")
                    .append(CodeFormatter.capitalize(fieldNameCamel))
                    .append("() {\n");
            sb.append(CodeFormatter.indent(2)).append("return ").append(field.name).append(";\n");
            sb.append(CodeFormatter.indent(1)).append("}\n\n");

            // Setter
            sb.append(CodeFormatter.indent(1))
                    .append("public void set")
                    .append(CodeFormatter.capitalize(fieldNameCamel))
                    .append("(")
                    .append(javaType)
                    .append(" ")
                    .append(field.name)
                    .append(") {\n");
            sb.append(CodeFormatter.indent(2))
                    .append("this.")
                    .append(field.name)
                    .append(" = ")
                    .append(field.name)
                    .append(";\n");
            sb.append(CodeFormatter.indent(1)).append("}\n\n");
        }

        // encode() method - marshal to WASM memory
        sb.append(CodeFormatter.indent(1))
                .append("public long[] encode(Memory memory) throws Exception {\n");
        sb.append(CodeFormatter.indent(2)).append("Map<String, Object> map = new HashMap<>();\n");
        for (RecordType.Field field : recordType.fields()) {
            sb.append(CodeFormatter.indent(2))
                    .append("map.put(\"")
                    .append(field.name)
                    .append("\", ")
                    .append(field.name)
                    .append(");\n");
        }
        sb.append(CodeFormatter.indent(2))
                .append("return CanonicalAbi.encode(map, createRecordType(), memory);\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // decode() static method - unmarshal from WASM memory
        sb.append(CodeFormatter.indent(1))
                .append("public static ")
                .append(className)
                .append(" decode(long[] encoded, Memory memory) throws Exception {\n");
        sb.append(CodeFormatter.indent(2))
                .append("Object obj = CanonicalAbi.decode(encoded, createRecordType(), memory);\n");
        sb.append(CodeFormatter.indent(2)).append("if (obj instanceof Map) {\n");
        sb.append(CodeFormatter.indent(3)).append("@SuppressWarnings(\"unchecked\")\n");
        sb.append(CodeFormatter.indent(3))
                .append("Map<String, Object> map = (Map<String, Object>) obj;\n");
        sb.append(CodeFormatter.indent(3))
                .append(className)
                .append(" result = new ")
                .append(className)
                .append("();\n");
        for (RecordType.Field field : recordType.fields()) {
            String javaType = witTypeToJavaType(field.type);
            sb.append(CodeFormatter.indent(3))
                    .append("result.")
                    .append(field.name)
                    .append(" = (")
                    .append(javaType)
                    .append(") map.get(\"")
                    .append(field.name)
                    .append("\");\n");
        }
        sb.append(CodeFormatter.indent(3)).append("return result;\n");
        sb.append(CodeFormatter.indent(2)).append("}\n");
        sb.append(CodeFormatter.indent(2))
                .append("throw new IllegalArgumentException(\"Invalid decode result type\");\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // Helper method to create RecordType
        sb.append(CodeFormatter.indent(1))
                .append("private static RecordType createRecordType() {\n");
        sb.append(CodeFormatter.indent(2))
                .append("RecordType record = new RecordType(\"")
                .append(recordType.displayName())
                .append("\");\n");
        for (RecordType.Field field : recordType.fields()) {
            sb.append(CodeFormatter.indent(2))
                    .append("record.addField(\"")
                    .append(field.name)
                    .append("\", ");
            appendTypeCreation(sb, field.type);
            sb.append(");\n");
        }
        sb.append(CodeFormatter.indent(2)).append("return record;\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // Static initializer to register this POJO class
        sb.append(CodeFormatter.indent(1)).append("static {\n");
        sb.append(CodeFormatter.indent(2))
                .append("com.dylibso.chicory.component.PojoRegistry.register(\"")
                .append(recordType.displayName())
                .append("\", ")
                .append(className)
                .append(".class);\n");
        sb.append(CodeFormatter.indent(1)).append("}\n");

        // Class closing brace
        sb.append("}\n");

        return sb.toString();
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
            return CodeFormatter.capitalize(((RecordType) type).displayName());
        }
        if (type instanceof ListType) {
            ListType listType = (ListType) type;
            return "java.util.List<" + witTypeToJavaType(listType.elementType()) + ">";
        }
        return "Object";
    }

    private void appendTypeCreation(StringBuilder sb, WitType type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) type;
            sb.append("PrimitiveType.").append(prim.name());
        } else if (type instanceof RecordType) {
            sb.append("new RecordType(\"").append(((RecordType) type).displayName()).append("\")");
        } else {
            sb.append("null");
        }
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
