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

public class Cryptography_43_0_0 extends AbstractCompositionAnalysisTest{

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://files.pythonhosted.org/packages/ca/25/7b53082e4c373127c1fb190f70c5aca7bf7a03ac11f67ba15473bc6d9a0e/cryptography-43.0.0-pp310-pypy310_pp73-win_amd64.whl")
                .setSha256Hash("aae4d918f6b180a8ab8bf6511a419473d107df4dbb4225c7b48c5c9602c38c7f")
                .setName(Cryptography_43_0_0.class.getName());
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

        artifactList.with(attributeValue(ID, "System.Numerics.Vectors-4.5.0"),
                        attributeValue(VERSION, "4.5.0"),
                        attributeValue(PROJECTS, "[system.numerics.vectors.4.5.0.nupkg]"),
                        attributeValue(PATH_IN_ASSET, "[system.numerics.vectors.4.5.0.nupkg]"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "cryptography-43.0.0"),
                        attributeValue(VERSION, "43.0.0"),
                        attributeValue(PROJECTS, "[cryptography-43.0.0-pp310-pypy310_pp73-win_amd64.whl]"),
                        attributeValue(PATH_IN_ASSET, "[cryptography-43.0.0-pp310-pypy310_pp73-win_amd64.whl]"))
                .assertNotEmpty();
    }
}
