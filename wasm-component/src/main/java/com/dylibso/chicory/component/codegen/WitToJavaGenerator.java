package com.dylibso.chicory.component.codegen;

import com.dylibso.chicory.component.ComponentDefinition;
import java.nio.file.Path;

/**
 * Main orchestrator for generating Java POJOs and component wrappers from WIT definitions.
 *
 * <p>Coordinates sub-generators for records, variants, and component wrappers. Uses
 * ComponentDefinition and TypeRegistry to access type metadata.
 */
public class WitToJavaGenerator {
    private final ComponentDefinition componentDefinition;
    private final Path outputDirectory;
    private final String packageName;
    private final RecordGenerator recordGenerator;
    private final VariantGenerator variantGenerator;
    private final ComponentWrapperGenerator componentWrapperGenerator;

    /**
     * Creates a new generator for the given component definition.
     *
     * @param componentDefinition parsed WIT component definition
     * @param outputDirectory directory where generated sources will be written
     * @param packageName Java package for generated classes
     */
    public WitToJavaGenerator(
            ComponentDefinition componentDefinition, Path outputDirectory, String packageName) {
        this.componentDefinition = componentDefinition;
        this.outputDirectory = outputDirectory;
        this.packageName = packageName;
        this.recordGenerator = new RecordGenerator(componentDefinition, packageName);
        this.variantGenerator = new VariantGenerator(componentDefinition, packageName);
        this.componentWrapperGenerator =
                new ComponentWrapperGenerator(componentDefinition, packageName);
    }

    /**
     * Generate all Java classes from the component definition.
     *
     * <p>Generates:
     * <ul>
     *   <li>Record POJOs with encode/decode methods
     *   <li>Variant classes with sealed class hierarchy
     *   <li>Component wrapper with typed export methods
     * </ul>
     *
     * @throws java.io.IOException if unable to write generated files
     */
    public void generate() throws java.io.IOException {
        recordGenerator.generateAll(outputDirectory);
        variantGenerator.generateAll(outputDirectory);
        componentWrapperGenerator.generate(outputDirectory);
    }

    public ComponentDefinition getComponentDefinition() {
        return componentDefinition;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public String getPackageName() {
        return packageName;
    }
}
