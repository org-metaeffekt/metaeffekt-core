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

public class RxSwift_6_7_1 extends AbstractCompositionAnalysisTest {

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://github.com/ReactiveX/RxSwift/archive/refs/tags/6.7.1.zip")
                .setSha256Hash("2c242d946246b543004c6b01f0484cb80bb23e1d4ab899bc1f835b6598655979")
                .setName(RxSwift_6_7_1.class.getName());
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
                .selectArtifacts()
                .filter(a -> a.getVersion() != null);

        artifactList.logListWithAllAttributes();

        artifactList.with(attributeValue(ID, "RxTest-6.7.0"),
                    attributeValue(VERSION, "6.7.0"),
                    attributeValue(PROJECTS, "[6.7.1.zip]/RxSwift-6.7.1"),
                    attributeValue(PURL, "pkg:cocoapods/RxTest@6.7.0"),
                    attributeValue(PATH_IN_ASSET, "[6.7.1.zip]/RxSwift-6.7.1"),
                    attributeValue(TYPE, "package"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "RxCocoa-6.7.0"),
                    attributeValue(VERSION, "6.7.0"),
                    attributeValue(PROJECTS, "[6.7.1.zip]/RxSwift-6.7.1"),
                    attributeValue(PURL, "pkg:cocoapods/RxCocoa@6.7.0"),
                    attributeValue(PATH_IN_ASSET, "[6.7.1.zip]/RxSwift-6.7.1"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "RxRelay-6.7.0"),
                    attributeValue(VERSION, "6.7.0"),
                    attributeValue(PROJECTS, "[6.7.1.zip]/RxSwift-6.7.1"),
                    attributeValue(PURL, "pkg:cocoapods/RxRelay@6.7.0"),
                    attributeValue(PATH_IN_ASSET, "[6.7.1.zip]/RxSwift-6.7.1"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "RxSwift-6.7.0"),
                    attributeValue(VERSION, "6.7.0"),
                    attributeValue(PROJECTS, "[6.7.1.zip]/RxSwift-6.7.1"),
                    attributeValue(PURL, "pkg:cocoapods/RxSwift@6.7.0"),
                    attributeValue(PATH_IN_ASSET, "[6.7.1.zip]/RxSwift-6.7.1"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "RxBlocking-6.7.0"),
                    attributeValue(VERSION, "6.7.0"),
                    attributeValue(PROJECTS, "[6.7.1.zip]/RxSwift-6.7.1"),
                    attributeValue(PURL, "pkg:cocoapods/RxBlocking@6.7.0"),
                    attributeValue(PATH_IN_ASSET, "[6.7.1.zip]/RxSwift-6.7.1"))
                .assertNotEmpty();

        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "cocoapods")).hasSizeOf(artifactList.size());
        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "cocoapods")).hasSizeOf(5);
    }
}
