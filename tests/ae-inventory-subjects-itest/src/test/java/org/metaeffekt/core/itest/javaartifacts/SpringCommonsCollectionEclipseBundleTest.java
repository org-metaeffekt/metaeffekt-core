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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCommonsCollectionEclipseBundleTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("http://www.java2s.com/Code/JarDownload/com.springsource.org.apache/com.springsource.org.apache.commons.collections-3.2.1.jar.zip")
                .setName(SpringCommonsCollectionEclipseBundleTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception{
        Assert.assertTrue(preparer.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception{
        Assert.assertTrue(preparer.rebuildInventory());
    }

    @Test
    public void first() throws Exception{
        LOG.info(preparer.getInventory().toString());
    }

    @Test
    public void testCompositionAnalysis() throws Exception {
        final Inventory inventory= preparer.getInventory();

        inventory.getArtifacts().stream().map(a -> a.deriveQualifier()).forEach(LOG::info);

        assertThat(inventory.findArtifact(
                "com.springsource.org.apache.commons.collections-3.2.1.jar")).isNotNull();

        // the embedded artifact is listed separately as we cannot connect it to the containing one;
        assertThat(inventory.findArtifact(
                "commons-collections-3.2.1.jar")).isNotNull();

        // also no attributes should be shared
        assertThat(inventory.findArtifact(
                "com.springsource.org.apache.commons.collections-3.2.1.jar").getGroupId()).isNull();

        assertThat(inventory.findArtifact(
                "commons-collections-3.2.1.jar").getGroupId()).isNotNull();


        assertThat(inventory.getArtifacts().size()).isEqualTo(3);
    }

}
