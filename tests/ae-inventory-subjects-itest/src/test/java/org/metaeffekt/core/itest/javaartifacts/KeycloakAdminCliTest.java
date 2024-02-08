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
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.metaeffekt.core.itest.inventory.Analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.itest.inventory.dsl.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.IdStartsWith.idStartsWith;

public class KeycloakAdminCliTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://repo1.maven.org/maven2/org/keycloak/keycloak-admin-cli/23.0.1/keycloak-admin-cli-23.0.1.jar")
                .setName(KeycloakAdminCliTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(preparer.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(preparer.rebuildInventory());
    }

    @Test
    public void first() throws Exception {
        LOG.info("Inventory of size {}.", preparer.getInventory().getArtifacts().size());
    }

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory = preparer.getInventory();

        inventory.getArtifacts().stream().map(a -> a.deriveQualifier()).forEach(LOG::info);

        Analysis analysis = new Analysis(inventory);

        // expect that the substructure is visible
        analysis.selectArtifacts().hasSizeGreaterThan(1);
        analysis.selectArtifacts(idStartsWith("jansi")).hasSizeOf(9);
        analysis.selectArtifacts(idStartsWith("commons")).hasSizeOf(2);
        analysis.selectArtifacts(idStartsWith("http")).hasSizeOf(2);
        analysis.selectArtifacts(attributeValue("Id", "keycloak-admin-cli-23.0.1.jar")).hasSizeOf(1);
    }

}







