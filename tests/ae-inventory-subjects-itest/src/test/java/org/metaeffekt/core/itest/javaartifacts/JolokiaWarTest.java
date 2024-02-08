/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.itest.javaartifacts;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.itest.inventory.Analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.GROUPID;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.VERSION;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.IdStartsWith.idStartsWith;

public class JolokiaWarTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/jolokia/jolokia-war/1.7.2/jolokia-war-1.7.2.war")
                .setName(JolokiaWarTest.class.getName());
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
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory = testSetup.getInventory();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts(idStartsWith("jolokia")).hasSizeOf(4);
        analysis.selectArtifacts(idStartsWith("json")).hasSizeOf(2);

        analysis.selectArtifacts(attributeValue(GROUPID, "org.jolokia")).hasSizeOf(4);
        analysis.selectArtifacts(attributeValue(GROUPID, "com.googlecode.json-simple")).hasSizeOf(1);

        analysis.selectArtifacts(attributeValue(VERSION, "1.7.2")).hasSizeOf(4);
        analysis.selectArtifacts(attributeValue(VERSION, "1.1.1")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(VERSION, "$JSON_JMX_AGENT_VERSION")).hasSizeOf(1);
    }

}
