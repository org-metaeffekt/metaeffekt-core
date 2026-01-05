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
package org.metaeffekt.core.itest.analysis.archives;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

@Ignore // Open Code currently unavailable
public class EvbItDigitalTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://gitlab.opencode.de/OC000015621018/evb-it-digital/-/archive/357defbceb1ae895d5a460aee2cfc77375ef04da/evb-it-digital-357defbceb1ae895d5a460aee2cfc77375ef04da.zip")
                .setSha256Hash("e2f1307e9f8c31ef58589d0b8806fa0b65feb06ac5f4ae3a7f307b9ea1b26b66")
                .setName(EvbItDigitalTest.class.getName());
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
        final Inventory inventory = testSetup.getInventory();

        Analysis analysis = new Analysis(inventory);
        analysis.selectArtifacts().logListWithAllAttributes();

        // expect that the substructure is visible
        analysis.selectArtifacts().hasSizeGreaterThan(1);
        analysis.selectArtifacts(attributeValue(TYPE, "web-module")).hasSizeOf(239);
        analysis.selectArtifacts(attributeValue(COMPONENT_SOURCE_TYPE, "cargo-crate")).hasSizeOf(47);
        analysis.selectArtifacts(attributeValue(COMPONENT_SOURCE_TYPE, "cargo-application")).hasSizeOf(1);

        // expect 7 artifacts without version
        analysis.selectArtifacts(attributeValue(VERSION, null)).hasSizeOf(7);
    }
}
