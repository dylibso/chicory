package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.internal.smap.Smap;
import com.dylibso.chicory.runtime.internal.smap.SmapParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Stratum represents a mapping of source code lines to function names and addresses.
 * <p>
 * It is used to manage the relationship between input source files and their corresponding output lines,
 * as well as to provide a way to retrieve function mappings based on line numbers.
 * </p>
 */
public final class Stratum {

    private final List<String> fileNameList;
    private final List<String> filePathList;
    private final List<LineMapping> lineData;
    private final List<Stratum.FunctionMapping> functionData;

    public static final class FunctionMapping {
        private final String functionName;
        private final long startLine;
        private final long endLine;

        private FunctionMapping(String functionName, long startLine, long endLine) {
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

    /**
     * Creates a stratum builder with the specified name.
     *
     * @param name the name of the stratum
     * @return a new Stratum instance
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String stratumName;
        private final List<String> fileNameList = new ArrayList<>();
        private final List<String> filePathList = new ArrayList<>();
        private final HashMap<String, Integer> filePathIdx = new HashMap<>();
        private final List<LineMapping.Builder> lineData = new ArrayList<>();
        private final List<Stratum.FunctionMapping> functionData = new ArrayList<>();

        private Builder(String name) {
            this.stratumName = name;
        }

        public List<LineMapping.Builder> lineData() {
            return lineData;
        }

        public List<String> fileNameList() {
            return fileNameList;
        }

        public List<String> filePathList() {
            return filePathList;
        }

        public String stratumName() {
            return stratumName;
        }

        public List<Stratum.FunctionMapping> functionData() {
            return functionData;
        }

        public boolean isEmpty() {
            return lineData.isEmpty();
        }

        public void clear() {
            fileNameList.clear();
            filePathList.clear();
            filePathIdx.clear();
            lineData.clear();
            functionData.clear();
        }

        // *********************************************************************
        // Methods to add mapping information

        public Builder withFunctionMapping(String functionName, long startLine, long endLine) {
            functionData.add(new Stratum.FunctionMapping(functionName, startLine, endLine));
            return this;
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
        public Builder withLineMapping(
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
                if (li.lineFileID == lineFileID
                        && inputStartLine == li.inputStartLine + li.inputLineCount
                        && outputLineCount == li.outputLineCount
                        && outputStartLine
                                == li.outputStartLine
                                        + ((long) li.inputLineCount * li.outputLineCount)) {
                    li.withInputLineCount(li.inputLineCount + inputLineCount);
                    return this;
                }

                // is this just increasing the output line count?
                if (li.lineFileID == lineFileID
                        && inputStartLine == li.inputStartLine
                        && inputLineCount == 1
                        && li.inputLineCount == 1
                        && outputStartLine
                                == li.outputStartLine
                                        + (long) li.inputLineCount * li.outputLineCount) {
                    li.withOutputLineCount(
                            (int) (outputStartLine - li.outputStartLine + outputLineCount));
                    return this;
                }
            }

            // Add a new LineInfo
            var li = LineMapping.builder();
            li.withInputStartLine(inputStartLine);
            li.withInputLineCount(inputLineCount);
            li.withOutputStartLine(outputStartLine);
            li.withOutputLineCount(outputLineCount);
            li.withLineFileID(lineFileID);
            lineData.add(li);
            return this;
        }

        public Stratum build() {

            // Optimize the line data for lookups.
            lineData.sort(Comparator.comparingLong(x -> x.outputStartLine));
            functionData.sort(Comparator.comparingLong(x -> x.startLine));

            List<LineMapping> builtLineData = new ArrayList<>();
            for (LineMapping.Builder lineDatum : lineData) {
                builtLineData.add(lineDatum.build());
            }
            return new Stratum(fileNameList, filePathList, builtLineData, functionData);
        }
    }

    /**
     * Converts the given source file and stratum into a SMAP string as defined by the JSR-045 spec.
     *
     * @param sourceFile the name of the source file
     * @param stratum    the stratum to convert
     * @return a string representation of the SMap
     */
    public static String toSMapString(String sourceFile, Stratum.Builder stratum) {
        return new Smap().withOutputFileName(sourceFile).withStratum(stratum, true).toString();
    }

    /**
     * Parses the given SMAP string (as defined by the JSR-045 spec) and returns the
     * default Stratum instance.
     *
     * @param smap the SMAP string to parse
     * @return a Stratum instance representing the parsed SMAP
     */
    public static Stratum parseSMapString(String smap) {
        return SmapParser.parse(smap).getDefaultStratum().build();
    }

    private Stratum(
            List<String> fileNameList,
            List<String> filePathList,
            List<LineMapping> lineData,
            List<FunctionMapping> functionData) {
        this.fileNameList = List.copyOf(fileNameList);
        this.filePathList = List.copyOf(filePathList);
        this.lineData = List.copyOf(lineData);
        this.functionData = List.copyOf(functionData);
    }

    public boolean isEmpty() {
        return lineData.isEmpty();
    }

    public String getFunctionMapping(int outputLine) {
        // do a ranged binary search on functionData to find the FunctionMapping that contains the
        // outputLine
        int left = 0;
        int right = functionData.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Stratum.FunctionMapping midInfo = functionData.get(mid);

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

    public static final class Line {
        private final String fileName;
        private final String filePath;
        private final long line;
        private final int count;

        public Line(String fileName, String filePath, long line, int count) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.line = line;
            this.count = count;
        }

        public String fileName() {
            return fileName;
        }

        public String filePath() {
            return filePath;
        }

        public long line() {
            return line;
        }

        public int count() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Line line1 = (Line) o;
            return line == line1.line
                    && count == line1.count
                    && Objects.equals(fileName, line1.fileName)
                    && Objects.equals(filePath, line1.filePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileName, filePath, line, count);
        }
    }

    public Line getInputLine(int outputLine) {
        var l = getLineMapping(outputLine);
        if (l != null) {
            String fileName = fileNameList.get(l.lineFileID());
            String filePath = filePathList.get(l.lineFileID());
            return new Line(fileName, filePath, l.inputStartLine(), l.inputLineCount());
        }
        return null;
    }

    /**
     * does a binary search for the LineMapping that contains the outputLine
     */
    private LineMapping getLineMapping(int outputLine) {
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
}
