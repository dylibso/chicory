package com.dylibso.chicory.annotations.processor;

import static java.lang.String.format;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

import com.dylibso.chicory.annotations.WasmModuleInterface;
import com.dylibso.chicory.codegen.ModuleInterfaceCodegen;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public final class WasmModuleProcessor extends AbstractModuleProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(WasmModuleInterface.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (Element element : round.getElementsAnnotatedWith(WasmModuleInterface.class)) {
            log(NOTE, "Generating wasm module helpers for " + element, null);
            try {
                processModule((TypeElement) element);
            } catch (URISyntaxException ex) {
                log(ERROR, "Failed to parse URI from wasmFile", element);
            } catch (IOException ex) {
                log(ERROR, "Failed to load wasmFile", element);
            } catch (AbortProcessingException e) {
                // skip type
            }
        }

        return false;
    }

    private void processModule(TypeElement type) throws URISyntaxException, IOException {
        var annot = type.getAnnotation(WasmModuleInterface.class);
        var wasmFile = annot.value();

        WasmModule module;
        if (wasmFile.startsWith("file:")) {
            module = Parser.parse(Path.of(new URI(wasmFile)));
        } else {
            FileObject fileObject =
                    processingEnv
                            .getFiler()
                            .getResource(StandardLocation.CLASS_OUTPUT, "", wasmFile);
            module = Parser.parse(fileObject.openInputStream());
        }

        var pkg = getPackageName(type);
        var packageName = pkg.getQualifiedName().toString();
        var typeName = type.getSimpleName().toString();

        var codegen =
                ModuleInterfaceCodegen.builder(module)
                        .withPackageName(packageName)
                        .withTypeName(typeName)
                        .withGeneratorName(getClass().getName())
                        .build();
        var writableClasses = codegen.generate();

        writableClasses.forEach((k, v) -> writeCu(k, v, type));
    }

    private void writeCu(String qualifiedName, CompilationUnit cu, TypeElement type) {
        try (Writer writer = filer().createSourceFile(qualifiedName, type).openWriter()) {
            writer.write(cu.printer(printer()).toString());
        } catch (IOException e) {
            log(ERROR, format("Failed to create %s file: %s", qualifiedName, e), null);
        }
    }
}
