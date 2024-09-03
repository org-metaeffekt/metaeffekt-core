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

package org.metaeffekt.core.itest.container;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.filescan.ComponentPatternValidator;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.DuplicateList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.COMPONENT_SOURCE_TYPE;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.VERSION_ANCHOR;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.container.ContainerDumpSetup.exportContainerFromRegistryByRepositoryAndTag;

public class FedoraTest extends AbstractCompositionAnalysisTest {

    @BeforeClass
    public static void prepare() throws IOException, InterruptedException, NoSuchAlgorithmException {
        String path = exportContainerFromRegistryByRepositoryAndTag(null, FedoraTest.class.getSimpleName().toLowerCase(), null, FedoraTest.class.getName());
        String sha256Hash = FileUtils.computeSHA256Hash(new File(path));
        AbstractCompositionAnalysisTest.testSetup = new UrlBasedTestSetup()
                .setSource("file://" + path)
                .setSha256Hash(sha256Hash)
                .setName(FedoraTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(AbstractCompositionAnalysisTest.testSetup.clear());

    }

    @Ignore
    @Test
    public void analyse() throws Exception {
        Assert.assertTrue(AbstractCompositionAnalysisTest.testSetup.rebuildInventory());
    }

    @Test
    public void testComponentPatterns() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        final Inventory referenceInventory = AbstractCompositionAnalysisTest.testSetup.readReferenceInventory();
        final File baseDir = new File(AbstractCompositionAnalysisTest.testSetup.getScanFolder());
        List<FilePatternQualifierMapper> filePatternQualifierMapperList = ComponentPatternValidator.detectDuplicateComponentPatternMatches(referenceInventory, inventory, baseDir);
        DuplicateList duplicateList = new DuplicateList(filePatternQualifierMapperList);
        duplicateList.identifyRemainingDuplicatesWithoutArtifact();
        Assert.assertEquals(0, duplicateList.getRemainingDuplicates().size());
        Assert.assertFalse(duplicateList.getFileWithoutDuplicates().isEmpty());
    }

    @Test
    public void testContainerStructure() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        Analysis analysis = new Analysis(inventory);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "rpm")).hasSizeOf(144);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "python-library")).hasSizeOf(1);
        analysis.selectComponentPatterns(containsToken(VERSION_ANCHOR, "rpmdb.sqlite")).hasSizeGreaterThan(1);
    }
}
