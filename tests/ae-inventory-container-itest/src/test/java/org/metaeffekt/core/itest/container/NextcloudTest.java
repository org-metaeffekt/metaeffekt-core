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

package org.metaeffekt.core.itest.container;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.DuplicateList;
import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.FolderBasedTestSetup;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Predicate;

import static org.metaeffekt.core.inventory.processor.filescan.ComponentPatternValidator.evaluateComponentPatterns;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.COMPONENT_SOURCE_TYPE;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.container.ContainerDumpSetup.saveContainerFromRegistryByRepositoryAndTag;


// FIXME: this test will be ignored for now, since it is extremely slow
@Disabled
public class NextcloudTest extends AbstractCompositionAnalysisTest {

    public static final NamedBasePredicate<AssetMetaData> CONTAINER_ASSET_PREDICATE = new NamedBasePredicate<AssetMetaData>() {
        @Override
        public Predicate<AssetMetaData> getPredicate() {
            return a -> "container".equals(a.get(TYPE));
        }

        @Override
        public String getDescription() {
            return "Asset of type container";
        }
    };

    @BeforeAll
    public static void prepare() throws IOException, InterruptedException, NoSuchAlgorithmException {
        final File baseDir = saveContainerFromRegistryByRepositoryAndTag(
                null,
                NextcloudTest.class.getSimpleName().toLowerCase(),
                "30",
                NextcloudTest.class.getName());

        AbstractCompositionAnalysisTest.testSetup = new FolderBasedTestSetup()
                .setSource("file://" + baseDir.getAbsolutePath())
                .setName(NextcloudTest.class.getName());
    }

    @Disabled
    @Test
    public void clear() throws Exception {
        Assertions.assertTrue(AbstractCompositionAnalysisTest.testSetup.clear());

    }

    @Disabled
    @Test
    public void analyse() throws Exception {

        // FIXME: runs extremely slow; needs to be optimized
        Assertions.assertTrue(AbstractCompositionAnalysisTest.testSetup.rebuildInventory());
    }

    @Test
    public void testContainerStructure() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        final Analysis analysis = new Analysis(inventory);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "generic-version")).hasSizeOf(1);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "exe")).hasSizeOf(0);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "nextcloud-app")).hasSizeOf(52);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "dpkg")).hasSizeOf(276);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "php-composer")).hasSizeOf(91);

        // there must be only once container asset
        analysis.selectAssets(CONTAINER_ASSET_PREDICATE).hasSizeOf(1);

        // we expect the container being only represented as asset; no artifacts with type container
        analysis.selectArtifacts(containsToken(TYPE, "container")).hasSizeOf(0);
    }

    @Test
    public void testComponentPatterns() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        final Inventory referenceInventory = AbstractCompositionAnalysisTest.testSetup.readReferenceInventory();
        final File baseDir = new File(AbstractCompositionAnalysisTest.testSetup.getScanFolder());
        List<FilePatternQualifierMapper> filePatternQualifierMapperList =
                evaluateComponentPatterns(referenceInventory, inventory, baseDir);
        DuplicateList duplicateList = new DuplicateList(filePatternQualifierMapperList);

        duplicateList.identifyRemainingDuplicatesWithoutArtifact("nextcloud-nextcloud-1.0.0-1.0.0", "nextcloud-nextcloud/3rdparty-dev-master-dev-master", "External storage support-files_external-1.22.0");

        // FIXME: os-release is a duplicate in the container
        Assertions.assertEquals(1, duplicateList.getRemainingDuplicates().size());
        Assertions.assertFalse(duplicateList.getFileWithoutDuplicates().isEmpty());
    }
}
