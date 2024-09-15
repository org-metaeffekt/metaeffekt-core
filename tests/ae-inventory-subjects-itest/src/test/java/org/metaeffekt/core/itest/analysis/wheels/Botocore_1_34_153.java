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
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;

public class Botocore_1_34_153 extends AbstractCompositionAnalysisTest{

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://files.pythonhosted.org/packages/93/05/e84db052ba8da6c51ecc9af9461059e77413a4d9903c550e3d319fcb3f7f/botocore-1.34.153-py3-none-any.whl")
                .setSha256Hash("9fc2ad40be8c103ab9bfcb48b97b117d299d0b3a542cdd30134ee2935bee827a")
                .setName(Botocore_1_34_153.class.getName());
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

        artifactList.with(attributeValue(ID, "botocore-1.34.153"),
                        attributeValue(VERSION, "1.34.153"),
                        attributeValue(PROJECTS, "[botocore-1.34.153-py3-none-any.whl]"),
                        attributeValue(PATH_IN_ASSET, "[botocore-1.34.153-py3-none-any.whl]"))
                .assertNotEmpty();

        ArtifactList archiveList = artifactList.with(containsToken(ID, ".whl"));
        archiveList.with(attributeValue(TYPE, "archive")).hasSizeOf(archiveList);
        archiveList.with(attributeValue(COMPONENT_SOURCE_TYPE, "whl-archive")).hasSizeOf(archiveList);

        ArtifactList pythonList = artifactList.with(attributeValue(TYPE, "module"));
        pythonList.with(attributeValue(COMPONENT_SOURCE_TYPE, "python-library")).hasSizeOf(pythonList);
    }
}
