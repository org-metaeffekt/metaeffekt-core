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

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@org.junit.Ignore
public class InitialRepositoryTest {

    private static final String SOURCE_INVENTORY = "/Volumes/USB/lcm/input/artifact-inventory-thirdparty-license_verification.xls";
    private static final String TARGET_INVENTORY = "/Volumes/USB/lcm/out/out-inventory.xls";


    @Test
    public void testRepositoryReport() throws Exception {

        File targetFolder = new File("/Volumes/USB/lcm/out");
        targetFolder.mkdirs();

        File sourceFile = new File(SOURCE_INVENTORY);
        File targetFile = new File(TARGET_INVENTORY);

        Properties properties = new Properties();
        properties.setProperty(MavenCentralVersionProcessor.GROUPID_EXCLUDE_PATTERNS, "-nothing-");
        properties.setProperty(MavenCentralVersionProcessor.ARTIFACTID_EXCLUDE_PATTERNS, "-nothing-");

        InventoryUpdate inventoryUpdate = new InventoryUpdate();
        inventoryUpdate.setSourceInventoryFile(sourceFile);
        inventoryUpdate.setTargetInventoryFile(targetFile);

        List<InventoryProcessor> inventoryProcessors = new ArrayList<InventoryProcessor>();
        inventoryProcessors.add(new CleanupInventoryProcessor(properties));
        inventoryProcessors.add(new UpdateVersionRecommendationProcessor(properties));

        inventoryUpdate.setInventoryProcessors(inventoryProcessors);

        Inventory inventory = inventoryUpdate.process();


        System.out.println(inventory.getArtifacts().size());

        InventoryReport report = new InventoryReport();
        report.setFailOnUnknown(false);
        report.setFailOnUnknownVersion(false);

        report.setReferenceInventoryDir(new File(TARGET_INVENTORY).getParentFile());
        report.setReferenceInventoryIncludes(new File(TARGET_INVENTORY).getName());
        report.setRepositoryInventory(inventory);
        //PatternArtifactFilter artifactFilter = new PatternArtifactFilter();
        // artifactFilter.addIncludePattern("^org\\.metaeffekt\\..*$:*");
        //sreport.setArtifactFilter(artifactFilter);

        File target = new File("/Volumes/USB/lcm/out/report");
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
    public void createDiff() throws IOException {
        String inventory1 = "/Volumes/USB/lcm/input/artifact-inventory-thirdparty-license_verification.xls";
        String inventory2 = "/Volumes/USB/lcm/input/artifact-inventory-thirdparty.xls";

        Inventory inv1 = new InventoryReader().readInventory(new File(inventory1));
        Inventory inv2 = new InventoryReader().readInventory(new File(inventory2));

        inv1.dumpAsFile(new File("/Volumes/USB/lcm/input/artifact-inventory-thirdparty-license_verification.xls.txt"));
        inv2.dumpAsFile(new File("/Volumes/USB/lcm/input/artifact-inventory-thirdparty.xls.txt"));
    }

}
