/**
 * Copyright 2009-2018 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.ArtifactLicenseData;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepositoryReportTest {

    public static final String TARGET_FOLDER = "target/test-inventory-01";

    private static final String INVENTORY = "src/test/resources/test-inventory-01/inventory/artifact-inventory.xls";
    private static final String LICENSES_FOLDER = "src/test/resources/test-inventory-01/licenses";
    public static final String UTF_8 = "UTF-8";

    @Test
    public void testInventoryReport() throws Exception {

        InventoryReport report = new InventoryReport();
        report.setFailOnUnknown(false);
        report.setFailOnUnknownVersion(false);
        report.setGlobalInventoryPath(INVENTORY);
        report.setRepositoryInventory(new InventoryReader().readInventory(new File(INVENTORY)));
        PatternArtifactFilter artifactFilter = new PatternArtifactFilter();
        artifactFilter.addIncludePattern("^org\\.metaeffekt\\..*$:*");
        report.setArtifactFilter(artifactFilter);

        File target = new File(TARGET_FOLDER);
        target.mkdirs();
        File licenseReport = new File(target, "license.dita");
        File componentReport = new File(target, "component-report.dita");
        File noticeReport = new File(target, "notice.dita");
        File artifactReport = new File(target, "artifacts.dita");
        File mavenPom = new File(target, "ae-pom.xml");

        report.setTargetDitaLicenseReportPath(licenseReport.getAbsolutePath());
        report.setTargetDitaNoticeReportPath(noticeReport.getAbsolutePath());
        report.setTargetDitaReportPath(artifactReport.getAbsolutePath());
        report.setTargetDitaComponentReportPath(componentReport.getAbsolutePath());
        report.setTargetMavenPomPath(mavenPom.getAbsolutePath());

        report.setLicenseSourcePath(LICENSES_FOLDER);
        final File targetLicensesFolder = new File(target, "licenses");
        report.setLicenseTargetPath(targetLicensesFolder.getAbsolutePath());

        final boolean valid = report.createReport();

        assertTrue(valid);
        assertTrue(targetLicensesFolder.exists());

        // check first-level license folders are created as expected
        assertTrue(new File(targetLicensesFolder, "A-License").exists());
        assertTrue(new File(targetLicensesFolder, "A-License-B-License").exists());
        assertTrue(new File(targetLicensesFolder, "B-License").exists());
        assertTrue(new File(targetLicensesFolder, "D-License").exists());
        assertTrue(new File(targetLicensesFolder, "G-License-(with-sub-components)").exists());
        assertFalse(new File(targetLicensesFolder, "T-License").exists());

        // check multiple licensed artifacts are multiplied to the different license folders
        assertTrue(new File(targetLicensesFolder, "A-License-B-License/AlphaBeta-Component-1.0.0").exists());
        assertTrue(new File(targetLicensesFolder, "A-License/AlphaBeta-Component-1.0.0").exists());

        // check license information is multiplied for sub-components
        assertTrue(new File(targetLicensesFolder, "G-License-(with-sub-components)/Gamma-Component-1.0.0").exists());
        assertTrue(new File(targetLicensesFolder, "A-License/Gamma-Component-1.0.0").exists());
        assertTrue(new File(targetLicensesFolder, "B-License/Gamma-Component-1.0.0").exists());

        // check generated DITA files contain the appropriate details
        String artifacts = FileUtils.readFileToString(artifactReport, UTF_8);
        assertTrue(artifacts.contains("<xref href=\"licenses/A-License/\" type=\"html\" scope=\"external\">A License</xref>"));
        assertTrue(artifacts.contains("<xref href=\"licenses/B-License/\" type=\"html\" scope=\"external\">B License</xref>"));
        assertTrue(artifacts.contains("<xref href=\"licenses/A-License-B-License/\" type=\"html\" scope=\"external\">A License + B License</xref>"));
        assertTrue(artifacts.contains("<xref href=\"licenses/G-License-(with-sub-components)/\" type=\"html\" scope=\"external\">G License (with sub-components)</xref>"));
        assertTrue(artifacts.contains("<xref href=\"licenses/D-License/\" type=\"html\" scope=\"external\">D License</xref>"));

        String components = FileUtils.readFileToString(componentReport, UTF_8);
        assertTrue(components.contains("<xref href=\"licenses/A-License/\" type=\"html\" scope=\"external\">A License</xref>"));
        assertTrue(components.contains("<xref href=\"licenses/B-License/\" type=\"html\" scope=\"external\">B License</xref>"));
        assertTrue(components.contains("<xref href=\"licenses/A-License-B-License/\" type=\"html\" scope=\"external\">A License + B License</xref>"));
        assertTrue(components.contains("<xref href=\"licenses/G-License-(with-sub-components)/\" type=\"html\" scope=\"external\">G License (with sub-components)</xref>"));
        assertTrue(components.contains("<xref href=\"licenses/D-License/\" type=\"html\" scope=\"external\">D License</xref>"));

        String licenses = FileUtils.readFileToString(licenseReport, UTF_8);
        assertTrue(licenses.contains("<xref href=\"licenses/A-License/\" type=\"html\" scope=\"external\">A License</xref>"));
        assertTrue(licenses.contains("<xref href=\"licenses/B-License/\" type=\"html\" scope=\"external\">B License</xref>"));
        assertTrue(licenses.contains("<xref href=\"licenses/D-License/\" type=\"html\" scope=\"external\">D License</xref>"));
        assertFalse(licenses.contains("<xref href=\"licenses/A-License-B-License/\" type=\"html\" scope=\"external\">A License + B License</xref>"));
        assertFalse(licenses.contains("<xref href=\"licenses/G-License-(with-sub-components)/\" type=\"html\" scope=\"external\">G License (with sub-components)</xref>"));

        String notices = FileUtils.readFileToString(noticeReport, UTF_8);
        assertTrue(notices.contains("Notice for Alpha component licensed under A License."));
        assertTrue(notices.contains("Notice for Beta component licensed under B License."));
        assertTrue(notices.contains("Notice for AlphaBeta component licensed under either A License or B License. A License is selected for this distribution."));
        assertTrue(notices.contains("Notice for Gamma component, which contains sub-components licensed under A License and B License."));

        List<ArtifactLicenseData> artifactLicenseData = report.getLastProjectInventory().evaluateNotices("A License");
        assertTrue(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Alpha Component")));
        assertTrue(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Gamma Component")));
        assertFalse(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Beta Component")));

        artifactLicenseData = report.getLastProjectInventory().evaluateNotices("B License");
        assertTrue(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Beta Component")));
        assertTrue(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Gamma Component")));
        assertFalse(artifactLicenseData.stream().anyMatch(l -> l.getComponentName().equals("Alpha Component")));

        artifactLicenseData = report.getLastProjectInventory().evaluateNotices("D License");
        assertTrue(artifactLicenseData.isEmpty());

        artifactLicenseData = report.getLastProjectInventory().evaluateNotices("G License (with sub-components)");
        assertTrue(artifactLicenseData.isEmpty());
    }

    @Test
    public void testStringEscaping() {

        InventoryReport inventoryReport = new InventoryReport();

        Assert.assertEquals("this&amp;that.&#8203;those-&#8203;these_&#8203;which",
                inventoryReport.xmlEscapeArtifactId("this&that.those-these_which"));


        Assert.assertEquals("&nbsp;", inventoryReport.xmlEscapeArtifactId(null));

    }

}
