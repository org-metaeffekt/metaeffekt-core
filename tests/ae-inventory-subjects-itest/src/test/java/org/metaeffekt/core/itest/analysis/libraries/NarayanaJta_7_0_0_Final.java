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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;


public class NarayanaJta_7_0_0_Final extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/jboss/narayana/jta/narayana-jta/7.0.0.Final/narayana-jta-7.0.0.Final.jar")
                .setSha256Hash("21e24127dee2582053a07e90c446eab5d290e1e141f9b45f0e1f7c6f30a0151b")
                .setName(NarayanaJta_7_0_0_Final.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(testSetup.clear());

    }

    @Ignore
    @Test
    public void analyse() throws Exception {
        Assert.assertTrue(testSetup.rebuildInventory());
    }

    @Test
    public void assertContent() throws Exception {
        ArtifactList artifactList = getAnalysisAfterInvariantCheck()
                .selectArtifacts()
                .filter(a -> a.getVersion() != null);

        artifactList.logListWithAllAttributes();

        artifactList.with(attributeValue(ID, "arjuna-7.0.0.Final.jar"),
                        attributeValue(VERSION, "7.0.0.Final"),
                        attributeValue(PROJECTS, "[narayana-jta-7.0.0.Final.jar]/META-INF/maven/org.jboss.narayana.arjunacore/arjuna/pom.xml"),
                        attributeValue(PATH_IN_ASSET, "[narayana-jta-7.0.0.Final.jar]/META-INF/maven/org.jboss.narayana.arjunacore/arjuna/pom.xml"))
                .assertNotEmpty();

        artifactList.hasSizeOf(7);

        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "jar-module")).hasSizeOf(artifactList.size());
        artifactList.with(containsToken(COMPONENT_SOURCE_TYPE, "jar-module")).hasSizeOf(7);
    }


}
