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

import org.assertj.core.api.Assertions;
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

import java.io.File;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class JustJEclipseBundleTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://download.eclipse.org/justj/jres/17/updates/release/17.0.2/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar")
                .setSha256Hash("1743a6794b0c99859424ec993abf6a0b25649cd71aa5e291fd375dfc473f0801")
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
    public void assertStructure() throws Exception{
        LOG.info(testSetup.getInventory().toString());

        final File scanBaseDir =  new File("target/.test/scan/analysis/bundles/JustJEclipseBundleTest");

        {
            // check that the intermediate archives are removed
            final File unwrappedFile = new File(scanBaseDir, "[org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar]/jre/lib/[src.zip]");
            final File originalFile = new File(scanBaseDir, "[org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar]/jre/lib/src.zip)");
            Assertions.assertThat(unwrappedFile).exists();
            Assertions.assertThat(originalFile).doesNotExist();
        }

        {
            // check top-level archives remain
            final File unwrappedFile = new File(scanBaseDir, "[org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar]");
            final File originalFile = new File(scanBaseDir, "org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar");
            Assertions.assertThat(unwrappedFile).exists();
            Assertions.assertThat(originalFile).exists();
        }
    }

    @Test
    public void assertContent() throws Exception {
        final Inventory inventory = testSetup.getInventory();
        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logList();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        analysis.selectArtifacts(attributeValue(ID, "org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "17.0.2.v20220201-1208")).hasSizeOf(1);

        // FIXME: this is not of interest; snapshots contributions should be handled as secondary contribution; or be disabled (no value)
        analysis.selectArtifacts(attributeValue(ID, "org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64-17.0.2-SNAPSHOT.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(GROUPID, "org.eclipse.justj")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(PURL, "pkg:maven/org.eclipse.justj/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64@17.0.2-SNAPSHOT?type=jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "17.0.2-SNAPSHOT")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(ID, "temurin-jdk-17.0.2")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "17.0.2")).hasSizeOf(1);

        final int size = analysis.selectArtifacts().getItemList().size();

        Assert.assertTrue(size == 3 || size == 4); // result depends on whether 7z is installed
    }

}
