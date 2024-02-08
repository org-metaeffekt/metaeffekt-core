/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.itest.javaartifacts;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.metaeffekt.core.itest.inventory.Analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.itest.inventory.dsl.predicates.AttributeValue.attributeValue;

public class CoreRuntimeEclipseBundleTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://download.eclipse.org/rt/rap/3.20/M1-20220112-0916/plugins/org.eclipse.core.runtime_3.24.0.v20210910-0750.jar")
                .setName(CoreRuntimeEclipseBundleTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception{
        Assert.assertTrue(preparer.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception{
        Assert.assertTrue(preparer.rebuildInventory());
    }

    @Test
    public void first() throws Exception{
        LOG.info(preparer.getInventory().toString());
    }

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory= preparer.getInventory();

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logArtifactList();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        analysis.selectArtifacts(attributeValue("Id", "org.eclipse.core.runtime_3.24.0.v20210910-0750.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue("Version", "3.24.0.v20210910-0750")).hasSizeOf(1);

        analysis.selectArtifacts().hasSizeOf(1);

    }

}
