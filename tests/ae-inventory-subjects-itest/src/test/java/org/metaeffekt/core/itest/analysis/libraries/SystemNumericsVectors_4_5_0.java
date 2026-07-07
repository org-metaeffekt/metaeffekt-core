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
package org.metaeffekt.core.itest.analysis.libraries;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class SystemNumericsVectors_4_5_0 extends AbstractCompositionAnalysisTest{

    @BeforeAll
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://globalcdn.nuget.org/packages/system.numerics.vectors.4.5.0.nupkg")
                .setSha256Hash("a9d49320581fda1b4f4be6212c68c01a22cdf228026099c20a8eabefcf90f9cf")
                .setName(SystemNumericsVectors_4_5_0.class.getName());
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

        artifactList.with(attributeValue(ID, "System.Numerics.Vectors-4.5.0"),
                        attributeValue(VERSION, "4.5.0"),
                        attributeValue(Artifact.Attribute.ROOT_PATHS, "system.numerics.vectors.4.5.0.nupkg"),
                        attributeValue(PURL, "pkg:nuget/System.Numerics.Vectors@4.5.0"),
                        attributeValue(PATH_IN_ASSET, "system.numerics.vectors.4.5.0.nupkg"))
                .assertNotEmpty();
    }
}
