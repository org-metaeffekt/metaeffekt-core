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

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.configuration.DirectoryScanAggregatorConfiguration;
import org.metaeffekt.core.inventory.processor.model.Artifact;
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
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.filescan.ComponentPatternValidator.evaluateComponentPatterns;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.VERSION_ANCHOR;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.container.ContainerDumpSetup.saveContainerFromRegistryByRepositoryAndTag;

public class PythonContainerTest extends AbstractCompositionAnalysisTest {

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
    public static void prepare() throws IOException, InterruptedException, NoSuchAlgorithmException {
        final File baseDir = saveContainerFromRegistryByRepositoryAndTag(
                null,
                "python",
                "3.13.1-alpine@sha256:f9d772b2b40910ee8de2ac2b15ff740b5f26b37fc811f6ada28fce71a2542b0e",
                PythonContainerTest.class.getName());

        AbstractCompositionAnalysisTest.testSetup = new FolderBasedTestSetup()
                .setSource("file://" + baseDir.getAbsolutePath())
                .setName(PythonContainerTest.class.getName());
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

        final List<FilePatternQualifierMapper> filePatternQualifierMapperList =
                evaluateComponentPatterns(referenceInventory, inventory, baseDir);

        final DuplicateList duplicateList = new DuplicateList(filePatternQualifierMapperList);

        duplicateList.identifyRemainingDuplicatesWithoutArtifact();

        Assertions.assertThat(duplicateList.getRemainingDuplicates()).as("remaining duplicates must be empty").isEmpty();

        // FIXME-AOE: what is the intention?; why does the current implementation not align with this?
//        Assertions.assertThat(duplicateList.getFileWithoutDuplicates()).as("files w/o duplicates must not be empty").isNotEmpty();
    }

    @Test
    public void testContainerStructure() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();

        final Analysis analysis = new Analysis(inventory);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "apk")).hasSizeOf(28);

        analysis.selectComponentPatterns(containsToken(VERSION_ANCHOR, "/installed")).hasSizeGreaterThan(1);

        // check how many artifacts have no version (harness unmapped files)
        // FIXME-AOE: something is wrong with the extraction from this container; too many unmapped filed
        analysis.selectArtifacts(attributeValue(VERSION, null)).hasSizeOf(1038);

        // there must be only once container asset
        analysis.selectAssets(CONTAINER_ASSET_PREDICATE).hasSizeOf(1);

        // we expect the container being only represented as asset; no artifacts with type container
        analysis.selectArtifacts(containsToken(TYPE, "container")).hasSizeOf(0);
    }

    @Test
    public void testAggregation() throws Exception {
        final File aggregationDir = new File(testSetup.getInventoryFolder(), "aggregation");

        DirectoryScanAggregatorConfiguration config =
                new DirectoryScanAggregatorConfiguration(testSetup.readReferenceInventory(),
                        testSetup.getInventory(), new File(testSetup.getScanFolder()));

        config.aggregateFiles(aggregationDir);

        final List<Artifact> apk = testSetup.getInventory().getArtifacts();

        final List<Artifact> collect = apk.stream().filter(a -> a.get(COMPONENT_SOURCE_TYPE) != null
                && a.get(COMPONENT_SOURCE_TYPE).contains("apk"))
                .collect(Collectors.toList());

        Assertions.assertThat(collect.size()).isEqualTo(28);

    }

}