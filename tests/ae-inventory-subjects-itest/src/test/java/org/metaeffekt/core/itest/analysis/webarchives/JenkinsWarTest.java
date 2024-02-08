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
package org.metaeffekt.core.itest.analysis.webarchives;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.itest.common.Analysis;
import org.metaeffekt.core.itest.common.setup.AbstractBasicInvariantsTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.VERSION;
import static org.metaeffekt.core.itest.common.predicates.AttributeExists.withAttribute;
import static org.metaeffekt.core.itest.common.predicates.IdMissmatchesVersion.idMismatchesVersion;
import static org.metaeffekt.core.itest.common.predicates.Not.not;
import static org.metaeffekt.core.itest.common.predicates.BooleanPredicate.alwaysTrue;
import static org.metaeffekt.core.itest.common.predicates.BooleanPredicate.alwaysFalse;

public class JenkinsWarTest extends AbstractBasicInvariantsTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setSha256Hash("8d84f3cdd6430c098d1f4f38740957e3f2d0ac261b2f9c68cbf9c306363fd1c8")
                .setName(JenkinsWarTest.class.getName());
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


    //TODO
    @Ignore
    @Test
    public void typesMustBeSetPredicate() {
        getAnalysis()
                .selectArtifacts(not(withAttribute(TYPE)))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void noErrorsExist() {
        getAnalysisAfterInvariantCheck()
                .selectArtifacts(withAttribute("Errors"))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void versionMismatch() {
        getAnalysis()
                .selectArtifacts(withAttribute(VERSION))
                .assertNotEmpty()
                .logArtifactList()
                .assertEmpty(idMismatchesVersion());
    }

    @Ignore
    @Test
    public void testPredicatePrimitives() {
        getAnalysis()
                .selectArtifacts()
                .assertEmpty(alwaysFalse)
                .assertNotEmpty(alwaysTrue)
                .logArtifactListWithAllAtributes()
                .logInfo()
                .logInfo("Typed List:")
                .filter(withAttribute(TYPE))
                .as("Artifact has Type")
                .assertNotEmpty()
                .logArtifactListWithAllAtributes();

    }

    @Ignore
    @Test
    public void namedLists() {
        getAnalysis()
                .selectArtifacts()
                .logInfo("List with a Name:")
                .filter(withAttribute(TYPE)).as("Artifact has Type")
                .assertEmpty();

    }

    @Ignore
    @Test
    public void checkInvariants() {
        getAnalysisAfterInvariantCheck();

    }

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory = testSetup.getInventory();

        inventory.getArtifacts().stream().map(Artifact::deriveQualifier).forEach(LOG::info);

        Analysis analysis = new Analysis(inventory);


    }


}
