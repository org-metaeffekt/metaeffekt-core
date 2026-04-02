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
package org.metaeffekt.core.itest.analysis.gems;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class Aws_Sdk_Resources_3_200_0 extends AbstractCompositionAnalysisTest {

    @BeforeAll
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://rubygems.org/downloads/aws-sdk-resources-3.200.0.gem")
                .setSha256Hash("aa32c2faf80ff5906caf95b958ed56a278e20589c3ab595e1d502f8e0bd14da4")
                .setName(Aws_Sdk_Resources_3_200_0.class.getName());
    }


    @Disabled
    @Test
    public void clear() throws Exception {
        Assertions.assertTrue(testSetup.clear());
    }

    @Disabled
    @Test
    public void inventorize() throws Exception {
        Assertions.assertTrue(testSetup.rebuildInventory());
    }

    @Test
    public void assertContent() throws Exception {
        ArtifactList artifactList = getAnalysisAfterInvariantCheck()
                .selectArtifacts();

        artifactList.logListWithAllAttributes();

        artifactList.with(attributeValue(ID, "aws-sdk-resources-3.200.0.gem"),
                        attributeValue(VERSION, "3.200.0"),
                        attributeValue(PURL, "pkg:gem/aws-sdk-resources@3.200.0"))
                .assertNotEmpty();
    }
}