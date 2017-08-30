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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.DefaultArtifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.reader.GlobalInventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Ignore
public class ExternalRepositoryReportTest {

    private static final String INVENTORY = "/Users/kklein/workspace/spring-boot-boms/spring-boot-10/src/main/inventory/artifact-inventory.xls";
    private static final String LICENSES_FOLDER = "/Users/kklein/workspace/spring-boot-boms/spring-boot-10/src/main/licenses";
    public static final String TARGET_FOLDER = "target/test-external";

    @Test
    public void testInventoryReport() throws Exception {

        InventoryReport report = new InventoryReport();
        report.setFailOnUnknown(false);
        report.setFailOnUnknownVersion(false);
        report.setGlobalInventoryPath(INVENTORY);
        final Inventory inventory = new GlobalInventoryReader().readInventory(new File(INVENTORY));

        final DefaultArtifact candidate = new DefaultArtifact();
        candidate.setLicense("L");
        candidate.setGroupId("org.springframework");
        candidate.setVersion("4.2.9.RELEASE");
        candidate.setId("spring-aop-4.2.9.RELEASE.jar");
        candidate.deriveArtifactId();
        inventory.getArtifacts().add(candidate);

        report.setRepositoryInventory(inventory);
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
        report.setLicenseTargetPath(new File(target, "licenses").getAbsolutePath());

        report.createReport();
    }

    @Test
    public void testInventory() throws IOException {
        UpdateVersionRecommendationProcessor processor = new UpdateVersionRecommendationProcessor();
        final Inventory inventory = new GlobalInventoryReader().readInventory(new File(INVENTORY));
        processor.process(inventory);

        final DefaultArtifact candidate = new DefaultArtifact();
        candidate.setLicense("L");
        candidate.setGroupId("org.springframework");
        candidate.setVersion("4.2.9.RELEASE");
        candidate.setId("spring-aop-4.2.9.RELEASE.jar");
        candidate.deriveArtifactId();

        Assert.assertNull(inventory.findArtifact(candidate));
        final Artifact artifactClassificationAgnostic = inventory.findArtifactClassificationAgnostic(candidate);
        Assert.assertNotNull(artifactClassificationAgnostic);
        System.out.println(artifactClassificationAgnostic);
    }

    @Test
    public void testValidateLicensesProcessor() throws IOException {
        Properties properties = new Properties();
        properties.setProperty(ValidateLicensesProcessor.LICENSES_DIR, LICENSES_FOLDER);
        ValidateLicensesProcessor validateLicensesProcessor = new ValidateLicensesProcessor(properties);
        final Inventory inventory = new GlobalInventoryReader().readInventory(new File(INVENTORY));
        validateLicensesProcessor.process(inventory);
    }

}
