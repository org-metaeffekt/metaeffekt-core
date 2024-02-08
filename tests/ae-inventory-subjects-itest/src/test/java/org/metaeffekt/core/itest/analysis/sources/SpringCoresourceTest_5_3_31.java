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
package org.metaeffekt.core.itest.analysis.sources;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.itest.common.setup.AbstractBasicInvariantsTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringCoresourceTest_5_3_31 extends AbstractBasicInvariantsTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare(){
        testSetup = new UrlBasedTestSetup()
                .setSource("https://repo1.maven.org/maven2/org/springframework/spring-core/5.3.31/spring-core-5.3.31-sources.jar")
                .setSha256Hash("232263ab0477178ca15bb476f7158dbe61cd6fb2f53b58060f81c2dd0720ba46")
                .setName(SpringCoresourceTest_5_3_31.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception{
        Assert.assertTrue(testSetup.clear());

    }
    @Ignore
    @Test
    public void inventorize() throws Exception{
        Assert.assertTrue(testSetup.rebuildInventory());
    }

    @Test
    public void first() throws Exception{
        LOG.info(testSetup.getInventory().toString());
    }
}
