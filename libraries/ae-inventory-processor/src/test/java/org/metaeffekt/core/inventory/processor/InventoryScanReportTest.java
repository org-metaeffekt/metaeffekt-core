/**
 * Copyright 2009-2016 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.model.DefaultArtifact;
import org.metaeffekt.core.inventory.processor.report.InventoryScanReport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InventoryScanReportTest {

    private static final String GLOBAL_INVENTORY =
            "C:/dev/workspace/artifact-inventory-trunk/thirdparty/src/main/resources/META-INF/artifact-inventory-thirdparty-2016-Q1.xls";

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
        report.setGlobalInventoryPath(GLOBAL_INVENTORY);
        report.setInputDirectory(new File("C:/dev/tmp/bominput"));
        report.setScanDirectory(new File("C:/dev/tmp/bomscan"));
        report.setTargetInventoryPath("C:/dev/tmp/bomscan/report.xls");

        List<Artifact> addOnArtifacts = new ArrayList<Artifact>();
        DefaultArtifact apacheAnt = new DefaultArtifact();
        apacheAnt.setArtifactId("apache-ant-1.8.2.zip");
        apacheAnt.setName("Apache Ant");
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
        report.setGlobalInventoryPath(GLOBAL_INVENTORY);
        report.setInputDirectory(new File("--somewhere--"));
        report.setScanDirectory(new File("target/bomscan"));
        report.setTargetInventoryPath("target/report.xls");

        report.setScanIncludes(new String[]{"**/*.jar", "**/*.zip", "**/*.war"});
        report.setScanExcludes(new String[]{"-nothing-"});

        report.createReport();
    }

}
