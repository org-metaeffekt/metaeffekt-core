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
package org.metaeffekt.core.itest.analysis.libraries;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class EhCacheTest_3_10_8 extends AbstractCompositionAnalysisTest {

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/ehcache/ehcache/3.10.8/ehcache-3.10.8-jakarta.jar")
                .setSha256Hash("4530ba51c1768f680bffcc5af722f7b65a0abb3874d9f17a731c7085eb2613e7")
                .setName(EhCacheTest_3_10_8.class.getName());
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
        getAnalysisAfterInvariantCheck()
                .selectArtifacts()
                .logListWithAllAttributes()
                .with(attributeValue(ID, "ehcache-3.10.8-jakarta.jar"),
                        attributeValue(CHECKSUM, "6767673b52b5c2157bb6b41daef38963"),
                        attributeValue(VERSION, "3.10.8"))
                .assertNotEmpty();
    }
}
