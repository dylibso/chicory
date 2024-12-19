package com.dylibso.chicory.experimental.hostmodule.processor;

import static com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption.COLUMN_ALIGN_PARAMETERS;

import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import java.util.Locale;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public abstract class AbstractModuleProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    Elements elements() {
        return processingEnv.getElementUtils();
    }

    Filer filer() {
        return processingEnv.getFiler();
    }

    void log(Diagnostic.Kind kind, String message, Element element) {
        processingEnv.getMessager().printMessage(kind, message, element);
    }

    static PackageElement getPackageName(Element element) {
        Element enclosing = element;
        while (enclosing.getKind() != ElementKind.PACKAGE) {
            enclosing = enclosing.getEnclosingElement();
        }
        return (PackageElement) enclosing;
    }

    static String camelCaseToSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
    }

    static String snakeCaseToCamelCase(String name, boolean className) {
        var sb = new StringBuilder();
        var toUppercase = className;
        for (int i = 0; i < name.length(); i++) {
            var c = name.charAt(i);
            if (c == '_' || c == '-') {
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

    static DefaultPrettyPrinter printer() {
        return new DefaultPrettyPrinter(
                new DefaultPrinterConfiguration()
                        .addOption(new DefaultConfigurationOption(COLUMN_ALIGN_PARAMETERS, true)));
    }

    static final class AbortProcessingException extends RuntimeException {}
}
