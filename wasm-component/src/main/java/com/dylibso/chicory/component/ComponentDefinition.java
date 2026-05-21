package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.TypeRegistry;
import com.dylibso.chicory.component.types.WitType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a parsed WIT component definition.
 * Contains all type definitions, function signatures, and metadata.
 */
public class ComponentDefinition {
    private final String packageName;
    private final String interfaceName;
    private final TypeRegistry typeRegistry;
    private final List<FunctionSignature> exports;
    private final List<FunctionSignature> imports;

    public ComponentDefinition(String packageName, String interfaceName) {
        this.packageName = packageName;
        this.interfaceName = interfaceName;
        this.typeRegistry = new TypeRegistry();
        this.exports = new ArrayList<>();
        this.imports = new ArrayList<>();
    }

    public String packageName() {
        return packageName;
    }

    public String interfaceName() {
        return interfaceName;
    }

    public TypeRegistry typeRegistry() {
        return typeRegistry;
    }

    public void addExport(FunctionSignature signature) {
        exports.add(signature);
    }

    public void addImport(FunctionSignature signature) {
        imports.add(signature);
    }

    public List<FunctionSignature> exports() {
        return Collections.unmodifiableList(exports);
    }

    public List<FunctionSignature> imports() {
        return Collections.unmodifiableList(imports);
    }

    public Optional<FunctionSignature> exportByName(String name) {
        return exports.stream().filter(f -> f.name().equals(name)).findFirst();
    }

    public Optional<FunctionSignature> importByName(String name) {
        return imports.stream().filter(f -> f.name().equals(name)).findFirst();
    }

    /**
     * Function signature definition.
     */
    public static class FunctionSignature {
        private final String name;
        private final List<Parameter> parameters;
        private final List<WitType> returns;

        public FunctionSignature(String name) {
            this.name = name;
            this.parameters = new ArrayList<>();
            this.returns = new ArrayList<>();
        }

        public String name() {
            return name;
        }

        public void addParameter(String paramName, WitType paramType) {
            parameters.add(new Parameter(paramName, paramType));
        }

        public void addReturnType(WitType returnType) {
            returns.add(returnType);
        }

        public List<Parameter> parameters() {
            return Collections.unmodifiableList(parameters);
        }

        public List<WitType> returns() {
            return Collections.unmodifiableList(returns);
        }

        public static class Parameter {
            public final String name;
            public final WitType type;

            public Parameter(String name, WitType type) {
                this.name = name;
                this.type = type;
            }
        }
    }
}
