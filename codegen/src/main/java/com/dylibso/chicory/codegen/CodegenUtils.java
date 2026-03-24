package com.dylibso.chicory.codegen;

import static com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption.COLUMN_ALIGN_PARAMETERS;

import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import java.util.Locale;

public final class CodegenUtils {

    private CodegenUtils() {}

    public static String camelCaseToSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
    }

    public static String snakeCaseToCamelCase(String name, boolean className) {
        var sb = new StringBuilder();
        var toUppercase = className;
        for (int i = 0; i < name.length(); i++) {
            var c = name.charAt(i);
            if ((c == '_' || c == '-' || !Character.isJavaIdentifierPart(c)) && i != 0) {
                toUppercase = true;
            } else if (toUppercase) {
                sb.append(Character.toUpperCase(c));
                toUppercase = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static DefaultPrettyPrinter printer() {
        return new DefaultPrettyPrinter(
                new DefaultPrinterConfiguration()
                        .addOption(new DefaultConfigurationOption(COLUMN_ALIGN_PARAMETERS, true)));
    }
}
