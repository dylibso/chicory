package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.ListType;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.component.types.ResourceType;
import com.dylibso.chicory.component.types.VariantType;
import com.dylibso.chicory.component.types.WitType;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses WIT (WebAssembly Interface Types) definitions from text format.
 * Supports Component Model world syntax with records, variants, lists.
 */
public class WitParser {
    private static final Pattern WORLD_EXPORT_PATTERN =
            Pattern.compile(
                    "export\\s+([\\w-]+)\\s*:\\s*func\\s*\\(([^)]*)\\)\\s*(?:->\\s*([^;\\n"
                            + "]+?))?\\s*;",
                    Pattern.MULTILINE);
    private static final Pattern WORLD_IMPORT_PATTERN =
            Pattern.compile(
                    "import\\s+([\\w-]+)\\s*:\\s*func\\s*\\(([^)]*)\\)\\s*(?:->\\s*([^;\\n"
                            + "]+?))?\\s*;",
                    Pattern.MULTILINE);
    private static final Pattern SIMPLE_FUNCTION_PATTERN =
            Pattern.compile(
                    "([\\w-]+)\\s*:\\s*function\\s*\\(([^)]*)\\)\\s*(?:->\\s*([^\\n]+?))?(?=\\n|$)",
                    Pattern.MULTILINE);
    private static final Pattern FUNCTION_PARAM_PATTERN =
            Pattern.compile("([\\w-]+)\\s*:\\s*([\\w<>-]+)");

    public ComponentDefinition parse(String witText) {
        String packageName = "default";
        String interfaceName = "component";

        Pattern packagePattern = Pattern.compile("package\\s+([\\w:]+)\\s*;");
        Matcher pkgMatcher = packagePattern.matcher(witText);
        if (pkgMatcher.find()) {
            packageName = pkgMatcher.group(1);
        }

        Pattern worldPattern = Pattern.compile("world\\s+(\\w+)\\s*\\{");
        Matcher worldMatcher = worldPattern.matcher(witText);
        if (worldMatcher.find()) {
            interfaceName = worldMatcher.group(1);
        }

        ComponentDefinition definition = new ComponentDefinition(packageName, interfaceName);

        parseTypeDefinitions(witText, definition);

        boolean isWorldFormat = witText.contains("world") && witText.contains("{");
        if (isWorldFormat) {
            parseWorldFunctionSignatures(witText, definition);
        } else {
            parseSimpleFunctionSignatures(witText, definition);
        }

        return definition;
    }

    private void parseTypeDefinitions(String witText, ComponentDefinition definition) {
        // Parse records with multi-line support (allow hyphens in names)
        Pattern recordPattern =
                Pattern.compile(
                        "record\\s+([-\\w]+)\\s*\\{([^}]*)\\}", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher recordMatcher = recordPattern.matcher(witText);
        while (recordMatcher.find()) {
            String recordName = recordMatcher.group(1);
            String recordBody = recordMatcher.group(2);
            parseRecord(recordName, recordBody, definition);
        }

        // Parse variants (allow hyphens in names)
        Pattern variantPattern =
                Pattern.compile(
                        "variant\\s+([-\\w]+)\\s*\\{([^}]*)\\}",
                        Pattern.MULTILINE | Pattern.DOTALL);
        Matcher variantMatcher = variantPattern.matcher(witText);
        while (variantMatcher.find()) {
            String variantName = variantMatcher.group(1);
            String variantBody = variantMatcher.group(2);
            parseVariant(variantName, variantBody, definition);
        }

        // Parse resources
        Pattern resourcePattern = Pattern.compile("resource\\s+([-\\w]+)");
        Matcher resourceMatcher = resourcePattern.matcher(witText);
        while (resourceMatcher.find()) {
            String resourceName = resourceMatcher.group(1);
            definition.typeRegistry().register(resourceName, new ResourceType(resourceName));
        }

        // Parse type aliases
        Pattern typePattern = Pattern.compile("type\\s+([-\\w]+)\\s*=\\s*([\\w<>,]+);");
        Matcher typeMatcher = typePattern.matcher(witText);
        while (typeMatcher.find()) {
            String typeName = typeMatcher.group(1);
            String typeStr = typeMatcher.group(2);
            WitType aliasedType = parseType(typeStr, definition);
            definition.typeRegistry().register(typeName, aliasedType);
        }
    }

    private void parseRecord(String recordName, String recordBody, ComponentDefinition definition) {
        RecordType record = new RecordType(recordName);
        Pattern fieldPattern = Pattern.compile("([\\w-]+)\\s*:\\s*([\\w<>]+?)(?=[,}])");
        Matcher fieldMatcher = fieldPattern.matcher(recordBody);
        while (fieldMatcher.find()) {
            String fieldName = fieldMatcher.group(1);
            String fieldTypeStr = fieldMatcher.group(2);
            WitType fieldType = parseType(fieldTypeStr, definition);
            record.addField(fieldName, fieldType);
        }
        definition.typeRegistry().register(recordName, record);
    }

    private void parseVariant(
            String variantName, String variantBody, ComponentDefinition definition) {
        VariantType variant = new VariantType(variantName);
        Pattern casePattern = Pattern.compile("([\\w-]+)(?:\\(([^)]*)\\))?");
        Matcher caseMatcher = casePattern.matcher(variantBody);
        while (caseMatcher.find()) {
            String caseName = caseMatcher.group(1);
            String caseTypeStr = caseMatcher.group(2);
            if (caseTypeStr != null && !caseTypeStr.trim().isEmpty()) {
                WitType caseType = parseType(caseTypeStr, definition);
                variant.addCase(caseName, Optional.of(caseType));
            } else {
                variant.addCase(caseName, Optional.empty());
            }
        }
        definition.typeRegistry().register(variantName, variant);
    }

    private void parseWorldFunctionSignatures(String witText, ComponentDefinition definition) {
        Matcher exportMatcher = WORLD_EXPORT_PATTERN.matcher(witText);
        while (exportMatcher.find()) {
            String funcName = exportMatcher.group(1);
            String paramsStr = exportMatcher.group(2);
            String returnStr = exportMatcher.group(3);

            ComponentDefinition.FunctionSignature sig =
                    parseFunctionSignature(funcName, paramsStr, returnStr, definition);
            definition.addExport(sig);
        }

        Matcher importMatcher = WORLD_IMPORT_PATTERN.matcher(witText);
        while (importMatcher.find()) {
            String funcName = importMatcher.group(1);
            String paramsStr = importMatcher.group(2);
            String returnStr = importMatcher.group(3);

            ComponentDefinition.FunctionSignature sig =
                    parseFunctionSignature(funcName, paramsStr, returnStr, definition);
            definition.addImport(sig);
        }
    }

    private void parseSimpleFunctionSignatures(String witText, ComponentDefinition definition) {
        Matcher matcher = SIMPLE_FUNCTION_PATTERN.matcher(witText);
        while (matcher.find()) {
            String funcName = matcher.group(1);
            String paramsStr = matcher.group(2);
            String returnStr = matcher.group(3);

            ComponentDefinition.FunctionSignature sig =
                    parseFunctionSignature(funcName, paramsStr, returnStr, definition);
            definition.addExport(sig);
        }
    }

    private ComponentDefinition.FunctionSignature parseFunctionSignature(
            String funcName, String paramsStr, String returnStr, ComponentDefinition definition) {
        ComponentDefinition.FunctionSignature sig =
                new ComponentDefinition.FunctionSignature(funcName);

        if (paramsStr != null && !paramsStr.trim().isEmpty()) {
            Matcher paramMatcher = FUNCTION_PARAM_PATTERN.matcher(paramsStr);
            while (paramMatcher.find()) {
                String paramName = paramMatcher.group(1);
                String paramTypeStr = paramMatcher.group(2);
                paramTypeStr =
                        paramTypeStr.replaceAll("[,\\s]+$", ""); // Remove trailing commas/spaces
                WitType paramType = parseType(paramTypeStr, definition);
                sig.addParameter(paramName, paramType);
            }
        }

        if (returnStr != null && !returnStr.trim().isEmpty()) {
            WitType returnType = parseType(returnStr.trim(), definition);
            sig.addReturnType(returnType);
        }

        return sig;
    }

    public WitType parseType(String typeStr, ComponentDefinition definition) {
        typeStr = typeStr.trim();

        // Normalize WIT type names to primitive types (s32 -> i32, etc)
        typeStr = normalizeType(typeStr);

        try {
            return PrimitiveType.fromName(typeStr);
        } catch (IllegalArgumentException e) {
            // Not a primitive
        }

        if (typeStr.startsWith("list<") && typeStr.endsWith(">")) {
            String elementTypeStr = typeStr.substring(5, typeStr.length() - 1);
            WitType elementType = parseType(elementTypeStr, definition);
            return new ListType(elementType);
        }

        Optional<WitType> registered = definition.typeRegistry().lookup(typeStr);
        if (registered.isPresent()) {
            return registered.get();
        }

        throw new IllegalArgumentException("Unknown type: " + typeStr);
    }

    private String normalizeType(String typeStr) {
        switch (typeStr) {
            case "s8":
                return "i32"; // signed 8-bit
            case "u8":
                return "i32"; // unsigned 8-bit
            case "s16":
                return "i32"; // signed 16-bit
            case "u16":
                return "i32"; // unsigned 16-bit
            case "s32":
                return "i32"; // signed 32-bit
            case "u32":
                return "i32"; // unsigned 32-bit
            case "s64":
                return "i64"; // signed 64-bit
            case "u64":
                return "i64"; // unsigned 64-bit
            default:
                return typeStr;
        }
    }
}
