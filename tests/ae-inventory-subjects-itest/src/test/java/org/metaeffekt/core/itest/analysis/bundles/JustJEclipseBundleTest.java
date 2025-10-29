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
import org.metaeffekt.core.inventory.processor.configuration.DirectoryScanAggregatorConfiguration;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.predicates.ContainsToken;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class JustJEclipseBundleTest extends AbstractCompositionAnalysisTest {

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
        assertContent();
    }

    @Test
    public void assertStructure() throws Exception{
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
        final Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logListWithAllAttributes();

        analysis.selectArtifacts(attributeValue(ID, "org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_17.0.2.v20220201-1208.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "17.0.2.v20220201-1208")).hasSizeOf(1);

        // FIXME: this is not of interest; snapshots contributions should be handled as secondary contribution; or be disabled (no value)
        analysis.selectArtifacts(attributeValue(GROUPID, "org.eclipse.justj")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "17.0.2-SNAPSHOT")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(ID, "temurin-jdk-17.0.2")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "17.0.2")).hasSizeOf(1);

        final int size = analysis.selectArtifacts().getItemList().size();

        Assertions.assertThat(size).isEqualTo(38);

        // EXE files
        final ArtifactList exeArtifactList = analysis.selectArtifacts(ContainsToken.containsToken(ID, ".exe"));
        exeArtifactList.hasSizeOf(35);

        // check we do not lose the checksums on the exe files.
        exeArtifactList.filter(a -> a.getChecksum() == null).hasSizeOf(0);
    }
    @Test

    public void assertAggregation() throws Exception {
        final File baseDir = new File(AbstractCompositionAnalysisTest.testSetup.getScanFolder());
        final File aggregationTargetDir = new File(testSetup.getInventoryFolder(), "aggregation");

        FileUtils.deleteDirectoryQuietly(aggregationTargetDir);

        final DirectoryScanAggregatorConfiguration aggregatorConfiguration =
                new DirectoryScanAggregatorConfiguration(testSetup.readReferenceInventory(), testSetup.getInventory(), baseDir);

        aggregatorConfiguration.aggregateFiles(aggregationTargetDir);

        String[] testPaths = new String[] {
            "javac.exe-5d92e5bee0d30faf2e0d600c5cad98ad.zip",
            "org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64-17.0.2-SNAPSHOT.jar-0b16dfa7fb45e3916e8eb8d756d86160.zip",
            "temurin-jdk-17.0.2-3c42528d132e385566b51bb92e7b6006.zip"
        };

        for (String testPath : testPaths) {
            File file = new File(aggregationTargetDir, testPath);
            FileUtils.validateExists(file);
        }
    }

}
