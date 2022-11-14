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


    @Test
    public void testSpecificWildcardMatch() {
        Inventory inventory = new Inventory();

        final Artifact artifact1 = new Artifact();
        artifact1.setId("test-" + ASTERISK + ".jar");
        artifact1.setVersion(ASTERISK);
        artifact1.deriveArtifactId();

        final Artifact artifact2 = new Artifact();
        artifact2.setId("test-lib-" + ASTERISK + ".jar");
        artifact2.setVersion(ASTERISK);
        artifact2.deriveArtifactId();

        inventory.getArtifacts().add(artifact1);
        inventory.getArtifacts().add(artifact2);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("test-lib-1.0.0.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getId(), "test-lib-" + ASTERISK + ".jar");
    }

    @Test
    public void testMatch_withVersion() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("test-1.0.0.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), "1.0.0");
    }

    @Test
    public void testMatch_withoutVersion() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test-1.0.0.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), "1.0.0");
    }

    @Test
    public void testMatch_withChecksumDifference() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-1.0.0.jar");
        artifact.setChecksum("A");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test-1.0.0.jar");
        candidate.setVersion("1.0.0");
        candidate.setChecksum("B");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact == null);
    }

    @Test
    public void testMatch_withVersionDifference() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test-1.0.0.jar");
        artifact.setVersion("1.0.0");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test-1.0.0.jar");
        candidate.setVersion("1.0.1");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact == null);
    }

    @Test
    public void testMatch_withVersionDifferencePlaceholder() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test.jar");
        artifact.setVersion("${PROJECTVERSION}");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test.jar");
        candidate.setVersion("1");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact == null);
    }

    @Test
    public void testMatch_withVersionDifferencePlaceholderBoth() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId("test.jar");
        artifact.setVersion("${PROJECTVERSION}");
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setId("test.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), "${PROJECTVERSION}");
    }

    @Test
    public void testWildcardMatch_AsteriskAsPrefix() {
        Inventory inventory = new Inventory();
        final Artifact artifact = new Artifact();
        artifact.setId(ASTERISK + "test.jar");
        artifact.setVersion(ASTERISK);
        artifact.deriveArtifactId();
        inventory.getArtifacts().add(artifact);

        final Artifact candidate = new Artifact();
        candidate.setVersion("1.0.0");
        candidate.setId("1.0.0.test.jar");
        candidate.deriveArtifactId();

        Artifact matchedArtifact = inventory.findArtifact(candidate, true);
        Assert.assertTrue(matchedArtifact != null);
        Assert.assertEquals(matchedArtifact.getVersion(), ASTERISK);
    }

}
