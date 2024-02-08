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
package org.metaeffekt.core.itest.javaartifacts.spring;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.itest.common.setup.AbstractBasicInvariantsTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class SpringCore6_1_1Test extends AbstractBasicInvariantsTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/springframework/spring-core/6.1.1/spring-core-6.1.1.jar")
                .setName(SpringCore6_1_1Test.class.getName());
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
    public void first() throws Exception {
        getAnalysisAfterInvariantCheck()
                .selectArtifacts()
                .logArtifactListWithAllAtributes()
                .with(attributeValue(ID, "spring-core-6.1.1.jar"),
                        attributeValue(CHECKSUM, "7a787700b8de9fc9034ffdc070517f51"),
                        attributeValue(VERSION, "6.1.1"),
                        attributeValue("Hash (SHA-256)", "a2ef6992edc54d3380ba95c56d86d1baf64afb0eda9296518be21a483318d93f"),
                        attributeValue(PROJECTS,"spring-core-6.1.1.jar"),
                        attributeValue("Path in Asset", "spring-core-6.1.1.jar")
                )
                .assertNotEmpty();
    }
}
