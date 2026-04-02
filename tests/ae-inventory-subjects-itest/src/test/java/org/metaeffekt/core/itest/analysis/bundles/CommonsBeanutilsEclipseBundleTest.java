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
package org.metaeffekt.core.itest.analysis.bundles;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.ID;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.VERSION;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class CommonsBeanutilsEclipseBundleTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeAll
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://download.eclipse.org/virgo/release/updatesite/3.6.2.RELEASE/plugins/org.apache.commons.collections_3.2.0.v201005080500.jar")
                .setSha256Hash("6fec79a57993aa10afe183d81aded02c10b5631547daf725739f1fb9b58b2d5b")
                .setName(CommonsBeanutilsEclipseBundleTest.class.getName());
    }

    @Disabled
    @Test
    public void clear() throws Exception{
        Assertions.assertTrue(testSetup.clear());
    }

    @Disabled
    @Test
    public void inventorize() throws Exception{
        Assertions.assertTrue(testSetup.rebuildInventory());
    }



    @Test
    public void assertContent() throws Exception {
        final Inventory inventory = testSetup.getInventory();

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logList();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        analysis.selectArtifacts(attributeValue(ID, "org.apache.commons.collections_3.2.0.v201005080500.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "3.2.0.v201005080500")).hasSizeOf(1);

        analysis.selectArtifacts().hasSizeOf(1);
    }

}
