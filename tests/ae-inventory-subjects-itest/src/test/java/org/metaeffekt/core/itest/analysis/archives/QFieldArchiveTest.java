/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.itest.analysis.archives;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.configuration.DirectoryScanAggregatorConfiguration;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import java.io.File;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class QFieldArchiveTest extends AbstractCompositionAnalysisTest {

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://github.com/opengisch/QField/archive/refs/tags/v3.4.7.zip")
                .setSha256Hash("aa4084e2291e2a9bde8540d7f321324aa3e28d9d943631ba7d63b1dfdc6d1c56")
                .setName(QFieldArchiveTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(testSetup.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(testSetup.rebuildInventory());
    }

    @Test
    public void assertContent() throws Exception {
        final ArtifactList artifactList = getAnalysisAfterInvariantCheck().selectArtifacts();

        artifactList.logListWithAllAttributes();

        artifactList.with(attributeValue(ID, "LICENSE"),
                    attributeValue(CHECKSUM, "e8c1458438ead3c34974bc0be3a03ed6"))
                .assertNotEmpty();

        final File baseDir = new File(AbstractCompositionAnalysisTest.testSetup.getScanFolder());
        final File aggregationTargetDir = new File(testSetup.getInventoryFolder(), "aggregation");
        final File resultFile = new File("target/aggregated-inventory.xlsx");

        final DirectoryScanAggregatorConfiguration aggregatorConfiguration =
                new DirectoryScanAggregatorConfiguration(testSetup.readReferenceInventory(), testSetup.getInventory(), baseDir);

        final Inventory aggregatedInventory = new Inventory();
        aggregatorConfiguration.contribute(aggregationTargetDir, aggregatedInventory);

        new InventoryWriter().writeInventory(aggregatedInventory, resultFile);

        final Artifact artifact = aggregatedInventory.findArtifactByIdAndChecksum("LICENSE", "e8c1458438ead3c34974bc0be3a03ed6");
        Assertions.assertThat(artifact).isNotNull();

        Assertions.assertThat(artifact.get(ARTIFACT_ROOT_PATHS)).isNotNull();
    }
}
