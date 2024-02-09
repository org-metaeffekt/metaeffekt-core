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
package org.metaeffekt.core.itest.analysis.bundles;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.setup.AbstractBasicInvariantsTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.metaeffekt.core.itest.common.Analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.AttributeExists.withAttribute;

public class SpringCommonsCollectionEclipseBundleTest extends AbstractBasicInvariantsTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("http://www.java2s.com/Code/JarDownload/com.springsource.org.apache/com.springsource.org.apache.commons.collections-3.2.1.jar.zip")
                .setSha256Hash("6fec79a57993aa10afe183d81aded02c10b5631547daf725739f1fb9b58b2d5b")
                .setName(SpringCommonsCollectionEclipseBundleTest.class.getName());
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

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory= testSetup.getInventory();

        Analysis analysis = new Analysis(inventory);

        analysis.selectArtifacts().logArtifactList();

        analysis.selectArtifacts(attributeValue("Id", "com.springsource.org.apache.commons.collections-3.2.1.jar")).hasSizeOf(1);

        // the embedded artifact is listed separately as we cannot connect it to the containing one;
        analysis.selectArtifacts(attributeValue("Id", "commons-collections-3.2.1.jar")).hasSizeOf(1);
        analysis.selectArtifacts(attributeValue("Id", "commons-collections-3.2.1.jar")).assertAll(withAttribute("Group Id"));
        analysis.selectArtifacts().hasSizeOf(3);
    }

}