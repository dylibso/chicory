/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dylibso.chicory.compiler.internal.smap;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for SMAP (Source Map) strings according to JSR-045 specification.
 * Can reconstruct SmapGenerator and SmapStratum objects from SMAP text.
 */
public final class SmapParser {

    private SmapParser() {}

    /**
     * Helper class to track file ID to filename mappings during parsing.
     */
    private static final class FileMapping {
        private final Map<Integer, String> idToFileName = new HashMap<>();
        private final Map<Integer, String> idToFilePath = new HashMap<>();

        void addFile(int fileId, String fileName, String filePath) {
            idToFileName.put(fileId, fileName);
            idToFilePath.put(fileId, filePath);
        }

        String getFileName(int fileId) {
            return idToFileName.get(fileId);
        }

        String getFilePath(int fileId) {
            return idToFilePath.get(fileId);
        }
    }

    /**
     * Helper class to return both the next line index and the file mapping from file section parsing.
     */
    private static class ParseResult {
        final int nextIndex;
        final FileMapping fileMapping;

        ParseResult(int nextIndex, FileMapping fileMapping) {
            this.nextIndex = nextIndex;
            this.fileMapping = fileMapping;
        }
    }

    /**
     * Helper class to track the last file ID used during line parsing to avoid outputting
     * unnecessary file ID markers.
     */
    private static final class LineParsingState {
        int lastFileId;
    }

    /**
     * Parses an SMAP string and returns a reconstructed SmapGenerator object.
     *
     * @param smapContent the SMAP string to parse
     * @return a SmapGenerator object reconstructed from the SMAP content
     * @throws IllegalArgumentException if the SMAP format is invalid
     */
    public static SmapGenerator parse(String smapContent) {
        if (smapContent == null || smapContent.trim().isEmpty()) {
            throw new IllegalArgumentException("SMAP content cannot be null or empty");
        }

        String[] lines = smapContent.split("\n");
        if (lines.length < 4) {
            throw new IllegalArgumentException("Invalid SMAP format: too few lines");
        }

        int lineIndex = 0;

        // Parse header
        if (!"SMAP".equals(lines[lineIndex++])) {
            throw new IllegalArgumentException("SMAP must start with 'SMAP'");
        }

        String outputFileName = lines[lineIndex++];
        String defaultStratum = lines[lineIndex++];

        SmapGenerator generator = new SmapGenerator();
        generator.setOutputFileName(outputFileName);

        // Parse sections
        while (lineIndex < lines.length) {
            String line = lines[lineIndex];

            if (line.equals("*E")) {
                // End of SMAP
                break;
            } else if (line.startsWith("*O ")) {
                // Embedded SMAP
                lineIndex = parseEmbeddedSmap(lines, lineIndex, generator);
            } else if (line.startsWith("*S ")) {
                // Stratum section
                lineIndex = parseStratum(lines, lineIndex, generator, defaultStratum);
            } else {
                lineIndex++;
            }
        }

        return generator;
    }

    private static int parseEmbeddedSmap(String[] lines, int startIndex, SmapGenerator generator) {
        String stratumName = lines[startIndex].substring(3); // Remove "*O "
        int lineIndex = startIndex + 1;
        StringBuilder embeddedContent = new StringBuilder();

        while (lineIndex < lines.length) {
            String line = lines[lineIndex];
            if (line.equals("*C " + stratumName)) {
                break;
            }
            embeddedContent.append(line).append("\n");
            lineIndex++;
        }

        generator.addSmap(embeddedContent.toString(), stratumName);
        return lineIndex + 1;
    }

    private static int parseStratum(
            String[] lines, int startIndex, SmapGenerator generator, String defaultStratumName) {
        String stratumName = lines[startIndex].substring(3); // Remove "*S "
        SmapStratum stratum = new SmapStratum(stratumName);
        int lineIndex = startIndex + 1;
        FileMapping fileMapping = new FileMapping();

        // Parse file section
        if (lineIndex < lines.length && lines[lineIndex].equals("*F")) {
            ParseResult fileResult = parseFileSection(lines, lineIndex + 1, stratum);
            lineIndex = fileResult.nextIndex;
            fileMapping = fileResult.fileMapping;
        }

        // Parse line section
        if (lineIndex < lines.length && lines[lineIndex].equals("*L")) {
            LineParsingState lineState = new LineParsingState();
            lineIndex = parseLineSection(lines, lineIndex + 1, stratum, fileMapping, lineState);
        }

        boolean isDefault = stratumName.equals(defaultStratumName);
        generator.addStratum(stratum, isDefault);

        return lineIndex;
    }

    private static ParseResult parseFileSection(
            String[] lines, int startIndex, SmapStratum stratum) {
        int lineIndex = startIndex;
        FileMapping fileMapping = new FileMapping();

        while (lineIndex < lines.length) {
            String line = lines[lineIndex];

            if (line.startsWith("*")) {
                // Next section
                break;
            }

            if (line.startsWith("+ ")) {
                // File with path: "+ fileId fileName"
                String[] parts = line.substring(2).split(" ", 2);
                if (parts.length >= 2) {
                    int fileId = Integer.parseInt(parts[0]);
                    String fileName = parts[1];

                    // Next line should be the file path
                    lineIndex++;
                    if (lineIndex < lines.length) {
                        String filePath = lines[lineIndex];
                        // Add leading "/" if not present (since it was removed during generation)
                        if (!filePath.startsWith("/")) {
                            filePath = "/" + filePath;
                        }
                        fileMapping.addFile(fileId, fileName, filePath);
                        stratum.addFile(fileName, filePath);
                    }
                }
            } else {
                // File without path: "fileId fileName"
                String[] parts = line.split(" ", 2);
                if (parts.length >= 2) {
                    int fileId = Integer.parseInt(parts[0]);
                    String fileName = parts[1];
                    fileMapping.addFile(fileId, fileName, fileName);
                    stratum.addFile(fileName);
                }
            }

            lineIndex++;
        }

        return new ParseResult(lineIndex, fileMapping);
    }

    private static int parseLineSection(
            String[] lines,
            int startIndex,
            SmapStratum stratum,
            FileMapping fileMapping,
            LineParsingState lineState) {
        int lineIndex = startIndex;

        while (lineIndex < lines.length) {
            String line = lines[lineIndex];

            if (line.startsWith("*") || line.trim().isEmpty()) {
                // Next section or end
                break;
            }

            parseLineInfo(line, stratum, fileMapping, lineState);
            lineIndex++;
        }

        return lineIndex;
    }

    private static void parseLineInfo(
            String lineInfo,
            SmapStratum stratum,
            FileMapping fileMapping,
            LineParsingState lineState) {
        // Format: inputStartLine[#fileId][,inputLineCount]:outputStartLine[,outputLineIncrement]
        int colonIndex = lineInfo.indexOf(':');
        if (colonIndex == -1) {
            return; // Skip invalid line
        }

        String inputPart = lineInfo.substring(0, colonIndex);
        String outputPart = lineInfo.substring(colonIndex + 1);

        // Parse input part
        int inputStartLine;
        int fileId = lineState.lastFileId; // Use last file ID if not specified
        int inputLineCount = 1; // Default line count

        // Check for file ID
        int hashIndex = inputPart.indexOf('#');
        if (hashIndex != -1) {
            inputStartLine = Integer.parseInt(inputPart.substring(0, hashIndex));
            String fileAndCount = inputPart.substring(hashIndex + 1);

            int commaIndex = fileAndCount.indexOf(',');
            if (commaIndex != -1) {
                fileId = Integer.parseInt(fileAndCount.substring(0, commaIndex));
                inputLineCount = Integer.parseInt(fileAndCount.substring(commaIndex + 1));
            } else {
                fileId = Integer.parseInt(fileAndCount);
            }
            // Update last file ID when explicitly specified
            lineState.lastFileId = fileId;
        } else {
            int commaIndex = inputPart.indexOf(',');
            if (commaIndex != -1) {
                inputStartLine = Integer.parseInt(inputPart.substring(0, commaIndex));
                inputLineCount = Integer.parseInt(inputPart.substring(commaIndex + 1));
            } else {
                inputStartLine = Integer.parseInt(inputPart);
            }
        }

        // Parse output part
        int outputStartLine;
        int outputLineIncrement = 1; // Default increment

        int commaIndex = outputPart.indexOf(',');
        if (commaIndex != -1) {
            outputStartLine = Integer.parseInt(outputPart.substring(0, commaIndex));
            outputLineIncrement = Integer.parseInt(outputPart.substring(commaIndex + 1));
        } else {
            outputStartLine = Integer.parseInt(outputPart);
        }

        // Use the file mapping to get the actual filename/path
        String filePath = fileMapping.getFilePath(fileId);
        if (filePath == null) {
            // Fallback if file ID not found - this shouldn't happen in well-formed SMAPs
            filePath = "file" + fileId;
            stratum.addFile(filePath);
        }

        stratum.addLineData(
                inputStartLine, filePath, inputLineCount, outputStartLine, outputLineIncrement);
    }
}
