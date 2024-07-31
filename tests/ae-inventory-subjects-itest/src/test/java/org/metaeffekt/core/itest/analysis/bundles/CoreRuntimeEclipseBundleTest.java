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
package org.metaeffekt.core.itest.analysis.bundles;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
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

public class CoreRuntimeEclipseBundleTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://download.eclipse.org/rt/rap/3.20/M1-20220112-0916/plugins/org.eclipse.core.runtime_3.24.0.v20210910-0750.jar")
                .setSha256Hash("46b5698beaa99c475cfd6c790bc509d27e82a7e27f0117044a31455099144d9e")
                .setName(CoreRuntimeEclipseBundleTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception{
        Assert.assertTrue(testSetup.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception{
        Assert.assertTrue(testSetup.rebuildInventory());
    }

    @Test
    public void assertContent() throws Exception {
        final Inventory inventory= testSetup.getInventory();

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logList();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        analysis.selectArtifacts(attributeValue(ID, "org.eclipse.core.runtime_3.24.0.v20210910-0750.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "3.24.0.v20210910-0750")).hasSizeOf(1);

        analysis.selectArtifacts().hasSizeOf(1);

    }

}
