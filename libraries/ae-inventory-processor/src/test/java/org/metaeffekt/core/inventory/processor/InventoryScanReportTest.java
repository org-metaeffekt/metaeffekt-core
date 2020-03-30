/**
 * Copyright 2009-2020 the original author or authors.
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

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.InventoryScanReport;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InventoryScanReportTest {

    private static final File GLOBAL_INVENTORY_DIR = new File(
            "../../inventory/src/main/resources/META-INF/");
    private static final String GLOBAL_INVENTORY_INCLUDES =
            "ae-core-artifact-inventory.xls";

    /**
     * Local test based on sample artifact. Ignored in general build.
     *
     * FIXME: enable test for continuous integration with stripped sample artifact
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testInventory() throws Exception {
        InventoryScanReport report = new InventoryScanReport();
        report.setReferenceInventoryDir(GLOBAL_INVENTORY_DIR);
        report.setReferenceInventoryIncludes(GLOBAL_INVENTORY_INCLUDES);
        report.setInputDirectory(new File("C:/dev/tmp/bominput"));
        report.setScanDirectory(new File("C:/dev/tmp/bomscan"));
        report.setTargetInventoryPath("C:/dev/tmp/bomscan/report.xls");

        List<Artifact> addOnArtifacts = new ArrayList<Artifact>();
        Artifact apacheAnt = new Artifact();
        apacheAnt.setArtifactId("apache-ant-1.8.2.zip");
        apacheAnt.setComponent("Apache Ant");
        apacheAnt.setVersion("1.8.2");
        addOnArtifacts.add(apacheAnt);
        report.setAddOnArtifacts(addOnArtifacts);

        report.setScanIncludes(new String[]{"**/*"});
        report.setScanExcludes(new String[]{"-nothing-"});

        report.createReport();
    }

    @Ignore
    @Test
    public void testProjectInventory() throws Exception {

        InventoryScanReport report = new InventoryScanReport();
        report.setReferenceInventoryDir(GLOBAL_INVENTORY_DIR);
        report.setReferenceInventoryIncludes(GLOBAL_INVENTORY_INCLUDES);

        report.setInputDirectory(new File("--somewhere--"));
        report.setScanDirectory(new File("target/bomscan"));
        report.setTargetInventoryPath("target/report.xls");

        report.setScanIncludes(new String[]{"**/*.jar", "**/*.zip", "**/*.war"});
        report.setScanExcludes(new String[]{"-nothing-"});

        report.createReport();
    }

    @Ignore
    @Test
    public void inventoryScanReport() throws Exception {
        final InventoryScanReport report = setupReport();

        report.createReport();

    }

    private InventoryScanReport setupReport() {
        final InventoryScanReport report = new InventoryScanReport();

        report.setReferenceInventoryDir(new File("XXX/inventory/src/main/resources"));
        report.setReferenceInventoryIncludes("**/*.xls");
        report.setReferenceComponentPath("XXX/inventory/src/main/resources/components");
        report.setReferenceLicensePath("XXX/inventory/src/main/resources/licenses");

        final File inputDir = new File("XXX/target/contents");
        final File scanDir = new File("XXX/target/bomscan");

        final String[] scanIncludes = new String[] {"**/*"};
        final String[] scanExcludes = new String[] {"--none--"};

        report.setInputDirectory(inputDir);
        report.setScanDirectory(scanDir);
        report.setScanExcludes(scanExcludes);
        report.setScanIncludes(scanIncludes);

        report.setTargetInventoryDir(new File("target"));
        report.setTargetInventoryPath("result-inventory.xls");
        return report;
    }

    @Ignore
    @Test
    public void createReportFromScannedInventory() throws Exception  {
        Inventory scanInventory = new InventoryReader().readInventory(new File("target/scan-inventory.xls"));

        final InventoryScanReport report = setupReport();

        report.createReport(null, scanInventory);

        Inventory resultInventory = new InventoryReader().readInventory(new File("target/result-inventory.xls"));

        for (Artifact artifact : resultInventory.getArtifacts()) {
            if (artifact.getId().contains("Java Runtime")) {
                System.out.println(artifact.createCompareStringRepresentation());
            }
        }
    }

}