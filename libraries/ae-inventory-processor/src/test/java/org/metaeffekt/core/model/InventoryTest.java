/**
 * Copyright 2009-2018 the original author or authors.
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
package org.metaeffekt.core.model;


import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import static org.metaeffekt.core.inventory.processor.model.Constants.ASTERISK;

public class InventoryTest {
    @Test
    public void testFindCurrent() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setLicense("L");
        artifact.setComponent("Test Component");
        artifact.setClassification("current");
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.setGroupId("org.test");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setLicense("L");
        candidate.setComponent("Test Component");
        candidate.setGroupId("org.test");
        candidate.setId("test-1.0.0.jar");
        candidate.setVersion("1.0.0");
        candidate.deriveArtifactId();

        Assert.assertNotNull(inventory.findArtifact(candidate));
        Assert.assertNotNull(inventory.findArtifactClassificationAgnostic(candidate));
    }

    @Test
    public void testFindClassificationAgnostic() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setLicense("L");
        artifact.setComponent("Test Component");
        artifact.setClassification("any");
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.setGroupId("org.test");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setLicense("L");
        candidate.setComponent("Test Component");
        candidate.setClassification("any");
        candidate.setGroupId("org.test");
        candidate.setVersion("1.0.1");
        candidate.setId("test-1.0.1.jar");
        candidate.deriveArtifactId();

        Assert.assertNull(inventory.findArtifact(candidate));
        Assert.assertNotNull(inventory.findArtifactClassificationAgnostic(candidate));
    }

    @Test
    public void testUnderscoreSupport() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test_1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("test-1.0.0.jar");
        candidate.deriveArtifactId();

        Assert.assertEquals("test", artifact.getArtifactId());

        // while the artifacts are equivalent with respect to GAV details they remain different by id.
        Assert.assertNull(inventory.findArtifact("test-1.0.0.jar"));
        Assert.assertNotNull(inventory.findArtifact("test_1.0.0.jar"));
    }

    @Test
    public void testWildcardMatch() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-" + ASTERISK + ".jar");
        artifact.setVersion(ASTERISK);
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("test-1.0.0.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), ASTERISK);
    }
}
