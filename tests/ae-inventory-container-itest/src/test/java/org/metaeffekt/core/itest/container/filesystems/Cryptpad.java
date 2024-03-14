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
package org.metaeffekt.core.itest.container.filesystems;

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
import static org.metaeffekt.core.itest.common.predicates.AttributeExists.withAttribute;
import static org.metaeffekt.core.itest.common.predicates.BooleanPredicate.alwaysFalse;
import static org.metaeffekt.core.itest.common.predicates.BooleanPredicate.alwaysTrue;
import static org.metaeffekt.core.itest.common.predicates.IdMismatchesVersion.idMismatchesVersion;
import static org.metaeffekt.core.itest.common.predicates.Not.not;
import static org.metaeffekt.core.itest.common.predicates.TokenStartsWith.tokenStartsWith;

public class Cryptpad extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        AbstractCompositionAnalysisTest.testSetup = new UrlBasedTestSetup()
                .setSource("file:///home/aleyc0re/Dokumente/container-dumps/CID-cryptpad@f4d20d5c38c87b11ed1a1b46ef6a3633d32c6758ebdff8556458f040318fa5e2-export.tar")
                .setSha256Hash("f37a5037210a4afa1cc5badea2c8121f9cd48a192d3e415cc691fe51883d4036")
                .setName(Cryptpad.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(AbstractCompositionAnalysisTest.testSetup.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(AbstractCompositionAnalysisTest.testSetup.rebuildInventory());
    }


    //TODO
    @Ignore
    @Test
    public void typesMustBeSetPredicate() {
        getAnalysis()
                .selectArtifacts(Not.not(AttributeExists.withAttribute(Attribute.TYPE)))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void noErrorsExist() {
        getAnalysisAfterInvariantCheck()
                .selectArtifacts(AttributeExists.withAttribute(Attribute.ERRORS))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void versionMismatch() {
        getAnalysis()
                .selectArtifacts(AttributeExists.withAttribute(Attribute.VERSION))
                .assertNotEmpty()
                .logList()
                .assertEmpty(IdMismatchesVersion.idMismatchesVersion());
    }

    @Ignore
    @Test
    public void testPredicatePrimitives() {
        getAnalysis()
                .selectArtifacts()
                .assertEmpty(BooleanPredicate.alwaysFalse())
                .assertNotEmpty(BooleanPredicate.alwaysTrue())
                .logListWithAllAttributes()
                .logInfo()
                .logInfo("Typed List:")
                .filter(AttributeExists.withAttribute(Attribute.TYPE))
                .as("Artifact has Type")
                .assertNotEmpty()
                .logListWithAllAttributes();

    }

    @Ignore
    @Test
    public void namedLists() {
        getAnalysis()
                .selectArtifacts()
                .logInfo("List with a Name:")
                .filter(AttributeExists.withAttribute(Attribute.TYPE)).as("Artifact has Type")
                .assertEmpty();

    }

    @Ignore
    @Test
    public void checkInvariants() {
        getAnalysisAfterInvariantCheck();

    }

    @Ignore
    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory = AbstractCompositionAnalysisTest.testSetup.getInventory();
        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logListWithAllAttributes();

        analysis.selectArtifacts().hasSizeGreaterThan(1);
        analysis.selectArtifacts(TokenStartsWith.tokenStartsWith(Attribute.ID, "jenkins", ",")).hasSizeOf(6);
        analysis.selectArtifacts(TokenStartsWith.tokenStartsWith(Attribute.ID, "spring")).hasSizeOf(9);
        analysis.selectArtifacts(TokenStartsWith.tokenStartsWith(Attribute.ID, "jakarta")).hasSizeOf(5);
        analysis.selectArtifacts(TokenStartsWith.tokenStartsWith(Attribute.ID, "javax")).hasSizeOf(2);
    }
}
