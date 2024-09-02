package com.dylibso.chicory.function.processor;

import static com.github.javaparser.StaticJavaParser.parseType;
import static com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption.COLUMN_ALIGN_PARAMETERS;
import static java.lang.String.format;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public final class FunctionProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(HostModule.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (Element element : round.getElementsAnnotatedWith(HostModule.class)) {
            log(NOTE, "Generating module factory for " + element, null);
            try {
                processModule((TypeElement) element);
            } catch (AbortProcessingException e) {
                // skip type
            }
        }

        return false;
    }

    private void processModule(TypeElement type) {
        var moduleName = type.getAnnotation(HostModule.class).value();

        var functions = new NodeList<Expression>();
        for (Element member : elements().getAllMembers(type)) {
            if (member instanceof ExecutableElement && annotatedWith(member, WasmExport.class)) {
                functions.add(processMethod(member, (ExecutableElement) member, moduleName));
            }
        }

        var cu = new CompilationUnit(type.getEnclosingElement().toString());
        cu.addImport("com.dylibso.chicory.runtime.HostFunction");
        cu.addImport("com.dylibso.chicory.runtime.Instance");
        cu.addImport("com.dylibso.chicory.wasm.types.Value");
        cu.addImport("com.dylibso.chicory.wasm.types.ValueType");
        cu.addImport("java.util.List");

        var typeName = type.getSimpleName().toString();
        var processorName = new StringLiteralExpr(getClass().getName());
        var classDef =
                cu.addClass(typeName + "_ModuleFactory")
                        .setPublic(true)
                        .setFinal(true)
                        .addSingleMemberAnnotation(Generated.class, processorName);

        classDef.addConstructor().setPrivate(true);

        var newHostFunctions =
                new ArrayCreationExpr(
                        parseType("HostFunction"),
                        new NodeList<>(new ArrayCreationLevel()),
                        new ArrayInitializerExpr(functions));

        classDef.addMethod("toHostFunctions")
                .setPublic(true)
                .setStatic(true)
                .addParameter(typeName, "functions")
                .setType("HostFunction[]")
                .setBody(new BlockStmt(new NodeList<>(new ReturnStmt(newHostFunctions))));

        String qualifiedName = type.getQualifiedName() + "_ModuleFactory";
        try (Writer writer = filer().createSourceFile(qualifiedName, type).openWriter()) {
            writer.write(cu.printer(printer()).toString());
        } catch (IOException e) {
            log(ERROR, format("Failed to create %s file: %s", qualifiedName, e), null);
        }
    }

    private Expression processMethod(
            Element member, ExecutableElement executable, String moduleName) {
        // compute function name
        var name = executable.getAnnotation(WasmExport.class).value();
        if (name.isEmpty()) {
            name = camelCaseToSnakeCase(executable.getSimpleName().toString());
        }

        // compute parameter types and argument conversions
        NodeList<Expression> paramTypes = new NodeList<>();
        NodeList<Expression> arguments = new NodeList<>();
        for (VariableElement parameter : executable.getParameters()) {
            var argExpr =
                    new ArrayAccessExpr(
                            new NameExpr("args"),
                            new IntegerLiteralExpr(String.valueOf(paramTypes.size())));
            switch (parameter.asType().toString()) {
                case "int":
                    paramTypes.add(valueType("I32"));
                    arguments.add(new MethodCallExpr(argExpr, "asInt"));
                    break;
                case "long":
                    paramTypes.add(valueType("I64"));
                    arguments.add(new MethodCallExpr(argExpr, "asLong"));
                    break;
                case "float":
                    paramTypes.add(valueType("F32"));
                    arguments.add(new MethodCallExpr(argExpr, "asFloat"));
                    break;
                case "double":
                    paramTypes.add(valueType("F64"));
                    arguments.add(new MethodCallExpr(argExpr, "asDouble"));
                    break;
                case "com.dylibso.chicory.runtime.Instance":
                    arguments.add(new NameExpr("instance"));
                    break;
                case "com.dylibso.chicory.runtime.Memory":
                    arguments.add(new MethodCallExpr(new NameExpr("instance"), "memory"));
                    break;
                default:
                    log(ERROR, "Unsupported WASM type: " + parameter.asType(), parameter);
                    throw new AbortProcessingException();
            }
        }

        // compute return type and conversion
        String returnName = executable.getReturnType().toString();
        NodeList<Expression> returnType = new NodeList<>();
        String returnExpr = null;
        switch (returnName) {
            case "void":
                break;
            case "int":
                returnType.add(valueType("I32"));
                returnExpr = "i32";
                break;
            case "long":
                returnType.add(valueType("I64"));
                returnExpr = "i64";
                break;
            case "float":
                returnType.add(valueType("F32"));
                returnExpr = "fromFloat";
                break;
            case "double":
                returnType.add(valueType("F64"));
                returnExpr = "fromDouble";
                break;
            default:
                log(ERROR, "Unsupported WASM type: " + returnName, executable);
                throw new AbortProcessingException();
        }

        // function invocation
        Expression invocation =
                new MethodCallExpr(
                        new NameExpr("functions"), member.getSimpleName().toString(), arguments);

        // convert return value
        BlockStmt handleBody = new BlockStmt();
        if (returnType.isEmpty()) {
            handleBody.addStatement(invocation).addStatement(new ReturnStmt(new NullLiteralExpr()));
        } else {
            var result = new VariableDeclarator(parseType(returnName), "result", invocation);
            var boxed =
                    new MethodCallExpr(
                            new NameExpr("Value"),
                            returnExpr,
                            new NodeList<>(new NameExpr("result")));
            var wrapped =
                    new ArrayCreationExpr(
                            parseType("Value"),
                            new NodeList<>(new ArrayCreationLevel()),
                            new ArrayInitializerExpr(new NodeList<>(boxed)));
            handleBody
                    .addStatement(new ExpressionStmt(new VariableDeclarationExpr(result)))
                    .addStatement(new ReturnStmt(wrapped));
        }

        // lambda for host function
        var handle =
                new LambdaExpr()
                        .addParameter("Instance", "instance")
                        .addParameter(new Parameter(parseType("Value"), "args").setVarArgs(true))
                        .setEnclosingParameters(true)
                        .setBody(handleBody);

        // create host function
        var function =
                new ObjectCreationExpr()
                        .setType("HostFunction")
                        .addArgument(handle)
                        .addArgument(new StringLiteralExpr(moduleName))
                        .addArgument(new StringLiteralExpr(name))
                        .addArgument(new MethodCallExpr(new NameExpr("List"), "of", paramTypes))
                        .addArgument(new MethodCallExpr(new NameExpr("List"), "of", returnType));
        // TODO: update javaparser and replace with multiline formatting
        function.setLineComment("");
        return function;
    }

    private Elements elements() {
        return processingEnv.getElementUtils();
    }

    private Filer filer() {
        return processingEnv.getFiler();
    }

    private void log(Diagnostic.Kind kind, String message, Element element) {
        processingEnv.getMessager().printMessage(kind, message, element);
    }

    private static boolean annotatedWith(Element element, Class<? extends Annotation> annotation) {
        var annotationName = annotation.getName();
        return element.getAnnotationMirrors().stream()
                .map(AnnotationMirror::getAnnotationType)
                .map(TypeMirror::toString)
                .anyMatch(annotationName::equals);
    }

    private static Expression valueType(String type) {
        return new FieldAccessExpr(new NameExpr("ValueType"), type);
    }

    private static String camelCaseToSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static DefaultPrettyPrinter printer() {
        return new DefaultPrettyPrinter(
                new DefaultPrinterConfiguration()
                        .addOption(new DefaultConfigurationOption(COLUMN_ALIGN_PARAMETERS, true)));
    }

    private static final class AbortProcessingException extends RuntimeException {}
}
