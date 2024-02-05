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
package org.metaeffekt.core.itest.javaartifacts.apereo;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.metaeffekt.core.itest.inventory.Analysis;
import org.metaeffekt.core.itest.inventory.artifactlist.Matcher;
import org.metaeffekt.core.itest.javaartifacts.TestBasicInvariants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.ID;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.VERSION;
import static org.metaeffekt.core.itest.inventory.artifactlist.Matcher.Cardinality.*;

public class FbmsWarTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://repo1.maven.org/maven2/org/jasig/portal/fbms/fbms-webapp/1.3.1/fbms-webapp-1.3.1.war")
                .setName(FbmsWarTest.class.getName());
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
    public void manifestSuper() throws Exception {
        Analysis template = getTemplate("/apereo/FbmsWarTest/SUPERSET/");
        Matcher matcher = new Matcher()
                .setPrimaryAttribute(ID)
                .setCardinality(SUPERSET)
                .setAttributes("CHECKSUM", VERSION.getKey());
        matcher.match(template, getAnalysis());
        matcher.getListOfMatching().logArtifactListWithAllAtributes();
        matcher.getListOfMissing().logArtifactListWithAllAtributes();
    }

    @Test
    @Ignore // currently deviated by one; test lifecycle issue
    public void manifestEqual() throws Exception {
        Analysis template = getTemplate("/apereo/FbmsWarTest/EQUAL/");
        Matcher matcher = new Matcher()
                .setPrimaryAttribute(ID)
                .setCardinality(EQUAL)
                .setAttributes("CHECKSUM", VERSION.getKey());
        matcher.match(template, getAnalysis());
        matcher.getListOfMatching().logArtifactListWithAllAtributes();
        matcher.getListOfMissing().logArtifactListWithAllAtributes();
    }

    @Test
    public void manifestSubset() throws Exception {
        Analysis template = getTemplate("/apereo/FbmsWarTest/SUBSET/");
        Matcher matcher = new Matcher()
                .setPrimaryAttribute(ID)
                .setCardinality(SUBSET)
                .setAttributes("CHECKSUM", VERSION.getKey());
        matcher.match(template, getAnalysis());
        matcher.getListOfMatching().logArtifactListWithAllAtributes();
        matcher.getListOfMissing().logArtifactListWithAllAtributes();
    }

}
