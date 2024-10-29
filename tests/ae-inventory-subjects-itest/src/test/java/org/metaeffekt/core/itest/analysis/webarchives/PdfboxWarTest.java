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
package org.metaeffekt.core.itest.analysis.webarchives;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;
import static org.metaeffekt.core.itest.common.predicates.StartsWith.startsWith;

public class PdfboxWarTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox-war/1.8.17/pdfbox-war-1.8.17.war")
                .setSha256Hash("598dc00130fb7bef4566dd73f26763ed1eb79daa63803fbae4f2ec5bb8b0a94b")
                .setName(PdfboxWarTest.class.getName());
    }


    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(testSetup.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(testSetup.rebuildInventory());
    }

    @Test
    public void assertContent() throws Exception {
        final Inventory inventory = testSetup.getInventory();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts(startsWith(ID, "pdfbox")).hasSizeOf(2);
        analysis.selectArtifacts(startsWith(ID, "fontbox")).hasSizeOf(1);
        analysis.selectArtifacts(startsWith(ID, "commons")).hasSizeOf(1);
        analysis.selectArtifacts(startsWith(ID, "jempbox")).hasSizeOf(1);

        analysis.selectArtifacts(startsWith(PURL, "pkg:maven/org.apache.pdfbox/")).hasSizeOf(4);


        analysis.selectArtifacts(attributeValue(GROUPID, "org.apache.pdfbox")).hasSizeOf(4);
        analysis.selectArtifacts(attributeValue(GROUPID, "commons-logging")).hasSizeOf(1);

        analysis.selectArtifacts(attributeValue(VERSION, "1.8.17")).hasSizeOf(4);
        analysis.selectArtifacts(attributeValue(VERSION, "1.1.1")).hasSizeOf(1);

        ArtifactList artifactList = getAnalysisAfterInvariantCheck().selectArtifacts();

        artifactList.logListWithAllAttributes();

        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "jar-module")).hasSizeOf(artifactList.size());
        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "jar-module")).hasSizeOf(5);
    }

}
