package com.dylibso.chicory.annotations.processor;

import com.dylibso.chicory.codegen.CodegenUtils;
import com.github.javaparser.printer.DefaultPrettyPrinter;
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
        return CodegenUtils.camelCaseToSnakeCase(name);
    }

    static String snakeCaseToCamelCase(String name, boolean className) {
        return CodegenUtils.snakeCaseToCamelCase(name, className);
    }

    static DefaultPrettyPrinter printer() {
        return CodegenUtils.printer();
    }

    static final class AbortProcessingException extends RuntimeException {}
}
