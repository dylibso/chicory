package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.internal.smap.Smap;
import com.dylibso.chicory.runtime.internal.smap.SmapParser;
import com.dylibso.chicory.runtime.internal.smap.SmapStratum;
import java.util.Objects;

/**
 * Stratum represents a mapping of source code lines to function names and addresses.
 * <p>
 * It is used to manage the relationship between input source files and their corresponding output lines,
 * as well as to provide a way to retrieve function mappings based on line numbers.
 * This interface allows for the creation, parsing, and manipulation of stratum data.
 * </p>
 */
public interface Stratum {

    final class Line {
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

    /**
     * Creates a new Stratum instance with the specified name.
     *
     * @param name the name of the stratum
     * @return a new Stratum instance
     */
    static Stratum create(String name) {
        return new SmapStratum(name);
    }

    /**
     * Converts the given source file and stratum into a SMAP string as defined by the JSR-045 spec.
     *
     * @param sourceFile the name of the source file
     * @param stratum    the stratum to convert
     * @return a string representation of the SMap
     */
    static String toSMapString(String sourceFile, Stratum stratum) {
        return new Smap()
                .withOutputFileName(sourceFile)
                .withStratum(((SmapStratum) stratum).optimizeForWrite(), true)
                .toString();
    }

    /**
     * Parses the given SMAP string (as defined by the JSR-045 spec) and returns the
     * default Stratum instance.
     *
     * @param smap the SMAP string to parse
     * @return a Stratum instance representing the parsed SMAP
     */
    static Stratum parseSMapString(String smap) {
        return SmapParser.parse(smap).getDefaultStratum().optimizeForLookups();
    }

    /**
     * Sorts the stratum data for optimized lookups using a binary search.
     */
    Stratum optimizeForLookups();

    /*
     * This depends on the stratum being optimized for lookups using the
     * optimizeForLookups() method.
     */
    Line getInputLine(int outputLine);

    /*
     * This depends on the stratum being optimized for lookups using the
     * optimizeForLookups() method.
     */
    String getFunctionMapping(int lineNumber);

    /**
     * resets the stratum data, clearing all mappings and line data.
     */
    void clear();

    /**
     * Checks if the stratum is empty, meaning it contains no line data or function mappings.
     *
     * @return true if the stratum is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Adds a line data mapping to the stratum.
     *
     * @param inputFileName   the name of the input file
     * @param inputFilePath   the path of the input file
     * @param inputStartLine  the starting line number in the input file
     * @param inputLineCount  the number of lines in the input file
     * @param outputStartLine the starting line number in the output
     * @param outputLineCount the number of lines in the output
     */
    void addLineData(
            String inputFileName,
            String inputFilePath,
            long inputStartLine,
            int inputLineCount,
            long outputStartLine,
            int outputLineCount);

    /**
     * Adds a function mapping to the stratum.
     *
     * @param functionName the name of the function
     * @param startAddress the starting address of the function
     * @param endAddress   the ending address of the function
     */
    void addFunctionMapping(String functionName, long startAddress, long endAddress);
}
