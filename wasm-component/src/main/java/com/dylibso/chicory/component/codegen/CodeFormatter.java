package com.dylibso.chicory.component.codegen;

/**
 * Utility for formatting Java code generation output.
 */
public class CodeFormatter {
    private static final String INDENT = "    ";

    public static String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }

    public static String indent(int level, String text) {
        return indent(level) + text;
    }

    public static String formatPackage(String packageName) {
        return "package " + packageName + ";\n";
    }

    public static String formatImports(String... imports) {
        if (imports.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        return sb + "\n";
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
