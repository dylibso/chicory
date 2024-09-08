package com.dylibso.chicory.function.processor;

import static com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption.COLUMN_ALIGN_PARAMETERS;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

abstract class AbstractModuleProcessor extends AbstractProcessor {
    private final Class<? extends Annotation> annotation;

    protected AbstractModuleProcessor(Class<? extends Annotation> annotation) {
        this.annotation = requireNonNull(annotation);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(annotation.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (Element element : round.getElementsAnnotatedWith(annotation)) {
            log(NOTE, "Generating module factory for " + element, null);
            try {
                processModule((TypeElement) element);
            } catch (AbortProcessingException e) {
                // skip type
            }
        }

        return false;
    }

    protected abstract void processModule(TypeElement type);

    protected Elements elements() {
        return processingEnv.getElementUtils();
    }

    protected Filer filer() {
        return processingEnv.getFiler();
    }

    protected void log(Diagnostic.Kind kind, String message, Element element) {
        processingEnv.getMessager().printMessage(kind, message, element);
    }

    protected void addGeneratedAnnotation(ClassOrInterfaceDeclaration classDef) {
        var processorName = new StringLiteralExpr(getClass().getName());
        classDef.addSingleMemberAnnotation(Generated.class, processorName);
    }

    protected void writeSourceFile(
            CompilationUnit cu, PackageElement pkg, TypeElement type, String suffix) {
        var prefix = (pkg.isUnnamed()) ? "" : pkg.getQualifiedName().toString() + ".";
        var qualifiedName = prefix + type.getSimpleName() + suffix;
        try (Writer writer = filer().createSourceFile(qualifiedName, type).openWriter()) {
            writer.write(cu.printer(printer()).toString());
        } catch (IOException e) {
            log(ERROR, format("Failed to create %s file: %s", qualifiedName, e), null);
        }
    }

    protected static DefaultPrettyPrinter printer() {
        return new DefaultPrettyPrinter(
                new DefaultPrinterConfiguration()
                        .addOption(new DefaultConfigurationOption(COLUMN_ALIGN_PARAMETERS, true)));
    }

    protected static PackageElement getPackageName(Element element) {
        Element enclosing = element;
        while (enclosing.getKind() != ElementKind.PACKAGE) {
            enclosing = enclosing.getEnclosingElement();
        }
        return (PackageElement) enclosing;
    }

    protected static boolean annotatedWith(
            Element element, Class<? extends Annotation> annotation) {
        var annotationName = annotation.getName();
        return element.getAnnotationMirrors().stream()
                .map(AnnotationMirror::getAnnotationType)
                .map(TypeMirror::toString)
                .anyMatch(annotationName::equals);
    }

    protected static CompilationUnit createCompilationUnit(PackageElement pkg, TypeElement type) {
        var packageName = pkg.getQualifiedName().toString();
        var cu = (pkg.isUnnamed()) ? new CompilationUnit() : new CompilationUnit(packageName);
        if (!pkg.isUnnamed()) {
            cu.setPackageDeclaration(packageName);
            cu.addImport(type.getQualifiedName().toString());
        }
        return cu;
    }

    protected static Expression valueType(String type) {
        return new FieldAccessExpr(new NameExpr("ValueType"), type);
    }

    protected static String camelCaseToSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
    }

    protected static final class AbortProcessingException extends RuntimeException {}
}
