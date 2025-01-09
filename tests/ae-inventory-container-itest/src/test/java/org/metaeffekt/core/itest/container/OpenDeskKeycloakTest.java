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
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.DuplicateList;
import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.FolderBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.container.ContainerDumpSetup.saveContainerFromRegistryByRepositoryAndTag;

public class OpenDeskKeycloakTest extends AbstractCompositionAnalysisTest {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

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

    @BeforeClass
    public static void prepare() {
        final File baseDir = saveContainerFromRegistryByRepositoryAndTag("registry.opencode.de",
                "bmi/opendesk/components/supplier/univention/images-mirror/keycloak-keycloak",
                "22.0.3-ucs2@sha256:1e8e45a2e01050c1473595c3b143446363016ea292b0c599ccd9f1bd37112206", OpenDeskKeycloakTest.class.getName());
        AbstractCompositionAnalysisTest.testSetup = new FolderBasedTestSetup()
                .setSource("file://" + baseDir.getAbsolutePath())
                .setSha256Hash("1e8e45a2e01050c1473595c3b143446363016ea292b0c599ccd9f1bd37112206")
                .setName(OpenDeskKeycloakTest.class.getName());
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
    public void testContainerStructure() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        Analysis analysis = new Analysis(inventory);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "dpkg")).hasSizeOf(121);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "jar-module")).hasSizeOf(453);
        analysis.selectArtifacts(containsToken(PURL, "pkg:maven/org.jboss.resteasy/resteasy-client@6.2.5.Final?type=jar")).assertNotEmpty();

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
        List<FilePatternQualifierMapper> filePatternQualifierMapperList = ComponentPatternValidator.evaluateComponentPatterns(referenceInventory, inventory, baseDir);
        DuplicateList duplicateList = new DuplicateList(filePatternQualifierMapperList);
        duplicateList.identifyRemainingDuplicatesWithoutArtifact();
        // FIXME: we have to write a function, which ignores sym links or we have to process them before
        Assert.assertEquals(98, duplicateList.getRemainingDuplicates().size());
        Assert.assertFalse(duplicateList.getFileWithoutDuplicates().isEmpty());
    }
}
