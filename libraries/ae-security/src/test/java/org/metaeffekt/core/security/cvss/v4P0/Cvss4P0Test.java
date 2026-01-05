/*
 * Copyright 2009-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.security.cvss.v4P0;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cvss4P0Test {

    private final static Logger LOG = LoggerFactory.getLogger(Cvss4P0Test.class);

    private final static File CVSS_RESOURCE_DIR = new File("src/test/resources/cvss");

    @Test
    @Ignore
    public void customTest() {
        final String vectorString = "CVSS:4.0/AV:P/AC:H/AT:P/PR:L/UI:P/VC:L/VI:L/VA:L/SC:H/SI:L/SA:L/E:U/IR:M/AR:H/MAT:P/MPR:N/MUI:P/MVC:N/MVI:L/MVA:L/MSC:L/MSI:L/MSA:S/S:P/R:I/U:Red";
        final Cvss4P0 cvss4P0 = new Cvss4P0(vectorString);

        LOG.info("{}", cvss4P0);
        LOG.info("{}", cvss4P0.getMacroVector());
        LOG.info("{}", cvss4P0.getOverallScore());
    }

    @Test
    public void initialTest() {
        final String vectorString = "CVSS:4.0/AV:N/AC:L/AT:N/PR:H/UI:N/VC:L/VI:L/VA:N/SC:N/SI:N/SA:N";
        final Cvss4P0 cvss4P0 = new Cvss4P0(vectorString);

        Assert.assertEquals(vectorString, cvss4P0.toString());
        Assert.assertEquals("102201", cvss4P0.getMacroVector().toString());
        Assert.assertEquals("1", cvss4P0.getMacroVector().getEq1().getLevel());
        Assert.assertEquals(0, cvss4P0.severityDistance(cvss4P0)); // distance to itself is 0
        Assert.assertEquals(5.3, cvss4P0.getMacroVector().getLookupTableScore(), 0.01);
    }

    @Test
    public void jsToFixedBehaviourTest() {
        final String vectorString = "CVSS:4.0/AV:P/AC:H/AT:P/PR:L/UI:P/VC:L/VI:L/VA:L/SC:H/SI:L/SA:L/E:U/IR:M/AR:H/MAT:P/MPR:N/MUI:P/MVC:N/MVI:L/MVA:L/MSC:L/MSI:L/MSA:S/S:P/R:I/U:Red";
        final Cvss4P0 cvss4P0 = new Cvss4P0(vectorString);

        Assert.assertEquals(vectorString, cvss4P0.toString());
        Assert.assertEquals("212021", cvss4P0.getMacroVector().toString());
        Assert.assertEquals(1.0, cvss4P0.getOverallScore(), 0.01);
    }

    @Test
    public void nomenclatureMultiScoreTest() {
        final String vectorString = "CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:A/VC:N/VI:N/VA:L/SC:N/SI:N/SA:N/MAV:N/MAT:P/MUI:N/MSC:N/E:U";
        final Cvss4P0 cvss4P0 = new Cvss4P0(vectorString);

        Assert.assertEquals(1.7, cvss4P0.getOverallScore(), 0.01);
        Assert.assertEquals(5.1, cvss4P0.getBaseScore(), 0.01);
        Assert.assertEquals(6.3, cvss4P0.getEnvironmentalScore(), 0.01);
        Assert.assertEquals(1.2, cvss4P0.getThreatScore(), 0.01);
    }

    @Test
    public void parseAndCheckAllCvssMacroVectors001Test() throws IOException {
        final File cvssVectorsFile = new File(CVSS_RESOURCE_DIR, "cvss-4.0-macro-vectors-001.txt");
        final List<String> lines = FileUtils.readLines(cvssVectorsFile, StandardCharsets.UTF_8);

        final List<String> invalidVectors = new ArrayList<>();
        final List<String> toStringNotEqualsVectors = new ArrayList<>();
        final List<String> incorrectScores = new ArrayList<>();
        final Map<String, String> exceptionVectors = new LinkedHashMap<>();

        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            try {
                final String[] parts = line.split(" ");
                final String vectorString = parts[0];
                final String expectedMacroVector = parts[1];
                final double expectedResultingScore = Double.parseDouble(parts[2]);

                final Cvss4P0 cvss4P0 = new Cvss4P0(vectorString);
                final Cvss4P0MacroVector macroVector = cvss4P0.getMacroVector();
                final String actualMacroVector = macroVector.toString();
                final double actualResultingScore = cvss4P0.getOverallScore();

                if (!expectedMacroVector.equals(actualMacroVector)) {
                    invalidVectors.add(line);
                }
                if (!vectorString.equals(cvss4P0.toString())) {
                    toStringNotEqualsVectors.add(line + " != " + cvss4P0);
                }
                if (actualResultingScore != expectedResultingScore) {
                    incorrectScores.add(line + " != " + actualResultingScore);
                }
            } catch (Exception e) {
                exceptionVectors.put(line, e.getMessage());
            }
        }

        if (!invalidVectors.isEmpty()) {
            LOG.error("Invalid macro vectors:");
            for (String invalidVector : invalidVectors) {
                LOG.error("  {}", invalidVector);
            }
        }

        if (!toStringNotEqualsVectors.isEmpty()) {
            LOG.error("toString() not equals:");
            for (String invalidVector : toStringNotEqualsVectors) {
                LOG.error("  {}", invalidVector);
            }
        }

        if (!incorrectScores.isEmpty()) {
            LOG.error("Incorrect scores:");
            for (String incorrectScore : incorrectScores) {
                LOG.error("  {}", incorrectScore);
            }
        }

        if (!exceptionVectors.isEmpty()) {
            LOG.error("Exception vectors:");
            for (Map.Entry<String, String> entry : exceptionVectors.entrySet()) {
                LOG.error("  {} ({})", entry.getKey(), entry.getValue());
            }
        }

        Assert.assertTrue(invalidVectors.isEmpty());
        Assert.assertTrue(toStringNotEqualsVectors.isEmpty());
        Assert.assertTrue(incorrectScores.isEmpty());
        Assert.assertTrue(exceptionVectors.isEmpty());

        LOG.info("Successfully validated [{}] vectors", lines.size());
    }
}