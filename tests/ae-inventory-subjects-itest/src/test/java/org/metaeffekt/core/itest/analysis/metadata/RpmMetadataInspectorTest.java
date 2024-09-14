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
package org.metaeffekt.core.itest.analysis.metadata;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.inspector.RpmMetadataInspector;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.predicates.AttributeValue;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;
import static org.metaeffekt.core.itest.common.predicates.ContainsToken.containsToken;

public class RpmMetadataInspectorTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://vault.centos.org/8.4.2105/BaseOS/x86_64/os/Packages/krb5-libs-1.18.2-8.3.el8_4.x86_64.rpm")
                .setSha256Hash("3872004a25e644440c65cc1501614926a185831e1415248bd0d7871fe1648476")
                .setName(RpmMetadataInspectorTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(testSetup.clear());
    }

    @Test
    public void assertContent() throws Exception {
        File projectDir = new File(testSetup.getScanFolder());
        File testRpm = new File("krb5-libs-1.18.2-8.3.el8_4.x86_64.rpm");
        String rpmPath = testRpm.getPath();
        Artifact artifact = new Artifact();
        artifact.setId(rpmPath);
        artifact.setPathInAsset(projectDir.getPath() + "/" + rpmPath);

        // create mock inventory for inspector
        Inventory inventory = new Inventory();
        inventory.setArtifacts(Collections.singletonList(artifact));

        // create mock properties for inspector
        final Properties properties = new Properties();
        properties.setProperty("project.path", projectDir.getPath());

        RpmMetadataInspector inspector = new RpmMetadataInspector();
        inspector.run(inventory, properties);

        // should have extracted
        assertEquals("krb5-libs-1.18.2-8.3.el8_4.x86_64.rpm", artifact.getId());
        assertEquals(projectDir.getPath() + "/krb5-libs-1.18.2-8.3.el8_4.x86_64.rpm", artifact.getPathInAsset());

        ArtifactList artifactList = getAnalysisAfterInvariantCheck()
                .selectArtifacts();

        artifactList.logListWithAllAttributes();

        artifactList.with(AttributeValue.attributeValue(Artifact.Attribute.ID, "krb5-libs-1.18.2-8.3.el8_4.x86_64.rpm")).with();

        ArtifactList packageList = artifactList.with(containsToken(ID, ".rpm"));
        packageList.with(attributeValue(TYPE, "package")).hasSizeOf(packageList);
        packageList.with(attributeValue(COMPONENT_SOURCE_TYPE, "rpm-package")).hasSizeOf(packageList);
    }
}
