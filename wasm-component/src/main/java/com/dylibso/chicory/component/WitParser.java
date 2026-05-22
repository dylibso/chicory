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
 * Supports both simple format: "functionname: function(...)"
 * And Component Model world syntax:
 * <pre>
 * package example:example;
 * world example {
 *   export functionname: func(a: s32, b: s32) -> s32;
 *   import hostfn: func(x: i32);
 * }
 * </pre>
 */
public class WitParser {
    // Pattern for both simple and world format functions
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
            Pattern.compile("([\\w-]+)\\s*:\\s*([\\w<>,]+)");
    private static final Pattern TYPE_PATTERN =
            Pattern.compile(
                    "^\\s*(type|record|variant|resource)\\s+(\\w+)(.*)$", Pattern.MULTILINE);

    /**
     * Parse WIT source code.
     * Detects format and handles both simple and Component Model world syntax.
     *
     * @param witText WIT source in text format
     * @return Parsed component definition
     * @throws IllegalArgumentException if WIT source is invalid
     */
    public ComponentDefinition parse(String witText) {
        // Extract package and world/interface names from WIT header
        String packageName = "default";
        String interfaceName = "component";

        // Try to extract from WIT header
        Pattern packagePattern = Pattern.compile("package\\s+([\\w:]+)\\s*;");
        Matcher pkgMatcher = packagePattern.matcher(witText);
        if (pkgMatcher.find()) {
            packageName = pkgMatcher.group(1);
        }

        // Try to extract world name
        Pattern worldPattern = Pattern.compile("world\\s+(\\w+)\\s*\\{");
        Matcher worldMatcher = worldPattern.matcher(witText);
        if (worldMatcher.find()) {
            interfaceName = worldMatcher.group(1);
        }

        ComponentDefinition definition = new ComponentDefinition(packageName, interfaceName);

        // Parse type definitions
        parseTypeDefinitions(witText, definition);

        // Detect format and parse function signatures
        boolean isWorldFormat = witText.contains("world") && witText.contains("{");
        if (isWorldFormat) {
            parseWorldFunctionSignatures(witText, definition);
        } else {
            parseSimpleFunctionSignatures(witText, definition);
        }

        return definition;
    }

    private void parseTypeDefinitions(String witText, ComponentDefinition definition) {
        Matcher typeMatcher = TYPE_PATTERN.matcher(witText);
        while (typeMatcher.find()) {
            String typeKeyword = typeMatcher.group(1);
            String typeName = typeMatcher.group(2);
            String typeBody = typeMatcher.group(3);

            switch (typeKeyword) {
                case "record":
                    parseRecord(typeName, typeBody, definition);
                    break;
                case "variant":
                    parseVariant(typeName, typeBody, definition);
                    break;
                case "resource":
                    definition.typeRegistry().register(typeName, new ResourceType(typeName));
                    break;
                case "type":
                    // Simple type alias
                    WitType aliasedType = parseType(typeBody.trim(), definition);
                    definition.typeRegistry().register(typeName, aliasedType);
                    break;
            }
        }
    }

    private void parseRecord(String recordName, String recordBody, ComponentDefinition definition) {
        RecordType record = new RecordType(recordName);
        // Simple field parsing: field-name: type
        Pattern fieldPattern = Pattern.compile("([\\w-]+)\\s*:\\s*([\\w<>,]+)");
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
        // Parse variant cases: case-name(optional-type) or case-name
        Pattern casePattern = Pattern.compile("(\\w+)(?:\\(([^)]+)\\))?");
        Matcher caseMatcher = casePattern.matcher(variantBody);
        while (caseMatcher.find()) {
            String caseName = caseMatcher.group(1);
            String caseTypeStr = caseMatcher.group(2);
            Optional<WitType> caseType =
                    caseTypeStr != null && !caseTypeStr.isEmpty()
                            ? Optional.of(parseType(caseTypeStr, definition))
                            : Optional.empty();
            variant.addCase(caseName, caseType);
        }
        definition.typeRegistry().register(variantName, variant);
    }

    /**
     * Parse function signatures from Component Model world syntax.
     * Supports export and import declarations within world { ... }.
     */
    private void parseWorldFunctionSignatures(String witText, ComponentDefinition definition) {
        // Parse exports
        Matcher exportMatcher = WORLD_EXPORT_PATTERN.matcher(witText);
        while (exportMatcher.find()) {
            String functionName = exportMatcher.group(1);
            String paramsStr = exportMatcher.group(2);
            String returnsStr = exportMatcher.group(3);
            parseAndAddFunctionSignature(definition, functionName, paramsStr, returnsStr, true);
        }

        // Parse imports
        Matcher importMatcher = WORLD_IMPORT_PATTERN.matcher(witText);
        while (importMatcher.find()) {
            String functionName = importMatcher.group(1);
            String paramsStr = importMatcher.group(2);
            String returnsStr = importMatcher.group(3);
            parseAndAddFunctionSignature(definition, functionName, paramsStr, returnsStr, false);
        }
    }

    /**
     * Parse function signatures from simple format.
     * Format: "functionname: function(params) -> returns"
     */
    private void parseSimpleFunctionSignatures(String witText, ComponentDefinition definition) {
        Matcher funcMatcher = SIMPLE_FUNCTION_PATTERN.matcher(witText);
        while (funcMatcher.find()) {
            String functionName = funcMatcher.group(1);
            String paramsStr = funcMatcher.group(2);
            String returnsStr = funcMatcher.group(3);
            parseAndAddFunctionSignature(definition, functionName, paramsStr, returnsStr, true);
        }
    }

    /**
     * Helper to parse and add a function signature to the definition.
     */
    private void parseAndAddFunctionSignature(
            ComponentDefinition definition,
            String functionName,
            String paramsStr,
            String returnsStr,
            boolean isExport) {
        ComponentDefinition.FunctionSignature sig =
                new ComponentDefinition.FunctionSignature(functionName);

        // Parse parameters
        if (paramsStr != null && !paramsStr.trim().isEmpty()) {
            Matcher paramMatcher = FUNCTION_PARAM_PATTERN.matcher(paramsStr);
            while (paramMatcher.find()) {
                String paramName = paramMatcher.group(1);
                String paramTypeStr = paramMatcher.group(2).trim();
                WitType paramType = parseType(paramTypeStr, definition);
                sig.addParameter(paramName, paramType);
            }
        }

        // Parse return types
        if (returnsStr != null && !returnsStr.trim().isEmpty()) {
            returnsStr = returnsStr.trim();
            String[] returnParts = returnsStr.split(",");
            for (String returnPart : returnParts) {
                String typeStr = returnPart.trim().replaceAll("[,;]\\s*$", "").trim();
                if (!typeStr.isEmpty()) {
                    WitType returnType = parseType(typeStr, definition);
                    sig.addReturnType(returnType);
                }
            }
        }

        if (isExport) {
            definition.addExport(sig);
        } else {
            definition.addImport(sig);
        }
    }

    private WitType parseType(String typeStr, ComponentDefinition definition) {
        typeStr = typeStr.trim().replaceAll("[,;]\\s*$", "").trim();

        // Check for primitive types (both i32 and s32 formats)
        try {
            return PrimitiveType.fromName(typeStr);
        } catch (IllegalArgumentException e) {
            // Try signed format (s32, s64, etc.)
            try {
                return PrimitiveType.fromName(convertSignedFormat(typeStr));
            } catch (IllegalArgumentException e2) {
                // Not a primitive, continue
            }
        }

        // Check for list type
        if (typeStr.startsWith("list<") && typeStr.endsWith(">")) {
            String elementTypeStr = typeStr.substring(5, typeStr.length() - 1);
            WitType elementType = parseType(elementTypeStr, definition);
            return new ListType(elementType);
        }

        // Check for inline record type
        if (typeStr.startsWith("record") && typeStr.contains("{")) {
            int openBrace = typeStr.indexOf('{');
            int closeBrace = typeStr.lastIndexOf('}');
            if (openBrace >= 0 && closeBrace > openBrace) {
                String fieldsStr = typeStr.substring(openBrace + 1, closeBrace);
                RecordType record = new RecordType("anonymous");
                parseRecordFields(fieldsStr, definition, record);
                return record;
            }
        }

        // Check for inline variant type
        if (typeStr.startsWith("variant") && typeStr.contains("{")) {
            int openBrace = typeStr.indexOf('{');
            int closeBrace = typeStr.lastIndexOf('}');
            if (openBrace >= 0 && closeBrace > openBrace) {
                String casesStr = typeStr.substring(openBrace + 1, closeBrace);
                VariantType variant = new VariantType("anonymous");
                parseVariantCases(casesStr, definition, variant);
                return variant;
            }
        }

        // Check for registered custom type
        final String finalTypeStr = typeStr;
        return definition
                .typeRegistry()
                .lookup(finalTypeStr)
                .orElseThrow(() -> new IllegalArgumentException("Unknown type: " + finalTypeStr));
    }

    /**
     * Convert WIT signed format (s32, s64) to Java format (i32, i64).
     * Component Model uses s32/s64/u32/u64, but we map to i32/i64/u32/u64.
     */
    private String convertSignedFormat(String typeStr) {
        return typeStr.replace("s32", "i32").replace("s64", "i64");
    }

    private void parseRecordFields(
            String fieldsStr, ComponentDefinition definition, RecordType record) {
        // Parse "x: i32, y: i32" into fields
        String[] fieldParts = fieldsStr.split(",");
        for (String fieldPart : fieldParts) {
            String field = fieldPart.trim();
            Matcher fieldMatcher = FUNCTION_PARAM_PATTERN.matcher(field);
            if (fieldMatcher.find()) {
                String fieldName = fieldMatcher.group(1);
                String fieldType = fieldMatcher.group(2);
                WitType type = parseType(fieldType, definition);
                record.addField(fieldName, type);
            }
        }
    }

    private void parseVariantCases(
            String casesStr, ComponentDefinition definition, VariantType variant) {
        // Parse variant cases (simplified: no payloads for now)
        String[] caseParts = casesStr.split(",");
        for (String casePart : caseParts) {
            String caseName = casePart.trim();
            variant.addCase(caseName, java.util.Optional.empty());
        }
    }
}
