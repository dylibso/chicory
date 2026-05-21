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
 */
public class WitParser {
    private static final Pattern FUNCTION_PATTERN =
            Pattern.compile(
                    "(\\w+)\\s*:\\s*function\\s*\\(([^)]*)\\)\\s*(?:->\\s*([^\\n]+?))?(?=\\n|$)",
                    Pattern.MULTILINE);
    private static final Pattern FUNCTION_PARAM_PATTERN =
            Pattern.compile("(\\w+)\\s*:\\s*([\\w<>,]+)");
    private static final Pattern TYPE_PATTERN =
            Pattern.compile(
                    "^\\s*(type|record|variant|resource)\\s+(\\w+)(.*)$", Pattern.MULTILINE);

    /**
     * Parse WIT source code.
     *
     * @param witText WIT source in text format
     * @return Parsed component definition
     * @throws IllegalArgumentException if WIT source is invalid
     */
    public ComponentDefinition parse(String witText) {
        // Extract package and interface names from WIT namespace
        String packageName = "default";
        String interfaceName = "component";

        // Try to extract from WIT header
        Pattern namespacePattern = Pattern.compile("package\\s+([\\w:]+)?(?:\\s+as\\s+(\\w+))?");
        Matcher nsMatcher = namespacePattern.matcher(witText);
        if (nsMatcher.find()) {
            packageName = nsMatcher.group(1) != null ? nsMatcher.group(1) : "default";
            interfaceName = nsMatcher.group(2) != null ? nsMatcher.group(2) : interfaceName;
        }

        ComponentDefinition definition = new ComponentDefinition(packageName, interfaceName);

        // Parse type definitions
        parseTypeDefinitions(witText, definition);

        // Parse function signatures
        parseFunctionSignatures(witText, definition);

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
        Pattern fieldPattern = Pattern.compile("(\\w+)\\s*:\\s*([\\w<>,]+)");
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

    private void parseFunctionSignatures(String witText, ComponentDefinition definition) {
        Matcher funcMatcher = FUNCTION_PATTERN.matcher(witText);
        while (funcMatcher.find()) {
            String functionName = funcMatcher.group(1);
            String paramsStr = funcMatcher.group(2);
            String returnsStr = funcMatcher.group(3);

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

            definition.addExport(sig);
        }
    }

    private WitType parseType(String typeStr, ComponentDefinition definition) {
        typeStr = typeStr.trim().replaceAll("[,;]\\s*$", "").trim();

        // Check for primitive types
        try {
            return PrimitiveType.fromName(typeStr);
        } catch (IllegalArgumentException e) {
            // Not a primitive, continue
        }

        // Check for list type
        if (typeStr.startsWith("list<") && typeStr.endsWith(">")) {
            String elementTypeStr = typeStr.substring(5, typeStr.length() - 1);
            WitType elementType = parseType(elementTypeStr, definition);
            return new ListType(elementType);
        }

        // Check for registered custom type
        final String finalTypeStr = typeStr;
        return definition
                .typeRegistry()
                .lookup(finalTypeStr)
                .orElseThrow(() -> new IllegalArgumentException("Unknown type: " + finalTypeStr));
    }
}
