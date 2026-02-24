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
package org.metaeffekt.core.itest.analysis.archives;

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

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.ID;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.PURL;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.StartsWith.startsWith;

public class KeycloakAdminCliTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeAll
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/keycloak/keycloak-admin-cli/23.0.1/keycloak-admin-cli-23.0.1.jar")
                .setSha256Hash("ee96ab6cd398f6a2e7b34765764dd170bf2a11b412b0d5dfddf5cc4fe482d3ce")
                .setName(KeycloakAdminCliTest.class.getName());
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

        analysis.selectArtifacts().logListWithAllAttributes();

        // expect that the substructure is visible
        analysis.selectArtifacts().hasSizeGreaterThan(1);
        analysis.selectArtifacts(startsWith(ID, "jansi")).hasSizeOf(9);
        analysis.selectArtifacts(startsWith(PURL, "pkg:maven/org.fusesource.jansi/jansi-"));
        analysis.selectArtifacts(startsWith(ID, "commons")).hasSizeOf(2);
        analysis.selectArtifacts(startsWith(ID, "http")).hasSizeOf(2);
        analysis.selectArtifacts(attributeValue(ID, "keycloak-admin-cli-23.0.1.jar")).hasSizeOf(1);
    }

}
