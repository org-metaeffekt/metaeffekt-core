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

import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.metaeffekt.core.itest.inventory.dsl.predicates.attributes.Exists;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.inventory.dsl.predicates.IdMissmatchesVersion;
import org.metaeffekt.core.itest.inventory.dsl.predicates.Not;
import org.metaeffekt.core.itest.inventory.dsl.predicates.TrivialPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.itest.inventory.dsl.predicates.IdMissmatchesVersion.idMismatchingVersion;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.Not.not;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.TrivialPredicates.trivialReturnAllElements;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.TrivialPredicates.trivialReturnNoElements;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.attributes.Exists.withAttribute;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;

public class JenkinsTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setName(JenkinsTest.class.getName());
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
        getAnalysisAfterInvariants()
                .selectArtifacts(withAttribute("Errors"))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void versionMissmatch() {
        getAnalysis()
                .selectArtifacts(withAttribute(VERSION))
                .assertNotEmpty()
                .logArtifactList()
                .assertEmpty(idMismatchingVersion);
    }

    @Ignore
    @Test
    public void trivialPredicates() {
        getAnalysis()
                .selectArtifacts()
                .assertEmpty(trivialReturnNoElements)
                .assertNotEmpty(trivialReturnAllElements)
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

}
