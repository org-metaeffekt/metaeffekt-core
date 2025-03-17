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
package org.metaeffekt.core.itest.analysis.wheels;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class Boto3_1_34_153 extends AbstractCompositionAnalysisTest{

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://files.pythonhosted.org/packages/35/31/fdb71109d828e8e7da49db2d2ead5d432e8d8cf20f3f6e5d035d9ba0c3f5/boto3-1.34.153-py3-none-any.whl")
                .setSha256Hash("ff9af9206fb235605cb65922f9090fe60f78ea89b4adc463f8f6391b30a3df03")
                .setName(Boto3_1_34_153.class.getName());
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

        artifactList.with(attributeValue(ID, "boto3-1.34.153"),
                        attributeValue(VERSION, "1.34.153"),
                        attributeValue(Artifact.Attribute.ROOT_PATHS, "boto3-1.34.153-py3-none-any.whl"),
                        attributeValue(PATH_IN_ASSET, "boto3-1.34.153-py3-none-any.whl"))
                .assertNotEmpty();
    }
}
