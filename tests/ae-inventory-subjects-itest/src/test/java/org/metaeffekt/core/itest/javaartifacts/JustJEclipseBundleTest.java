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
import org.metaeffekt.core.itest.common.setup.AbstractBasicInvariantsTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.itest.common.Analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class JustJEclipseBundleTest extends AbstractBasicInvariantsTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://download.eclipse.org/justj/jres/17/updates/release/17.0.2/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar")
                .setName(JustJEclipseBundleTest.class.getName());
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
    public void first() throws Exception{
        LOG.info(testSetup.getInventory().toString());
    }

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory = testSetup.getInventory();
        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logArtifactList();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        analysis.selectArtifacts(attributeValue("Id", "org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue("Version", "17.0.2.v20220201-1208")).hasSizeOf(1);

        analysis.selectArtifacts(attributeValue("Id", "org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64-17.0.2-SNAPSHOT.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue("Group Id", "org.eclipse.justj")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue("Version", "17.0.2-SNAPSHOT")).hasSizeOf(1);

        analysis.selectArtifacts(attributeValue("Id", "temurin-jdk-17.0.2")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue("Version", "17.0.2")).hasSizeOf(1);


        analysis.selectArtifacts().hasSizeOf(3);
    }

}
