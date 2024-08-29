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
package org.metaeffekt.core.itest.analysis.webarchives;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
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

public class FbmsWarTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/jasig/portal/fbms/fbms-webapp/1.3.1/fbms-webapp-1.3.1.war")
                .setSha256Hash("ffa47b63d97fc354805de6d8320c3b5929f37c15a1ed90d0d88e906f958ff72f")
                .setName(FbmsWarTest.class.getName());
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
        final Inventory inventory = testSetup.getInventory();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts(startsWith(ID, "jackson")).hasSizeOf(6);
        analysis.selectArtifacts(attributeValue(GROUPID, "com.fasterxml.jackson.core")).hasSizeOf(3);
        analysis.selectArtifacts(attributeValue(GROUPID, "com.fasterxml.jackson.datatype")).hasSizeOf(2);
        analysis.selectArtifacts(attributeValue(GROUPID, "com.fasterxml.jackson.module")).hasSizeOf(1);
        analysis.selectArtifacts(startsWith(PURL, "pkg:maven/com.fasterxml.jackson.core")).hasSizeOf(3);

        analysis.selectArtifacts(startsWith(ID, "spring-")).hasSizeOf(34);
        analysis.selectArtifacts(attributeValue(GROUPID, "org.springframework.data")).hasSizeOf(2);
        analysis.selectArtifacts(attributeValue(GROUPID, "org.springframework.hateoas")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue(GROUPID, "org.springframework.plugin")).hasSizeOf(2);
        analysis.selectArtifacts(startsWith(PURL, "pkg:maven/org.springframework")).hasSizeOf(5);

        analysis.selectArtifacts(startsWith(ID, "hibernate")).hasSizeOf(4);
        analysis.selectArtifacts(attributeValue(GROUPID, "org.hibernate.validator")).hasSizeOf(1);
        analysis.selectArtifacts(startsWith(PURL, "pkg:maven/org.hibernate.validator/hibernate-validator@6.0.10.Final?type=jar")).hasSizeOf(1);

        analysis.selectArtifacts(startsWith(ID, "log")).hasSizeOf(4);
        analysis.selectArtifacts(attributeValue(GROUPID, "org.apache.logging.log4j")).hasSizeOf(2);
        analysis.selectArtifacts(attributeValue(GROUPID, "ch.qos.logback")).hasSizeOf(2);
        analysis.selectArtifacts(startsWith(PURL, "pkg:maven/org.apache.logging.log4j")).hasSizeOf(2);
        analysis.selectArtifacts(startsWith(PURL, "pkg:maven/ch")).hasSizeOf(2);

        analysis.selectArtifacts(startsWith(ID, "springfox")).hasSizeOf(7);
        analysis.selectArtifacts(attributeValue(VERSION, "2.8.0")).hasSizeOf(7);

        analysis.selectArtifacts(startsWith(ID, "tomcat")).hasSizeOf(3);
        analysis.selectArtifacts(attributeValue(VERSION, "8.5.31")).hasSizeOf(3);

    }

}
