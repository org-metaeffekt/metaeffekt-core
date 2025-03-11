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
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.DuplicateList;
import org.metaeffekt.core.itest.common.predicates.NamedBasePredicate;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import static org.metaeffekt.core.inventory.processor.filescan.ComponentPatternValidator.evaluateComponentPatterns;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.COMPONENT_SOURCE_TYPE;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;

public class CoreUIMiddlewareTest extends AbstractCompositionAnalysisTest {

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
        AbstractCompositionAnalysisTest.testSetup = new UrlBasedTestSetup()
                .setSource("http://ae-scanner/images/CID-core-ui-middleware%408082edf30498a3ac1715f2d9b3e406f240ea586e2616b97f40c207ef55dff11f-export.tar")
                .setSha256Hash("c0e44a39a8dfd6d839a1f82c8665cd249c4c557794fce1f33fe2d45b8a0621e0")
                .setName(CoreUIMiddlewareTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(AbstractCompositionAnalysisTest.testSetup.clear());
    }

    @Ignore
    @Test
    public void analyze() throws Exception {
        Assert.assertTrue(AbstractCompositionAnalysisTest.testSetup.rebuildInventory());
    }

    @Test
    public void testComponentPatterns() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        final Inventory referenceInventory = AbstractCompositionAnalysisTest.testSetup.readReferenceInventory();
        final File baseDir = new File(AbstractCompositionAnalysisTest.testSetup.getScanFolder());
        List<FilePatternQualifierMapper> filePatternQualifierMapperList =
                evaluateComponentPatterns(referenceInventory, inventory, baseDir);
        DuplicateList duplicateList = new DuplicateList(filePatternQualifierMapperList);

        // FIXME: these Without functions don't work anymore
        duplicateList.identifyRemainingDuplicatesWithoutFile("os-release");

        Assert.assertEquals(0, duplicateList.getRemainingDuplicates().size());
        Assert.assertFalse(duplicateList.getFileWithoutDuplicates().isEmpty());
    }

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        final Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().hasSizeGreaterThan(1);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "dpkg-distroless")).hasSizeOf(9);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "node-runtime")).hasSizeOf(1);

        // NOTE-KKL: 769 instead of 787; considered removed duplicates or bower overlays
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "npm-module")).hasSizeOf(769);

        // there must be only once container asset
        analysis.selectAssets(CONTAINER_ASSET_PREDICATE).hasSizeOf(0);

        // we expect the container being only represented as asset; no artifacts with type container
        analysis.selectArtifacts(containsToken(TYPE, "container")).hasSizeOf(0);
    }
}