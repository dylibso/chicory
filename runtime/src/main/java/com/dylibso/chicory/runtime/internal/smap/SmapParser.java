package com.dylibso.chicory.runtime.internal.smap;

import static com.dylibso.chicory.runtime.internal.smap.Smap.FUNCTIONS_VENDOR_ID;

import com.dylibso.chicory.runtime.ParserException;
import com.dylibso.chicory.runtime.Stratum;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for SMAP (Source Map) strings according to JSR-045 specification.
 * Converts SMAP format into Smap objects.
 */
public final class SmapParser {

    private SmapParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses an SMAP string and returns a Smap object.
     *
     * @param smapString the SMAP string to parse
     * @return a Smap object representing the parsed SMAP
     * @throws ParserException if the SMAP string is invalid
     */
    public static Smap parse(String smapString) throws ParserException {
        if (smapString == null || smapString.trim().isEmpty()) {
            throw new ParserException("SMAP string cannot be null or empty");
        }

        String[] lines = smapString.split("\\r?\\n");
        int lineIndex = 0;

        Smap generator = new Smap();

        // Parse header
        lineIndex = parseHeader(lines, lineIndex, generator);

        // Parse embedded SMAPs and sections
        parseSections(lines, lineIndex, generator);

        return generator;
    }

    private static int parseHeader(String[] lines, int lineIndex, Smap generator)
            throws ParserException {
        // First line should be "SMAP"
        if (lineIndex >= lines.length || !lines[lineIndex].trim().equals("SMAP")) {
            throw new ParserException("Expected 'SMAP' at line " + (lineIndex + 1));
        }
        lineIndex++;

        // Second line is output filename
        if (lineIndex >= lines.length) {
            throw new ParserException("Expected output filename at line " + (lineIndex + 1));
        }
        generator.withOutputFileName(lines[lineIndex].trim());
        lineIndex++;

        // Third line is default stratum
        if (lineIndex >= lines.length) {
            throw new ParserException("Expected default stratum at line " + (lineIndex + 1));
        }
        // Note: default stratum is set when we add the first stratum with defaultStratum=true
        // We read but don't need to store the default stratum from the header
        lineIndex++;

        return lineIndex;
    }

    private static void parseSections(String[] lines, int lineIndex, Smap generator)
            throws ParserException {
        while (lineIndex < lines.length) {
            String line = lines[lineIndex].trim();

            if (line.equals("*E")) {
                // End section
                break;
            } else if (line.startsWith("*O ")) {
                // Embedded SMAP open section
                lineIndex = parseEmbeddedSmap(lines, lineIndex, generator);
            } else if (line.startsWith("*S ")) {
                // Stratum.Builder section
                lineIndex = parseStratumSection(lines, lineIndex, generator);
            } else if (line.startsWith("*V")) {
                // Vendor section - skip for now
                lineIndex = parseVendorSection(lines, lineIndex, generator);
            } else if (line.startsWith("*")) {
                // Unknown section - skip
                lineIndex = skipUnknownSection(lines, lineIndex);
            } else {
                lineIndex++;
            }
        }
    }

    private static int parseEmbeddedSmap(String[] lines, int lineIndex, Smap generator)
            throws ParserException {
        String openLine = lines[lineIndex].trim();
        if (!openLine.startsWith("*O ")) {
            throw new ParserException(
                    "Expected embedded SMAP open section at line " + (lineIndex + 1));
        }

        String stratumName = openLine.substring(3).trim();
        lineIndex++;

        // Find the embedded SMAP content
        StringBuilder embeddedSmapBuilder = new StringBuilder();
        boolean foundClosing = false;

        while (lineIndex < lines.length) {
            String line = lines[lineIndex];

            if (line.trim().equals("*C " + stratumName)) {
                foundClosing = true;
                lineIndex++;
                break;
            }

            embeddedSmapBuilder.append(line).append("\n");
            lineIndex++;
        }

        if (!foundClosing) {
            throw new ParserException("Missing closing section *C " + stratumName);
        }

        generator.addSmap(embeddedSmapBuilder.toString(), stratumName);
        return lineIndex;
    }

    private static int parseStratumSection(String[] lines, int lineIndex, Smap generator)
            throws ParserException {
        String stratumLine = lines[lineIndex].trim();
        if (!stratumLine.startsWith("*S ")) {
            throw new ParserException("Expected stratum section at line " + (lineIndex + 1));
        }

        String stratumName = stratumLine.substring(3).trim();
        Stratum.Builder stratum = Stratum.builder(stratumName);
        lineIndex++;

        // Parse file and line sections for this stratum
        Map<Integer, String> fileIdToPath = new HashMap<>();
        Map<Integer, String> fileIdToName = new HashMap<>();
        boolean hasFileSection = false;
        boolean hasLineSection = false;

        while (lineIndex < lines.length) {
            String line = lines[lineIndex].trim();

            if (line.equals("*F")) {
                // File section
                lineIndex++;
                lineIndex = parseFileSection(lines, lineIndex, stratum, fileIdToPath, fileIdToName);
                hasFileSection = true;
            } else if (line.equals("*L")) {
                // Line section
                lineIndex++;
                lineIndex = parseLineSection(lines, lineIndex, stratum, fileIdToPath, fileIdToName);
                hasLineSection = true;
            } else if (line.startsWith("*")) {
                // End of this stratum section
                break;
            } else {
                lineIndex++;
            }
        }

        if (hasFileSection || hasLineSection) {
            // Determine if this should be the default stratum (first stratum added)
            boolean isDefault = stratumName.equals("JSP") || stratumName.equals("WASM");
            generator.withStratum(stratum, isDefault);
        }

        return lineIndex;
    }

    private static int parseFileSection(
            String[] lines,
            int lineIndex,
            Stratum.Builder stratum,
            Map<Integer, String> fileIdToPath,
            Map<Integer, String> fileIdToName)
            throws ParserException {
        while (lineIndex < lines.length) {
            String line = lines[lineIndex].trim();

            if (line.startsWith("*")) {
                // End of file section
                break;
            }

            if (line.isEmpty()) {
                lineIndex++;
                continue;
            }

            if (line.startsWith("+ ")) {
                // File with path: + fileId fileName
                String[] parts = line.substring(2).split(" ", 2);
                if (parts.length < 2) {
                    throw new ParserException(
                            "Invalid file entry with path at line " + (lineIndex + 1));
                }

                int fileId = Integer.parseInt(parts[0]);
                String fileName = parts[1];
                lineIndex++;

                // Next line should be the path
                if (lineIndex >= lines.length) {
                    throw new ParserException("Expected file path at line " + (lineIndex + 1));
                }
                String filePath = lines[lineIndex].trim();

                fileIdToName.put(fileId, fileName);
                fileIdToPath.put(fileId, filePath);
                stratum.addFile(fileName, filePath);

            } else {
                // File without path: fileId fileName
                String[] parts = line.split(" ", 2);
                if (parts.length < 2) {
                    throw new ParserException("Invalid file entry at line " + (lineIndex + 1));
                }

                int fileId = Integer.parseInt(parts[0]);
                String fileName = parts[1];

                fileIdToName.put(fileId, fileName);
                fileIdToPath.put(fileId, fileName); // Use filename as path
                stratum.addFile(fileName, fileName);
            }

            lineIndex++;
        }

        return lineIndex;
    }

    private static int parseLineSection(
            String[] lines,
            int lineIndex,
            Stratum.Builder stratum,
            Map<Integer, String> fileIdToPath,
            Map<Integer, String> fileIdToName)
            throws ParserException {
        int lastFileId = 0;

        while (lineIndex < lines.length) {
            String line = lines[lineIndex].trim();

            if (line.startsWith("*")) {
                // End of line section
                break;
            }

            if (line.isEmpty()) {
                lineIndex++;
                continue;
            }

            // Parse line info:
            // InputStartLine[#FileId][,RepeatCount]:OutputStartLine[,OutputLineIncrement]
            int colonIndex = line.indexOf(':');
            if (colonIndex == -1) {
                throw new ParserException(
                        "Invalid line info format at line " + (lineIndex + 1) + ": " + line);
            }

            String inputPart = line.substring(0, colonIndex);
            String outputPart = line.substring(colonIndex + 1);

            // Parse input part
            long inputStartLine;
            int fileId =
                    lastFileId; // Will be overridden if specified, defaults to 0 for first file
            int inputLineCount = 1;

            int hashIndex = inputPart.indexOf('#');
            if (hashIndex != -1) {
                inputStartLine = Long.parseLong(inputPart.substring(0, hashIndex));
                String fileAndCount = inputPart.substring(hashIndex + 1);

                int commaIndex = fileAndCount.indexOf(',');
                if (commaIndex != -1) {
                    fileId = Integer.parseInt(fileAndCount.substring(0, commaIndex));
                    inputLineCount = Integer.parseInt(fileAndCount.substring(commaIndex + 1));
                } else {
                    fileId = Integer.parseInt(fileAndCount);
                }
            } else {
                int commaIndex = inputPart.indexOf(',');
                if (commaIndex != -1) {
                    inputStartLine = Long.parseLong(inputPart.substring(0, commaIndex));
                    inputLineCount = Integer.parseInt(inputPart.substring(commaIndex + 1));
                } else {
                    inputStartLine = Long.parseLong(inputPart);
                }
            }

            // Parse output part
            long outputStartLine;
            int outputLineCount = 1;

            int commaIndex = outputPart.indexOf(',');
            if (commaIndex != -1) {
                outputStartLine = Long.parseLong(outputPart.substring(0, commaIndex));
                outputLineCount = Integer.parseInt(outputPart.substring(commaIndex + 1));
            } else {
                outputStartLine = Long.parseLong(outputPart);
            }

            // Add line data to stratum
            String fileName = fileIdToName.get(fileId);
            String filePath = fileIdToPath.get(fileId);

            if (fileName == null) {
                throw new ParserException(
                        "Unknown file ID " + fileId + " at line " + (lineIndex + 1));
            }

            stratum.withLineMapping(
                    fileName,
                    filePath,
                    inputStartLine,
                    inputLineCount,
                    outputStartLine,
                    outputLineCount);
            lastFileId = fileId;

            lineIndex++;
        }

        return lineIndex;
    }

    private static int parseVendorSection(String[] lines, int lineIndex, Smap generator) {
        lineIndex++; // Skip *V line

        if (lineIndex >= lines.length) {
            return lineIndex; // No more lines to process
        }
        String vendor = lines[lineIndex].trim();
        if (vendor.equals(FUNCTIONS_VENDOR_ID)) {
            lineIndex++;
            while (lineIndex < lines.length) {
                String line = lines[lineIndex].trim();
                // parse the line. it should be in %d,%d=%s format
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String[] keys = parts[0].split(",", 2);
                    if (keys.length == 2) {
                        try {
                            long start = Long.parseLong(keys[0]);
                            long end = Long.parseLong(keys[1]);
                            String functionName = parts[1];
                            generator
                                    .getDefaultStratum()
                                    .withFunctionMapping(functionName, start, end);

                        } catch (NumberFormatException e) {
                            continue;
                        }
                    }
                }

                lineIndex++;
            }
        } else {
            lineIndex = skipTillSectionEnd(lines, lineIndex);
        }
        return lineIndex;
    }

    private static int skipUnknownSection(String[] lines, int lineIndex) {
        lineIndex++; // Skip section marker line

        return skipTillSectionEnd(lines, lineIndex);
    }

    private static int skipTillSectionEnd(String[] lines, int lineIndex) {
        while (lineIndex < lines.length) {
            String line = lines[lineIndex].trim();
            if (line.startsWith("*")) {
                break;
            }
            lineIndex++;
        }
        return lineIndex;
    }
}
