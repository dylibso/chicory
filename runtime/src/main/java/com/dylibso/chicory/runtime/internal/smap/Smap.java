// Original came from:
// https://github.com/apache/sling-org-apache-sling-scripting-jsp/blob/4e0f12aab9c42a1475587800cefe6a39721020ec/src/main/java/org/apache/sling/scripting/jsp/jasper/compiler/SmapGenerator.java
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

import com.dylibso.chicory.runtime.LineMapping;
import com.dylibso.chicory.runtime.Stratum;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a source map (SMAP), which serves to associate lines
 * of the input JSP file(s) to lines in the generated servlet in the
 * final .class file, according to the JSR-045 spec.
 *
 * @author Shawn Bayern
 */
public class Smap {

    public static final String FUNCTIONS_VENDOR_ID = "com.dylibso.chicory.functions";

    /*
     * The SMAP syntax is reasonably straightforward.  The purpose of this
     * class is currently twofold:
     *  - to provide a simple but low-level Java interface to build
     *    a logical SMAP
     *  - to serialize this logical SMAP for eventual inclusion directly
     *    into a .class file.
     */

    private String outputFileName;
    private String defaultStratum = "Java";
    private final List<Stratum.Builder> strata = new ArrayList<>();
    private final List<String> embedded = new ArrayList<>();
    private boolean doEmbedded = true;

    /**
     * Sets the filename (without path information) for the generated
     * source file.  E.g., "foo$jsp.java".
     */
    public Smap withOutputFileName(String x) {
        outputFileName = x;
        return this;
    }

    /**
     * Adds the given Stratum.Builder object, representing a Stratum.Builder with
     * logically associated FileSection and LineSection blocks, to
     * the current Smap.  If <code>default</code> is true, this
     * stratum is made the default stratum, overriding any previously
     * set default.
     *
     * @param stratum        the Stratum.Builder object to add
     * @param defaultStratum if <code>true</code>, this Stratum.Builder is considered
     *                       to represent the default SMAP stratum unless
     *                       overwritten
     */
    public Smap withStratum(Stratum.Builder stratum, boolean defaultStratum) {
        strata.add(stratum);
        if (defaultStratum) {
            this.defaultStratum = stratum.stratumName();
        }
        return this;
    }

    /**
     * Adds the given string as an embedded SMAP with the given stratum name.
     *
     * @param smap        the SMAP to embed
     * @param stratumName the name of the stratum output by the compilation
     *                    that produced the <code>smap</code> to be embedded
     */
    public void addSmap(String smap, String stratumName) {
        embedded.add("*O " + stratumName + "\n" + smap + "*C " + stratumName + "\n");
    }

    /**
     * Instructs the Smap whether to actually print any embedded
     * SMAPs or not.  Intended for situations without an SMAP resolver.
     *
     * @param status If <code>false</code>, ignore any embedded SMAPs.
     */
    public void setDoEmbedded(boolean status) {
        doEmbedded = status;
    }

    // *********************************************************************
    // Methods for serializing the logical SMAP

    @Override
    public String toString() {
        // check state and initialize buffer
        if (outputFileName == null) {
            throw new IllegalStateException();
        }
        StringBuilder out = new StringBuilder();

        // start the SMAP
        out.append("SMAP\n");
        out.append(outputFileName).append('\n');
        out.append(defaultStratum).append('\n');

        // include embedded SMAPs
        if (doEmbedded) {
            for (String s : embedded) {
                out.append(s);
            }
        }

        // print our StratumSections, FileSections, and LineSections
        for (Stratum.Builder s : strata) {
            out.append(stratumToString(s));
        }

        if (!getDefaultStratum().functionData().isEmpty()) {
            out.append(toVendorString(getDefaultStratum()));
        }

        // end the SMAP
        out.append("*E\n");

        return out.toString();
    }

    private Stratum.Builder optimize(Stratum.Builder builder) {
        var lineData = builder.lineData();
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
                li.withOutputLineCount(
                        (int)
                                (liNext.outputStartLine()
                                        - li.outputStartLine()
                                        + liNext.outputLineCount()));
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
                li.withInputLineCount(li.inputLineCount() + liNext.inputLineCount());
                lineData.remove(i + 1);
            } else {
                i++;
            }
        }
        return builder;
    }

    private String stratumToString(Stratum.Builder builder) {

        var s = optimize(builder);
        // check state and initialize buffer
        var fileNameList = s.fileNameList();
        var filePathList = s.filePathList();
        var lineData = s.lineData();

        if (fileNameList.isEmpty() || lineData.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();

        // print StratumSection
        out.append("*S ").append(s.stratumName()).append("\n");

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
            LineMapping.Builder li = lineData.get(i);
            int fileID = li.lineFileID();
            var includeFileID = fileID != lastFileID;
            out.append(getString(li, includeFileID));
            lastFileID = fileID;
        }

        return out.toString();
    }

    private String getString(LineMapping.Builder line, boolean includeFileID) {
        if (line.inputStartLine() == -1 || line.outputStartLine() == -1) {
            throw new IllegalStateException();
        }
        StringBuilder out = new StringBuilder();
        out.append(line.inputStartLine());
        if (includeFileID) {
            out.append("#").append(line.lineFileID());
        }
        if (line.inputLineCount() != 1) {
            out.append(",").append(line.inputLineCount());
        }
        out.append(":").append(line.outputStartLine());
        if (line.outputLineCount() != 1) {
            out.append(",").append(line.outputLineCount());
        }
        out.append('\n');
        return out.toString();
    }

    private String toVendorString(Stratum.Builder stratum) {
        StringBuilder out = new StringBuilder();

        // print StratumSection

        out.append("*V\n").append(Smap.FUNCTIONS_VENDOR_ID).append("\n");

        for (Stratum.FunctionMapping fm : stratum.functionData()) {
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

    public Stratum.Builder getDefaultStratum() {
        for (Stratum.Builder s : strata) {
            if (defaultStratum.equals(s.stratumName())) {
                return s;
            }
        }
        return null;
    }

    public List<Stratum.Builder> getStrata() {
        return strata;
    }
}
