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
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.container.ContainerDumpSetup.saveContainerFromRegistryByRepositoryAndTag;

public class KeycloakTest_25_0_4Test extends AbstractCompositionAnalysisTest {

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
                "quay.io/keycloak",
                "keycloak",
                "25.0.4",
                KeycloakTest_25_0_4Test.class.getName());

        AbstractCompositionAnalysisTest.testSetup = new FolderBasedTestSetup()
                .setSource("file://" + baseDir.getAbsolutePath())
                .setName(KeycloakTest_25_0_4Test.class.getName());
    }

    @Disabled
    @Test
    public void clear() throws Exception {
        Assertions.assertTrue(AbstractCompositionAnalysisTest.testSetup.clear());
    }

    @Disabled
    @Test
    public void analyse() throws Exception {
        Assertions.assertTrue(AbstractCompositionAnalysisTest.testSetup.rebuildInventory());
    }

    @Test
    public void testComponentPatterns() throws Exception {
        final Inventory inventory = testSetup.getInventory();
        final Inventory referenceInventory = testSetup.readReferenceInventory();
        final File baseDir = new File(testSetup.getScanFolder());
        final List<FilePatternQualifierMapper> filePatternQualifierMapperList =
                evaluateComponentPatterns(referenceInventory, inventory, baseDir);
        final DuplicateList duplicateList = new DuplicateList(filePatternQualifierMapperList);

        duplicateList.identifyRemainingDuplicatesWithoutArtifact();

        // FIXME: cfg, md, so files are being used by several artifacts
        Assertions.assertEquals(97, duplicateList.getRemainingDuplicates().size());
        Assertions.assertFalse(duplicateList.getFileWithoutDuplicates().isEmpty());
    }

    @Test
    public void testContainerStructure() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();

        final Analysis analysis = new Analysis(inventory);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "generic-version")).hasSizeOf(1);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "jar-module")).hasSizeOf(404);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "rpm")).hasSizeOf(43);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "linux-distro")).hasSizeOf(1);

        // check how many artifacts have no version (harness unmapped files)
        analysis.selectArtifacts(attributeValue(VERSION, null)).hasSizeOf(51);

        // there must be only once container asset
        analysis.selectAssets(CONTAINER_ASSET_PREDICATE).hasSizeOf(1);

        // we expect the container being only represented as asset; no artifacts with type container
        analysis.selectArtifacts(containsToken(TYPE, "container")).hasSizeOf(0);
    }

}
