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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.DefaultArtifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.PatternArtifactFilter;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Ignore
public class ExternalRepositoryReportTest {

    private static final String INVENTORY = "/Users/kklein/workspace/metaeffekt-inventory/src/main/resources/inventory/artifact-inventory.xls";
    private static final String LICENSES_FOLDER = "/Users/kklein/workspace/metaeffekt-inventory/src/main/resources/licenses";
    public static final String TARGET_FOLDER = "target/test-external";

    @Test
    public void testInventoryReport() throws Exception {

        InventoryReport report = new InventoryReport();
        report.setFailOnUnknown(false);
        report.setFailOnUnknownVersion(false);
        report.setGlobalInventoryPath(INVENTORY);

        final Inventory inventory = new InventoryReader().readInventory(new File(INVENTORY));

        // apply modifications to simulate defined cases.
        final List<Artifact> artifacts = new ArrayList<>(inventory.getArtifacts());
        inventory.getArtifacts().removeAll(artifacts);
        for (Artifact artifact: artifacts) {
            if (!artifact.getId().contains("*")) {
                final DefaultArtifact candidate = new DefaultArtifact();
                candidate.setId(artifact.getId());
                candidate.deriveArtifactId();
                inventory.getArtifacts().add(candidate);
            }
        }
        inventory.getLicenseMetaData().clear();

        final DefaultArtifact candidate = new DefaultArtifact();
        candidate.setId("effluxlib-1.3.83.jar");
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

        report.setTargetInventoryPath(new File(TARGET_FOLDER, "inventory.xls").getPath());

        report.setLicenseSourcePath(LICENSES_FOLDER);
        report.setLicenseTargetPath(new File(target, "licenses").getAbsolutePath());

        report.createReport();

        Inventory resultInventory = new InventoryReader().readInventory(new File(report.getTargetInventoryPath()));

        final Artifact artifact = resultInventory.findArtifact(candidate.getId());
        Assert.assertNotNull(artifact);
        Assert.assertEquals(artifact.getId(), candidate.getId());
        Assert.assertEquals("effluxlib-1.3.83.jar", artifact.getId());
        Assert.assertEquals("1.3.83", artifact.getVersion());

    }

    @Test
    public void testInventory() throws IOException {
        UpdateVersionRecommendationProcessor processor = new UpdateVersionRecommendationProcessor();
        final Inventory inventory = new InventoryReader().readInventory(new File(INVENTORY));
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


        // test that any artifact can be addressed by id only (relevant for scan based bom generation)
        for (Artifact artifact : inventory.getArtifacts()) {
            if (artifact.getId() != null) {
                if (artifact.getId().endsWith(".jar")) {
                    Assert.assertEquals(artifact.getId(), "jar", artifact.getType());
                }

                DefaultArtifact artifact1 = new DefaultArtifact();
                artifact1.setId(artifact.getId());
                Assert.assertNull(inventory.findArtifact(artifact1));
                Assert.assertNull(inventory.findArtifact(artifact1, false));
                Assert.assertNotNull(inventory.findArtifact(artifact1, true));

                if (StringUtils.isNotBlank(artifact.getGroupId())) {
                    // note: type and classifier are inferred from id
                    artifact1.setGroupId(artifact.getGroupId());
                    artifact1.setVersion(artifact.getVersion());
                    artifact1.deriveArtifactId();
                    System.out.println(artifact);
                    Assert.assertNotNull(inventory.findArtifact(artifact1, false));
                }
            }
        }
    }

    @Test
    public void testValidateInventoryProcessor() throws IOException {
        final Inventory inventory = new InventoryReader().readInventory(new File(INVENTORY));

        Properties properties = new Properties();
        properties.setProperty(ValidateInventoryProcessor.LICENSES_DIR, LICENSES_FOLDER);

        MavenCentralGroupIdProcessor groupIdProcessor = new MavenCentralGroupIdProcessor(properties);
        // groupIdProcessor.process(inventory);

        ValidateInventoryProcessor validateInventoryProcessor = new ValidateInventoryProcessor(properties);
        validateInventoryProcessor.process(inventory);

        final DefaultArtifact candidate = new DefaultArtifact();
        candidate.setId("effluxlib-1.8.83.jar");
        candidate.deriveArtifactId();

        Artifact found = inventory.findArtifact(candidate.getId());
        Assert.assertNotNull(found);
    }

}
