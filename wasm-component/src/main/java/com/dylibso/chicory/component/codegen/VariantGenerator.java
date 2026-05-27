package com.dylibso.chicory.component.codegen;

import com.dylibso.chicory.component.ComponentDefinition;
import com.dylibso.chicory.component.annotation.WitCase;
import com.dylibso.chicory.component.annotation.WitVariant;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.VariantType;
import com.dylibso.chicory.component.types.WitType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates sealed variant classes from WIT variant definitions.
 *
 * <p>Produces Java classes that represent WIT variants (tagged unions) with:
 * <ul>
 *   <li>Abstract base class with @WitVariant annotation
 *   <li>Inner case classes (one per variant case) with @WitCase annotation
 *   <li>encode() and decode() methods for marshalling
 *   <li>Support for cases with and without associated data
 * </ul>
 */
public class VariantGenerator {
    private final ComponentDefinition componentDefinition;
    private final String packageName;

    public VariantGenerator(ComponentDefinition componentDefinition, String packageName) {
        this.componentDefinition = componentDefinition;
        this.packageName = packageName;
    }

    /**
     * Generate all variant classes for types in the component definition.
     *
     * @param outputDirectory directory where variant classes will be written
     * @throws IOException if unable to write files
     */
    public void generateAll(Path outputDirectory) throws IOException {
        for (VariantType variantType : componentDefinition.typeRegistry().getAllVariantTypes()) {
            generate(variantType, outputDirectory);
        }
    }

    /**
     * Generate a single variant class for the given variant type.
     *
     * @param variantType variant type to generate
     * @param outputDirectory directory where the class will be written
     * @throws IOException if unable to write file
     */
    public void generate(VariantType variantType, Path outputDirectory) throws IOException {
        String className = CodeFormatter.capitalize(toCamelCase(variantType.displayName()));
        String classCode = generateClassCode(className, variantType);

        // Write to file
        Path packageDir = outputDirectory;
        for (String part : packageName.split("\\.")) {
            packageDir = packageDir.resolve(part);
        }
        Files.createDirectories(packageDir);

        Path classFile = packageDir.resolve(className + ".java");
        Files.write(classFile, classCode.getBytes(StandardCharsets.UTF_8));
    }

    private String generateClassCode(String className, VariantType variantType) {
        StringBuilder sb = new StringBuilder();

        // Package and imports
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import ").append(WitVariant.class.getName()).append(";\n");
        sb.append("import ").append(WitCase.class.getName()).append(";\n");
        sb.append("import com.dylibso.chicory.component.VariantValue;\n");
        sb.append("import com.dylibso.chicory.runtime.Memory;\n");
        sb.append("import com.dylibso.chicory.component.CanonicalAbi;\n");
        sb.append("import com.dylibso.chicory.component.types.VariantType;\n");
        sb.append("import com.dylibso.chicory.component.PojoRegistry;\n");
        sb.append("import java.util.Optional;\n\n");

        // Abstract base class declaration
        sb.append("@WitVariant(\"").append(variantType.displayName()).append("\")\n");
        sb.append("public abstract class ").append(className).append(" {\n");
        sb.append(CodeFormatter.indent(1)).append("protected final String caseName;\n");
        sb.append(CodeFormatter.indent(1)).append("protected final Object data;\n\n");

        // Protected constructor
        sb.append(CodeFormatter.indent(1))
                .append("protected ")
                .append(className)
                .append("(String caseName, Object data) {\n");
        sb.append(CodeFormatter.indent(2)).append("this.caseName = caseName;\n");
        sb.append(CodeFormatter.indent(2)).append("this.data = data;\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // Getters
        sb.append(CodeFormatter.indent(1)).append("public String getCaseName() {\n");
        sb.append(CodeFormatter.indent(2)).append("return caseName;\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        sb.append(CodeFormatter.indent(1)).append("public Object getData() {\n");
        sb.append(CodeFormatter.indent(2)).append("return data;\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // encode() method
        sb.append(CodeFormatter.indent(1))
                .append("public long[] encode(Memory memory) throws Exception {\n");
        sb.append(CodeFormatter.indent(2))
                .append("VariantValue variant = new VariantValue(caseName, data);\n");
        sb.append(CodeFormatter.indent(2))
                .append("return CanonicalAbi.encode(variant, createVariantType(), memory);\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // Case inner classes
        for (int i = 0; i < variantType.cases().size(); i++) {
            VariantType.Case caseInfo = variantType.cases().get(i);
            generateCaseClass(sb, className, caseInfo, i);
        }

        // decode() static factory method
        sb.append(CodeFormatter.indent(1))
                .append("public static ")
                .append(className)
                .append(" decode(long[] encoded, Memory memory) throws Exception {\n");
        sb.append(CodeFormatter.indent(2))
                .append(
                        "Object obj = CanonicalAbi.decode(encoded, createVariantType(),"
                                + " memory);\n");
        sb.append(CodeFormatter.indent(2)).append("if (obj instanceof VariantValue) {\n");
        sb.append(CodeFormatter.indent(3)).append("VariantValue variant = (VariantValue) obj;\n");
        sb.append(CodeFormatter.indent(3)).append("switch (variant.caseName()) {\n");

        for (VariantType.Case caseInfo : variantType.cases()) {
            String caseName = caseInfo.name;
            String caseClassName = CodeFormatter.capitalize(toCamelCase(caseName));
            sb.append(CodeFormatter.indent(4)).append("case \"").append(caseName).append("\":\n");
            sb.append(CodeFormatter.indent(5))
                    .append("return new ")
                    .append(caseClassName)
                    .append("(variant.data());\n");
        }

        sb.append(CodeFormatter.indent(4)).append("default:\n");
        sb.append(CodeFormatter.indent(5))
                .append(
                        "throw new IllegalArgumentException(\"Unknown variant case: \" +"
                                + " variant.caseName());\n");
        sb.append(CodeFormatter.indent(3)).append("}\n");
        sb.append(CodeFormatter.indent(2)).append("}\n");
        sb.append(CodeFormatter.indent(2))
                .append("throw new IllegalArgumentException(\"Invalid decode result type\");\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // Helper method to create VariantType
        sb.append(CodeFormatter.indent(1))
                .append("private static VariantType createVariantType() {\n");
        sb.append(CodeFormatter.indent(2))
                .append("VariantType variant = new VariantType(\"")
                .append(variantType.displayName())
                .append("\");\n");
        for (VariantType.Case caseInfo : variantType.cases()) {
            sb.append(CodeFormatter.indent(2))
                    .append("variant.addCase(\"")
                    .append(caseInfo.name)
                    .append("\", ");
            if (caseInfo.type.isPresent()) {
                appendTypeCreation(sb, caseInfo.type.get());
            } else {
                sb.append("Optional.empty()");
            }
            sb.append(");\n");
        }
        sb.append(CodeFormatter.indent(2)).append("return variant;\n");
        sb.append(CodeFormatter.indent(1)).append("}\n\n");

        // Register with PojoRegistry
        sb.append(CodeFormatter.indent(1)).append("static {\n");
        sb.append(CodeFormatter.indent(2))
                .append("PojoRegistry.registerVariant(\"")
                .append(variantType.displayName())
                .append("\", ")
                .append(className)
                .append(".class);\n");
        sb.append(CodeFormatter.indent(1)).append("}\n");

        // Class closing brace
        sb.append("}\n");

        return sb.toString();
    }

    private void generateCaseClass(
            StringBuilder sb, String parentClassName, VariantType.Case caseInfo, int caseIndex) {
        String caseName = caseInfo.name;
        String caseClassName = CodeFormatter.capitalize(toCamelCase(caseName));
        String javaType =
                caseInfo.type.isPresent() ? witTypeToJavaType(caseInfo.type.get()) : "Void";

        sb.append("\n")
                .append(CodeFormatter.indent(1))
                .append("@WitCase(\"")
                .append(caseName)
                .append("\")\n");
        sb.append(CodeFormatter.indent(1))
                .append("public static class ")
                .append(caseClassName)
                .append(" extends ")
                .append(parentClassName)
                .append(" {\n");

        if (caseInfo.type.isPresent()) {
            // Case with data
            sb.append(CodeFormatter.indent(2))
                    .append("public final ")
                    .append(javaType)
                    .append(" value;\n\n");
            sb.append(CodeFormatter.indent(2))
                    .append("public ")
                    .append(caseClassName)
                    .append("(")
                    .append(javaType)
                    .append(" value) {\n");
            sb.append(CodeFormatter.indent(3))
                    .append("super(\"")
                    .append(caseName)
                    .append("\", value);\n");
            sb.append(CodeFormatter.indent(3)).append("this.value = value;\n");
            sb.append(CodeFormatter.indent(2)).append("}\n");
        } else {
            // Empty case (no data)
            sb.append(CodeFormatter.indent(2))
                    .append("public ")
                    .append(caseClassName)
                    .append("() {\n");
            sb.append(CodeFormatter.indent(3))
                    .append("super(\"")
                    .append(caseName)
                    .append("\", null);\n");
            sb.append(CodeFormatter.indent(2)).append("}\n");
        }

        sb.append(CodeFormatter.indent(1)).append("}\n");
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
        return "Object";
    }

    private void appendTypeCreation(StringBuilder sb, WitType type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) type;
            sb.append("Optional.of(PrimitiveType.").append(prim.name()).append(")");
        } else if (type instanceof RecordType) {
            sb.append("Optional.of(new RecordType(\"")
                    .append(((RecordType) type).displayName())
                    .append("\"))");
        } else {
            sb.append("Optional.empty()");
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
