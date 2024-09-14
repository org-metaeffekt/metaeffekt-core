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

public class firebase_veraxai_0_2_2 extends AbstractCompositionAnalysisTest {

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://pub.dev/api/archives/firebase_vertexai-0.2.2%2B2.tar.gz")
                .setSha256Hash("72565c34ec39e196c1779e8235b6d0b8e41999360429c39481ef1f7f34c07a55")
                .setName(firebase_veraxai_0_2_2.class.getName());
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

        artifactList.with(attributeValue(ID, "firebase_vertexai-0.2.2+2"),
                    attributeValue(VERSION, "0.2.2+2"),
                    attributeValue(PROJECTS, "[firebase_vertexai-0.2.2%2B2.tar.gz]/[firebase_vertexai-0.2.2%2B2.tar]"),
                    attributeValue(PURL, "pkg:pub/firebase_vertexai@0.2.2+2"),
                    attributeValue(PATH_IN_ASSET, "[firebase_vertexai-0.2.2%2B2.tar.gz]/[firebase_vertexai-0.2.2%2B2.tar]"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "vertex_ai_example-1.0.0+1"),
                    attributeValue(VERSION, "1.0.0+1"),
                    attributeValue(PROJECTS, "[firebase_vertexai-0.2.2%2B2.tar.gz]/[firebase_vertexai-0.2.2%2B2.tar]/example"),
                    attributeValue(PURL, "pkg:pub/vertex_ai_example@1.0.0+1"),
                    attributeValue(PATH_IN_ASSET, "[firebase_vertexai-0.2.2%2B2.tar.gz]/[firebase_vertexai-0.2.2%2B2.tar]/example"))
                .assertNotEmpty();

        ArtifactList packageList = artifactList.with(attributeValue(TYPE, "package"));
        packageList.with(attributeValue(COMPONENT_SOURCE_TYPE, "pub")).hasSizeOf(packageList);
    }
}
