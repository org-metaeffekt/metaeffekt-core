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

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.HINT_ATOMIC;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;

public class Lombok_1_18_32 extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/projectlombok/lombok/1.18.32/lombok-1.18.32.jar")
                .setSha256Hash("97574674e2a25f567a313736ace00df8787d443de316407d57fc877d9f19a65d")
                .setName(Lombok_1_18_32.class.getName())
                .setReferenceInventory("reference-inventories");
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
                .with(attributeValue(ID, "lombok-1.18.32.jar"),
                        attributeValue(CHECKSUM, "56e9be7b9a26802ac0c784ad824f3a29"),
                        attributeValue(HASH_SHA256, "97574674e2a25f567a313736ace00df8787d443de316407d57fc877d9f19a65d"),
                        attributeValue(ROOT_PATHS, "lombok-1.18.32.jar"),
                        attributeValue(PATH_IN_ASSET, "lombok-1.18.32.jar"))
                .assertNotEmpty();

        ArtifactList artifactList = getAnalysisAfterInvariantCheck()
                .selectArtifacts()
                .filter(a -> a.getVersion() != null);

        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "jar-module")).hasSizeOf(artifactList.size());
        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "jar-module")).hasSizeOf(1);
    }

    @Test
    public void verifyAttributes() throws Exception {
        final Analysis analysis = getAnalysisAfterInvariantCheck();
        analysis.selectArtifacts().hasSizeOf(1);

        // FIXME: what is the better approach here
        final Artifact artifact = analysis.selectArtifacts().getItemList().get(0);

        // it is important, that the classification is not overwritten with 'scan'
        Assertions.assertThat(artifact.getClassification()).isEqualTo(HINT_ATOMIC);
        Assertions.assertThat(artifact.get(ROOT_PATHS)).isEqualTo("lombok-1.18.32.jar");
    }

}
