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
package org.metaeffekt.core.itest.container;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
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

public class CoreUIMiddlewareTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        // TODO: this has to be changed with the actual source url
        AbstractCompositionAnalysisTest.testSetup = new UrlBasedTestSetup()
                .setSource("http://ae-scanner/images/CID-core-ui-middleware%408082edf30498a3ac1715f2d9b3e406f240ea586e2616b97f40c207ef55dff11f-export.tar")
                .setSha256Hash("c0e44a39a8dfd6d839a1f82c8665cd249c4c557794fce1f33fe2d45b8a0621e0")
                .setName(CoreUIMiddlewareTest.class.getName());
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
                .selectArtifacts(not(withAttribute(TYPE)))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void noErrorsExist() {
        getAnalysisAfterInvariantCheck()
                .selectArtifacts(withAttribute(ERRORS))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void versionMismatch() {
        getAnalysis()
                .selectArtifacts(withAttribute(VERSION))
                .assertNotEmpty()
                .logList()
                .assertEmpty(idMismatchesVersion());
    }

    @Ignore
    @Test
    public void testPredicatePrimitives() {
        getAnalysis()
                .selectArtifacts()
                .assertEmpty(alwaysFalse())
                .assertNotEmpty(alwaysTrue())
                .logListWithAllAttributes()
                .logInfo()
                .logInfo("Typed List:")
                .filter(withAttribute(TYPE))
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
                .filter(withAttribute(TYPE)).as("Artifact has Type")
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
        analysis.selectArtifacts(tokenStartsWith(ID, "jenkins", ",")).hasSizeOf(6);
        analysis.selectArtifacts(tokenStartsWith(ID, "spring")).hasSizeOf(9);
        analysis.selectArtifacts(tokenStartsWith(ID, "jakarta")).hasSizeOf(5);
        analysis.selectArtifacts(tokenStartsWith(ID, "javax")).hasSizeOf(2);
    }
}