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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.PATH_IN_ASSET;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class SystemIoFilesystemAccesscontrol_5_0_0 extends AbstractCompositionAnalysisTest{

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://globalcdn.nuget.org/packages/system.io.filesystem.accesscontrol.5.0.0.nupkg")
                .setSha256Hash("73d3250ca25f8fadd846f97b6eb44136ce7da25ae66cbf86d40ea84d2f32b447")
                .setName(SystemIoFilesystemAccesscontrol_5_0_0.class.getName());
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
        ArtifactList artifactList = getAnalysisAfterInvariantCheck().selectArtifacts();

        artifactList.logListWithAllAttributes();

        artifactList.with(attributeValue(ID, "System.IO.FileSystem.AccessControl-5.0.0"),
                        attributeValue(VERSION, "5.0.0"),
                        attributeValue(Artifact.Attribute.ROOT_PATHS, "system.io.filesystem.accesscontrol.5.0.0.nupkg"),
                        attributeValue(PURL, "pkg:nuget/System.IO.FileSystem.AccessControl@5.0.0"),
                        attributeValue(PATH_IN_ASSET, "system.io.filesystem.accesscontrol.5.0.0.nupkg"))
                .assertNotEmpty();
    }

}
