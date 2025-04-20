// Original came from:
// https://github.com/apache/sling-org-apache-sling-scripting-jsp/blob/4e0f12aab9c42a1475587800cefe6a39721020ec/src/main/java/org/apache/sling/scripting/jsp/jasper/compiler/SmapStratum.java
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

package com.dylibso.chicory.runtime.internal.smap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Represents the line and file mappings associated with a JSR-045 "stratum".
 */
public class SmapStratum implements com.dylibso.chicory.runtime.Stratum {

    public static final String FUNCTIONS_VENDOR_ID = "com.dylibso.chicory.functions";

    /**
     * Long implementation of LineInfo for very large files
     */
    private static final class LongLineInfo extends LineMapping {
        private long inputStartLine = -1;
        private long outputStartLine = -1;

        @Override
        public long inputStartLine() {
            return inputStartLine;
        }

        @Override
        public long outputStartLine() {
            return outputStartLine;
        }

        @Override
        LineMapping withInputStartLine(long inputStartLine) {
            if (inputStartLine < 0) {
                throw new IllegalArgumentException("" + inputStartLine);
            }
            this.inputStartLine = inputStartLine;
            return this;
        }

        @Override
        LineMapping withOutputStartLine(long outputStartLine) {
            if (outputStartLine < 0) {
                throw new IllegalArgumentException("" + outputStartLine);
            }
            this.outputStartLine = outputStartLine;
            return this;
        }
    }

    private static class SwitchToLong extends RuntimeException {
        private final LineMapping lineInfo;

        public SwitchToLong(LineMapping info) {
            this.lineInfo = info;
        }

        public LineMapping getLineInfo() {
            return lineInfo;
        }
    }

    /**
     * Int implementation of LineInfo for small files
     */
    private static final class IntLineInfo extends LineMapping {
        private int inputStartLine = -1;
        private int outputStartLine = -1;

        @Override
        public long inputStartLine() {
            return inputStartLine;
        }

        @Override
        public long outputStartLine() {
            return outputStartLine;
        }

        public LongLineInfo toLongLineInfo() {
            LongLineInfo longLineInfo = new LongLineInfo();
            longLineInfo.inputStartLine = inputStartLine;
            longLineInfo.outputStartLine = outputStartLine;
            longLineInfo.inputLineCount = inputLineCount;
            longLineInfo.outputLineCount = outputLineCount;
            longLineInfo.lineFileID = lineFileID;
            return longLineInfo;
        }

        @Override
        LineMapping withInputStartLine(long value) {
            if (value > Integer.MAX_VALUE) {
                throw new SwitchToLong(toLongLineInfo().withInputStartLine(value));
            }
            if (value < 0) {
                throw new IllegalArgumentException("" + value);
            }
            this.inputStartLine = (int) value;
            return this;
        }

        @Override
        LineMapping withOutputStartLine(long value) {
            if (value > Integer.MAX_VALUE) {
                throw new SwitchToLong(toLongLineInfo().withOutputStartLine(value));
            }
            if (value < 0) {
                throw new IllegalArgumentException("" + value);
            }
            this.outputStartLine = (int) value;
            return this;
        }
    }

    public static final class FunctionMapping {
        private final String functionName;
        private final long startLine;
        private final long endLine;

        public FunctionMapping(String functionName, long startLine, long endLine) {
            this.functionName = functionName;
            this.startLine = startLine;
            this.endLine = endLine;
        }

        public String getFunctionName() {
            return functionName;
        }

        public long getStartLine() {
            return startLine;
        }

        public long getEndLine() {
            return endLine;
        }
    }

    private final String stratumName;
    private final List<String> fileNameList = new ArrayList<>();
    private final List<String> filePathList = new ArrayList<>();
    private final HashMap<String, Integer> filePathIdx = new HashMap<>();
    private final List<LineMapping> lineData = new ArrayList<>();
    private final List<FunctionMapping> functionData = new ArrayList<>();

    /**
     * Constructs a new SmapStratum object for the given stratum name
     * (e.g., JSP).
     *
     * @param stratumName the name of the stratum (e.g., JSP)
     */
    public SmapStratum(String stratumName) {
        this.stratumName = stratumName;
    }

    @Override
    public void clear() {
        fileNameList.clear();
        filePathList.clear();
        filePathIdx.clear();
        lineData.clear();
    }

    @Override
    public boolean isEmpty() {
        return lineData.isEmpty();
    }

    public String getFile(int i) {
        return fileNameList.get(i);
    }

    public String getPath(int i) {
        return filePathList.get(i);
    }

    // *********************************************************************
    // Methods to add mapping information

    @Override
    public void addFunctionMapping(String functionName, long startLine, long endLine) {
        functionData.add(new FunctionMapping(functionName, startLine, endLine));
    }

    /**
     * Adds record of a new file, by filename and path.  The path
     * may be relative to a source compilation path.
     *
     * @param filename the filename to add, unqualified by path
     * @param filePath the path for the filename, potentially relative
     *                 to a source compilation path
     */
    public int addFile(String filename, String filePath) {
        var idx = filePathIdx.get(filePath);
        if (idx != null) {
            return idx;
        }
        var i = filePathList.size();
        filePathIdx.put(filePath, i);
        filePathList.add(filePath);
        fileNameList.add(filename);
        return i;
    }

    /**
     * Adds complete information about a simple line mapping.  Specify
     * all the fields in this method; the back-end machinery takes care
     * of printing only those that are necessary in the final SMAP.
     * (My view is that fields are optional primarily for spatial efficiency,
     * not for programmer convenience.  Could always add utility methods
     * later.)
     *
     * @param inputStartLine  starting line in the source file
     *                        (SMAP <code>InputStartLine</code>)
     * @param inputFileName   file name of the input file, unqualified by path
     * @param inputFilePath   the filepath (or name) from which the input comes
     *                        (yields SMAP <code>LineFileID</code>)  Use unqualified names
     *                        carefully, and only when they uniquely identify a file.
     * @param inputLineCount  the number of lines in the input to map
     *                        (SMAP <code>LineFileCount</code>)
     * @param outputStartLine starting line in the output file
     *                        (SMAP <code>OutputStartLine</code>)
     * @param outputLineCount number of output lines to map to each
     *                        input line (SMAP <code>OutputLineIncrement</code>).  <i>Given the
     *                        fact that the name starts with "output", I continuously have
     *                        the subconscious urge to call this field
     *                        <code>OutputLineExcrement</code>.</i>
     */
    @Override
    public void addLineData(
            String inputFileName,
            String inputFilePath,
            long inputStartLine,
            int inputLineCount,
            long outputStartLine,
            int outputLineCount) {

        if (outputStartLine == 0) {
            throw new IllegalArgumentException("outputStartLine must be > 0");
        }

        int lineFileID = addFile(inputFileName, inputFilePath);

        // can we merge it into the previous line?
        if (!lineData.isEmpty()) {
            int i = lineData.size() - 1;
            var li = lineData.get(i);

            // is this just increasing the input line count?
            if (li.lineFileID() == lineFileID
                    && inputStartLine == li.inputStartLine() + li.inputLineCount()
                    && outputLineCount == li.outputLineCount()
                    && outputStartLine
                            == li.outputStartLine()
                                    + ((long) li.inputLineCount() * li.outputLineCount())) {
                try {
                    li.withInputLineCount(li.inputLineCount() + inputLineCount);
                } catch (SwitchToLong e) {
                    li = e.getLineInfo();
                    lineData.set(i, li);
                }
                return;
            }

            // is this just increasing the output line count?
            if (li.lineFileID() == lineFileID
                    && inputStartLine == li.inputStartLine()
                    && inputLineCount == 1
                    && li.inputLineCount() == 1
                    && outputStartLine
                            == li.outputStartLine()
                                    + (long) li.inputLineCount() * li.outputLineCount()) {
                try {
                    li.withOutputLineCount(
                            (int) (outputStartLine - li.outputStartLine() + outputLineCount));
                } catch (SwitchToLong e) {
                    li = e.getLineInfo();
                    lineData.set(i, li);
                }
                return;
            }
        }

        // Add a new LineInfo
        LineMapping li = new IntLineInfo();
        try {
            li.withInputStartLine(inputStartLine);
            li.withInputLineCount(inputLineCount);
            li.withOutputStartLine(outputStartLine);
            li.withOutputLineCount(outputLineCount);
        } catch (SwitchToLong e) {
            li = new LongLineInfo();
            li.withInputStartLine(inputStartLine);
            li.withInputLineCount(inputLineCount);
            li.withOutputStartLine(outputStartLine);
            li.withOutputLineCount(outputLineCount);
        }
        li.withLineFileID(lineFileID);
        lineData.add(li);
    }

    /**
     * Combines consecutive LineInfos wherever possible
     */
    public SmapStratum optimizeForWrite() {

        // Incorporate each LineInfo into the previous LineInfo's
        // outputLineCount, if possible
        int i = 0;
        while (i < lineData.size() - 1) {
            var li = lineData.get(i);
            var liNext = lineData.get(i + 1);
            if (li.lineFileID() == liNext.lineFileID()
                    && liNext.inputStartLine() == li.inputStartLine()
                    && liNext.inputLineCount() == 1
                    && li.inputLineCount() == 1
                    && liNext.outputStartLine()
                            == li.outputStartLine()
                                    + (long) li.inputLineCount() * li.outputLineCount()) {
                try {
                    li.withOutputLineCount(
                            (int)
                                    (liNext.outputStartLine()
                                            - li.outputStartLine()
                                            + liNext.outputLineCount()));
                } catch (SwitchToLong e) {
                    li = e.getLineInfo();
                    lineData.set(i, li);
                }
                lineData.remove(i + 1);
            } else {
                i++;
            }
        }

        // Incorporate each LineInfo into the previous LineInfo's
        // inputLineCount, if possible
        i = 0;
        while (i < lineData.size() - 1) {
            var li = lineData.get(i);
            var liNext = lineData.get(i + 1);
            if (li.lineFileID() == liNext.lineFileID()
                    && liNext.inputStartLine() == li.inputStartLine() + li.inputLineCount()
                    && liNext.outputLineCount() == li.outputLineCount()
                    && liNext.outputStartLine()
                            == li.outputStartLine()
                                    + (long) li.inputLineCount() * li.outputLineCount()) {
                try {
                    li.withInputLineCount(li.inputLineCount() + liNext.inputLineCount());
                } catch (SwitchToLong e) {
                    li = e.getLineInfo();
                    lineData.set(i, li);
                }
                lineData.remove(i + 1);
            } else {
                i++;
            }
        }
        return this;
    }

    @Override
    public SmapStratum optimizeForLookups() {
        // sort the lineData by outputStartLine
        lineData.sort(Comparator.comparingLong(LineMapping::outputStartLine));
        functionData.sort(Comparator.comparingLong(FunctionMapping::getStartLine));
        return this;
    }

    /**
     * The caller must call optimizeForLookups() first so that the line data is in sorted
     * since this does a binary search
     */
    public LineMapping getLineMapping(int outputLine) {
        // do a ranged binary search on lineData to find the LineInfo that contains the outputLine
        int left = 0;
        int right = lineData.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            LineMapping midInfo = lineData.get(mid);

            long outputStart = midInfo.outputStartLine();
            long outputEnd =
                    outputStart + ((long) midInfo.inputLineCount() * midInfo.outputLineCount()) - 1;

            if (outputLine >= outputStart && outputLine <= outputEnd) {
                return midInfo;
            } else if (outputLine < outputStart) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return null;
    }

    @Override
    public String getFunctionMapping(int outputLine) {
        // do a ranged binary search on functionData to find the FunctionMapping that contains the
        // outputLine
        int left = 0;
        int right = functionData.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            FunctionMapping midInfo = functionData.get(mid);

            if (outputLine >= midInfo.getStartLine() && outputLine <= midInfo.getEndLine()) {
                return midInfo.getFunctionName();
            } else if (outputLine < midInfo.getStartLine()) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return null;
    }

    /**
     * Returns the name of the stratum.
     */
    public String getStratumName() {
        return stratumName;
    }

    @Override
    public String toString() {
        // check state and initialize buffer
        if (fileNameList.isEmpty() || lineData.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();

        // print StratumSection
        out.append("*S ").append(stratumName).append("\n");

        // print FileSection
        out.append("*F\n");
        int bound = fileNameList.size();
        for (int i = 0; i < bound; i++) {
            String fileName = fileNameList.get(i);
            String filePath = filePathList.get(i);
            if (!fileName.equals(filePath)) {
                out.append("+ ").append(i).append(" ").append(fileName).append("\n");
                out.append(filePath).append("\n");
            } else {
                out.append(i).append(" ").append(fileName).append("\n");
            }
        }

        // print LineSection
        out.append("*L\n");
        bound = lineData.size();
        int lastFileID = 0;

        for (int i = 0; i < bound; i++) {
            LineMapping li = lineData.get(i);
            int fileID = li.lineFileID();
            var includeFileID = fileID != lastFileID;
            out.append(li.getString(includeFileID));
            lastFileID = fileID;
        }

        return out.toString();
    }

    public String toVendorString() {
        StringBuilder out = new StringBuilder();

        // print StratumSection

        out.append("*V\n").append(FUNCTIONS_VENDOR_ID).append("\n");

        for (FunctionMapping fm : functionData) {
            String escapedFunctionName = fm.getFunctionName().replace('\n', '_');
            out.append(fm.getStartLine())
                    .append(",")
                    .append(fm.getEndLine())
                    .append("=")
                    .append(escapedFunctionName)
                    .append("\n");
        }
        return out.toString();
    }

    public List<LineMapping> lineData() {
        return this.lineData;
    }

    public List<FunctionMapping> functionData() {
        return functionData;
    }

    @Override
    public Line getInputLine(int outputLine) {
        var l = getLineMapping(outputLine);
        if (l != null) {
            String fileName = getFile(l.lineFileID());
            String filePath = getPath(l.lineFileID());
            return new Line(fileName, filePath, l.inputStartLine(), l.inputLineCount());
        }
        return null;
    }
}
