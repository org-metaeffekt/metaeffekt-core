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
package org.metaeffekt.core.itest.analysis.webarchives;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.StartsWith.startsWith;

public class JolokiaWarTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeAll
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/jolokia/jolokia-war/1.7.2/jolokia-war-1.7.2.war")
                .setSha256Hash("2d5c7fbd9791b012edb5e83f3d161b5c273f0f88f72ff0a1ec63eeae3f78b419")
                .setName(JolokiaWarTest.class.getName());
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
        final Inventory inventory = testSetup.getInventory();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts(startsWith(ID, "jolokia")).hasSizeOf(3);
        analysis.selectArtifacts(startsWith(ID, "json")).hasSizeOf(2);
        analysis.selectArtifacts(startsWith(PURL, "pkg:maven/org.jolokia")).hasSizeOf(3);

        analysis.selectArtifacts(attributeValue(GROUPID, "org.jolokia")).hasSizeOf(3);
        analysis.selectArtifacts(attributeValue(GROUPID, "com.googlecode.json-simple")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "1.7.2")).hasSizeOf(3);
        analysis.selectArtifacts(attributeValue(VERSION, "1.1.1")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "$JSON_JMX_AGENT_VERSION")).hasSizeOf(1);
    }

}
