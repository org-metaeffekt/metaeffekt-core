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

public class SystemSecurityAccessescontrol_6_0_1 extends AbstractCompositionAnalysisTest{

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://globalcdn.nuget.org/packages/system.security.accesscontrol.6.0.1.nupkg")
                .setSha256Hash("462e26f796417e8a4350655a85bbd4233b94cbd0f7150c1c5e9acfd88e885141")
                .setName(SystemSecurityAccessescontrol_6_0_1.class.getName());
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

        artifactList.with(attributeValue(ID, "system.security.accesscontrol.6.0.1.nupkg"),
                        attributeValue(CHECKSUM, "bd9b2506dd7bfc858d39fee988187094"),
                        attributeValue(HASH_SHA256, "462e26f796417e8a4350655a85bbd4233b94cbd0f7150c1c5e9acfd88e885141"),
                        attributeValue(PROJECTS, "system.security.accesscontrol.6.0.1.nupkg"),
                        attributeValue(PATH_IN_ASSET, "system.security.accesscontrol.6.0.1.nupkg"))
                .assertNotEmpty();
    }
}