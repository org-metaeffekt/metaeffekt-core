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
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.ComponentPatternList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.container.ContainerDumpSetup.exportContainerFromRegistryByRepositoryAndTag;

public class MatrixElementTest extends AbstractCompositionAnalysisTest {

    private static final Logger LOG = LoggerFactory.getLogger(MatrixElementTest.class);

    @BeforeClass
    public static void prepare() throws IOException, InterruptedException, NoSuchAlgorithmException {
        String path = exportContainerFromRegistryByRepositoryAndTag(null, "avhost/docker-matrix-element", null, MatrixElementTest.class.getName());
        String sha256Hash = FileUtils.computeSHA256Hash(new File(path));
        AbstractCompositionAnalysisTest.testSetup = new UrlBasedTestSetup()
                .setSource("file://" + path)
                .setSha256Hash(sha256Hash)
                .setName(MatrixElementTest.class.getName());
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
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "pwa-module")).hasSizeOf(1);
        analysis.selectArtifacts(containsToken(COMPONENT_SOURCE_TYPE, "npm-module")).hasSizeOf(48);
        analysis.selectArtifacts(containsToken(ID, "package-lock.json")).assertEmpty();
    }

    @Test
    public void testComponentPatterns() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();

        Analysis analysis = new Analysis(inventory);
        ComponentPatternList componentPatternList = analysis.selectComponentPatterns();
        List<ComponentPatternData> componentPatternDataList = componentPatternList.getItemList();

        ComponentPatternList filteredComponentPatternList = componentPatternList.filter(containsToken(VERSION_ANCHOR, "manifest.json"));
        List<ComponentPatternData> filteredComponentPatternDataList = filteredComponentPatternList.getItemList();
        ComponentPatternData componentPatternData = filteredComponentPatternDataList.get(0);
        String anchorPath = componentPatternData.get(VERSION_ANCHOR);

        // check if the list contains duplicates
        // Assert.assertEquals(componentPatternDataList.size(), componentPatternDataList.stream().distinct().count());

        HashMap<String, List<String>> artifactToIncludePattern = new HashMap<>();
        /*for (ComponentPatternData componentPatternData : componentPatternDataList) {
            String componentName = componentPatternData.get(COMPONENT_NAME);
            String includePattern = componentPatternData.get(INCLUDE_PATTERN);
            List<String> includePatternList = Arrays.asList(includePattern.split(","));
            artifactToIncludePattern.put(componentName, includePatternList);
        }*/

        // check for duplicate include patterns
        checkForDuplicateIncludePatterns(artifactToIncludePattern);
    }

    private void checkForDuplicateIncludePatterns(HashMap<String, List<String>> artifactToIncludePattern) {
        HashMap<String, String> patternToArtifact = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : artifactToIncludePattern.entrySet()) {
            String componentName = entry.getKey();
            List<String> includePatterns = entry.getValue();

            for (String pattern : includePatterns) {
                if (patternToArtifact.containsKey(pattern)) {
                    String otherComponent = patternToArtifact.get(pattern);
                    LOG.warn("Duplicate include pattern found between [{}] and [{}] for pattern [{}]", otherComponent, componentName, pattern);
                    Assert.fail();
                } else {
                    patternToArtifact.put(pattern, componentName);
                }
            }
        }
    }
}
