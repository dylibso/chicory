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
    private final List<SmapStratum> strata = new ArrayList<>();
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
     * Adds the given SmapStratum object, representing a Stratum with
     * logically associated FileSection and LineSection blocks, to
     * the current Smap.  If <code>default</code> is true, this
     * stratum is made the default stratum, overriding any previously
     * set default.
     *
     * @param stratum        the SmapStratum object to add
     * @param defaultStratum if <code>true</code>, this SmapStratum is considered
     *                       to represent the default SMAP stratum unless
     *                       overwritten
     */
    public Smap withStratum(SmapStratum stratum, boolean defaultStratum) {
        strata.add(stratum);
        if (defaultStratum) {
            this.defaultStratum = stratum.getStratumName();
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
        for (SmapStratum s : strata) {
            out.append(s.toString());
        }

        if (!getDefaultStratum().functionData().isEmpty()) {
            out.append(getDefaultStratum().toVendorString());
        }

        // end the SMAP
        out.append("*E\n");

        return out.toString();
    }

    public SmapStratum getDefaultStratum() {
        for (SmapStratum s : strata) {
            if (defaultStratum.equals(s.getStratumName())) {
                return s;
            }
        }
        return null;
    }

    public List<SmapStratum> getStrata() {
        return strata;
    }
}
