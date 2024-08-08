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
import org.metaeffekt.core.itest.common.fluent.ComponentPatternList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeExists.withAttribute;
import static org.metaeffekt.core.itest.common.predicates.BooleanPredicate.alwaysFalse;
import static org.metaeffekt.core.itest.common.predicates.BooleanPredicate.alwaysTrue;
import static org.metaeffekt.core.itest.common.predicates.IdMismatchesVersion.idMismatchesVersion;
import static org.metaeffekt.core.itest.common.predicates.Not.not;
import static org.metaeffekt.core.itest.common.predicates.TokenStartsWith.getTokenAtPosition;
import static org.metaeffekt.core.itest.common.predicates.TokenStartsWith.tokenStartsWith;

public class JenkinsWarTest extends AbstractCompositionAnalysisTest {

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

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory = testSetup.getInventory();
        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logListWithAllAttributes();
        List<String> prefixes = new ArrayList<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            prefixes.add(getTokenAtPosition(artifact.get(ID), 0));
        }
        prefixes = prefixes.stream().distinct().collect(Collectors.toList());
        LOG.info("Prefixes: {}", prefixes);

        analysis.selectArtifacts().hasSizeGreaterThan(1);

        // FIXME: a jar is created from the war; this needs to be resolved
        analysis.selectArtifacts(tokenStartsWith(ID, "jenkins", ",")).hasSizeOf(5 + 1);

        analysis.selectArtifacts(tokenStartsWith(ID, "spring")).hasSizeOf(9);
        analysis.selectArtifacts(tokenStartsWith(ID, "jakarta")).hasSizeOf(5);
        analysis.selectArtifacts(tokenStartsWith(ID, "javax")).hasSizeOf(2);
    }

    @Test
    public void testCompositionAsset() throws Exception {
        final Inventory inventory = testSetup.getInventory();
        Analysis analysis = new Analysis(inventory);
        analysis.selectAssets().hasSizeGreaterThan(1);
        analysis.selectAssets().logListWithAllAttributes();
    }

    @Test
    public void testCompositionComponentPattern() throws Exception {
        final Inventory inventory = testSetup.getInventory();

        Analysis analysis = new Analysis(inventory);
        analysis.selectComponentPatterns().hasSizeGreaterThan(1);
        ComponentPatternList componentPatternList = analysis.selectComponentPatterns();
        componentPatternList.logListWithAllAttributes();
    }
}
