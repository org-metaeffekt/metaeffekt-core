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
package org.metaeffekt.core.itest.analysis.libraries;

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

public class Charts_4_1_0 extends AbstractCompositionAnalysisTest {

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://github.com/ChartsOrg/Charts/archive/refs/tags/v4.1.0.zip")
                .setSha256Hash("24f8838d1b2a222d91b37a4f67f4b0ffcfd7a9cf638a4aa3817fd7ffecd630e7")
                .setName(Charts_4_1_0.class.getName());
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
        getAnalysisAfterInvariantCheck()
                .selectArtifacts()
                .logListWithAllAttributes()
                .with(attributeValue(ID, "Charts-4.1.0"),
                        attributeValue(VERSION, "4.1.0"),
                        attributeValue(ROOT_PATHS, "[v4.1.0.zip]/Charts-4.1.0"),
                        attributeValue(PURL, "pkg:cocoapods/Charts@4.1.0"),
                        attributeValue(PATH_IN_ASSET, "[v4.1.0.zip]/Charts-4.1.0"))
                .assertNotEmpty();

        ArtifactList artifactList = getAnalysisAfterInvariantCheck()
                .selectArtifacts()
                .filter(a -> a.getVersion() != null);

        artifactList.logListWithAllAttributes();

        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "cocoapods")).hasSizeOf(artifactList.size());
        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "cocoapods")).hasSizeOf(1);
    }
}
