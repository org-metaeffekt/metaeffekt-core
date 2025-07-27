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
package org.metaeffekt.core.itest.analysis.gems;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;

public class Mime_Types_Data_3_2024_0702 extends AbstractCompositionAnalysisTest{

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://rubygems.org/downloads/mime-types-data-3.2024.0702.gem")
                .setSha256Hash("205b39d4d4428712b39ad70eeb2b4c51694dee973c916df773892a89e93b9980")
                .setName(Mime_Types_Data_3_2024_0702.class.getName());
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
        ArtifactList artifactList = getAnalysisAfterInvariantCheck()
                .selectArtifacts();

        artifactList.logListWithAllAttributes();

        artifactList.with(attributeValue(ID, "mime-types-data-3.2024.0702.gem"),
                        attributeValue(VERSION, "3.2024.0702"),
                        attributeValue(PURL, "pkg:gem/mime-types-data@3.2024.0702"))
                .assertNotEmpty();

        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "ruby-gem-metadata")).hasSizeOf(artifactList.size());
        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "ruby-gem-metadata")).hasSizeOf(1);
    }
}
