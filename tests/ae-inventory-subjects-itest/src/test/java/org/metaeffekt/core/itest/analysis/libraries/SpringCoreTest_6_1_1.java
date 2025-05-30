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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class SpringCoreTest_6_1_1 extends AbstractCompositionAnalysisTest {

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/springframework/spring-core/6.1.1/spring-core-6.1.1.jar")
                .setSha256Hash("a2ef6992edc54d3380ba95c56d86d1baf64afb0eda9296518be21a483318d93f")
                .setName(SpringCoreTest_6_1_1.class.getName());
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
                .with(attributeValue(ID, "spring-core-6.1.1.jar"),
                        attributeValue(CHECKSUM, "7a787700b8de9fc9034ffdc070517f51"),
                        attributeValue(VERSION, "6.1.1"),
                        attributeValue(HASH_SHA256, "a2ef6992edc54d3380ba95c56d86d1baf64afb0eda9296518be21a483318d93f"),
                        attributeValue(Artifact.Attribute.ROOT_PATHS, "spring-core-6.1.1.jar"),
                        attributeValue(PATH_IN_ASSET, "spring-core-6.1.1.jar"))
                .assertNotEmpty();
    }
}
