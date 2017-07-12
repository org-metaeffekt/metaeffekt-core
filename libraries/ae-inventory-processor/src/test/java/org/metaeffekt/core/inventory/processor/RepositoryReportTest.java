/**
 * Copyright 2009-2017 the original author or authors.
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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.reader.GlobalInventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Ignore
public class RepositoryReportTest {

    private static final String GLOBAL_INVENTORY_ON_CLASSPATH = "/META-INF/ae-core-artifact-inventory.xls";
   //private static final String GLOBAL_INVENTORY = "/Users/kklein/workspace/metaeffekt-core/inventory/src/main/resources/META-INF/ae-core-artifact-inventory.xls";

    private static final String GLOBAL_INVENTORY = "/Users/kklein/Documents/venture/internal-projects/metaeffekt-lcm/{metaeffekt} inventory_02-07-2017/inventory/artifact-inventory.xls";

    private static final String REPOSITORY = "repository";

    @Test
    public void testInventory() throws Exception {
        Inventory inventory = new GlobalInventoryReader().readInventory(new File(GLOBAL_INVENTORY));
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.deriveArtifactId();
        }

        new InventoryWriter().writeInventory(inventory, new File("target", "out.xls"));

        inventory = new GlobalInventoryReader().readInventory(new File("target", "out.xls"));
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.deriveArtifactId();
        }

        // filter notices to only include the required notices
        Set<LicenseMetaData> covered = new HashSet<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            LicenseMetaData match = inventory.findMatchingLicenseMetaData(artifact.getName(), artifact.getLicense(), artifact.getVersion());
            if (match != null) {
                covered.add(match);
            }
        }
        inventory.getLicenseMetaData().retainAll(covered);

        new InventoryWriter().writeInventory(inventory, new File("target", "out2.xls"));

        System.out.println(inventory.evaluateLicenses(true, false));

        Assert.assertNotNull(inventory.getLicenseMetaData());

        List<String> evaluateLicenses = inventory.evaluateLicenses(false);
        Assert.assertNotNull(evaluateLicenses);

        String selectedLicense = evaluateLicenses.iterator().next();
        List<ArtifactLicenseData> licenseMetaData = inventory.evaluateNotices(selectedLicense);
        Assert.assertNotNull(licenseMetaData);
    }

    @Test
    public void testGlobalInventoryReport() throws Exception {

        InventoryReport report = new InventoryReport();
        report.setFailOnUnknown(false);
        report.setFailOnUnknownVersion(false);
        report.setGlobalInventoryPath(GLOBAL_INVENTORY);
        report.setRepositoryInventory(new GlobalInventoryReader().readInventory(new File(GLOBAL_INVENTORY)));
        PatternArtifactFilter artifactFilter = new PatternArtifactFilter();
        artifactFilter.addIncludePattern("^org\\.metaeffekt\\..*$:*");
        report.setArtifactFilter(artifactFilter);

        File target = new File("target");
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

        report.createReport();
    }

    @Test
    @Ignore
    public void testRepositoryReport() throws Exception {

        String repoPath = inferRepoPath();

        if (repoPath != null) {
            InventoryReport report = new InventoryReport();
            report.setFailOnUnknown(false);
            report.setFailOnUnknownVersion(false);
            report.setGlobalInventoryPath(GLOBAL_INVENTORY);
            report.setRepositoryPath(GLOBAL_INVENTORY);
            report.setRepositoryPath(repoPath);
            PatternArtifactFilter artifactFilter = new PatternArtifactFilter();
            artifactFilter.addIncludePattern("^org\\.metaeffekt\\..*$:*");
            report.setArtifactFilter(artifactFilter);

            File target = new File("target");
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

            report.createReport();
        } else {
            new IllegalStateException("Cannot produce report as local repository was not readable.");
        }

    }

    @Test
    @Ignore
    public void testRepositoryReportFromClasspath() throws Exception {

        String repoPath = inferRepoPath();

        if (repoPath != null) {
            InventoryReport report = new InventoryReport();
            report.setFailOnUnknown(false);
            report.setFailOnUnknownVersion(false);
            report.setGlobalInventoryPath(GLOBAL_INVENTORY_ON_CLASSPATH);
            report.setRepositoryPath(repoPath);
            PatternArtifactFilter artifactFilter = new PatternArtifactFilter();
            artifactFilter.addIncludePattern("^org\\.metaeffekt\\..*$:*");
            report.setArtifactFilter(artifactFilter);

            List<Artifact> addOnArtifacts = new ArrayList<Artifact>();

            DefaultArtifact licenseOnlyArtifact = new DefaultArtifact();
            licenseOnlyArtifact.setLicense("Karsten's Open Source License 7.1");
            addOnArtifacts.add(licenseOnlyArtifact);

            DefaultArtifact obligationAddOn = new DefaultArtifact();
            obligationAddOn.setLicense("MyLicense");
            obligationAddOn.setName("MyComponent");
            obligationAddOn.setVersion("1.0.3");
            addOnArtifacts.add(obligationAddOn);

            report.setAddOnArtifacts(addOnArtifacts);

            report.createReport();

            Inventory projectInventory = report.getLastProjectInventory();
            List<String> licenses = projectInventory.evaluateLicenses(false);

            Assert.assertTrue(licenses.contains("Apache License Version 2.0"));
            Assert.assertTrue(licenses.contains("Karsten's Open Source License 7.1"));
            Assert.assertTrue(licenses.contains("MyLicense"));

        } else {
            new IllegalStateException("Cannot produce report as local repository was not readable.");
        }

    }

    private String inferRepoPath() {
        // infer location of local repository
        String classpath = System.getProperty("java.class.path");
        String pathSeparator = System.getProperty("path.separator");
        String fileSeparator = System.getProperty("file.separator");

        String[] splitClassPath = classpath.split("\\" + pathSeparator);

        String repoPath = null;
        for (int i = 0; i < splitClassPath.length; i++) {

            String path = splitClassPath[i];
            int index = path.lastIndexOf(fileSeparator + REPOSITORY + fileSeparator);
            if (index > 0) {
                repoPath = path.substring(0, index + REPOSITORY.length() + 1);
                break;
            }
        }
        return repoPath;
    }

}
