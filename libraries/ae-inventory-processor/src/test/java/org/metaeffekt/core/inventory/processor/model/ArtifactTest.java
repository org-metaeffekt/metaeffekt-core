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
package org.metaeffekt.core.inventory.processor.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.StringJoiner;

public class ArtifactTest {

    @Test
    public void completeVulnerabilityLengthTest() {
        Artifact artifact = new Artifact();
        StringJoiner cveData = new StringJoiner(", ");
        for (int i = 0; i < 4000; i++) cveData.add("CVE-2022-21907 (9.8)");
        artifact.setVulnerability(cveData.toString());
        Assert.assertEquals(cveData.length(), artifact.get("Vulnerability").length());
        Assert.assertEquals(cveData.toString(), artifact.getVulnerability());
    }

    @Test
    public void longVulnerabilityResetTest() {
        Artifact artifact = new Artifact();
        StringJoiner cveData = new StringJoiner(", ");
        for (int i = 0; i < 4000; i++) cveData.add("CVE-2022-21907 (9.8)");
        artifact.setVulnerability(cveData.toString());
        Assert.assertNotNull(artifact.get("Vulnerability"));
        Assert.assertNull(artifact.get("Vulnerability (split-1)"));
        artifact.setVulnerability(null);
        Assert.assertNull(artifact.get("Vulnerability"));
        Assert.assertNull(artifact.get("Vulnerability (split-1)"));
    }

    @Test
    public void completeVulnerabilityNullTest() {
        Artifact artifact = new Artifact();
        artifact.setVulnerability(null);
        Assert.assertNull(artifact.getVulnerability());
    }

    @Test
    public void deriveClassifierTest() {
        {
            Artifact artifact = new Artifact();
            artifact.setId("guava-25.1-jre.jar");
            artifact.setVersion("25.1");
            Assert.assertEquals("jre", artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("guava-25.1-jre.jar");
            artifact.setVersion("25.1-jre");
            Assert.assertNull(artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId--classifier.txt");
            artifact.setVersion("");
            Assert.assertNull(artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId-null-classifier.txt");
            artifact.setVersion(null);
            Assert.assertNull(artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId-X-classifier.txt");
            artifact.setVersion("X");
            Assert.assertEquals("classifier", artifact.getClassifier());
        }
    }

    @Test
    public void testProjects() {
        Artifact artifact = new Artifact();
        artifact.addProject("A");
        artifact.addProject("A,B");
        artifact.addProject("A, B , C");
        artifact.addProject("D");

        final Set<String> projects = artifact.getProjects();
        Assert.assertTrue(projects.contains("A"));
        Assert.assertTrue(projects.contains("A,B"));
        Assert.assertTrue(projects.contains("A, B , C"));
        Assert.assertTrue(projects.contains("D"));
    }

    @Test
    public void artifactHardwareTypesTrueTest() {
        final Artifact artifact = new Artifact();
        artifact.set(Artifact.Attribute.ID, "some hardware component");

        for (ArtifactType type : ArtifactType.ARTIFACT_TYPES) {
            artifact.set(Artifact.Attribute.TYPE, type.getCategory());
            Assert.assertEquals(type.isHardware(), artifact.isHardware());
        }
    }

    @Test
    public void artifactInvalidHardwareTypesTest() {
        final Artifact artifact = new Artifact();
        artifact.set(Artifact.Attribute.ID, "some non hardware component");

        artifact.set(Artifact.Attribute.TYPE, "package");
        Assert.assertFalse(artifact.isHardware());

        artifact.set(Artifact.Attribute.TYPE, "library");
        Assert.assertFalse(artifact.isHardware());

        artifact.set(Artifact.Attribute.TYPE, "python-module");
        Assert.assertFalse(artifact.isHardware());
    }

    @Test
    public void artifactGetArtifactTypeTest() {
        final Artifact artifact = new Artifact();
        artifact.set(Artifact.Attribute.ID, "some non hardware component");

        artifact.set(Artifact.Attribute.TYPE, "library");
        Assert.assertFalse(artifact.getArtifactType().isPresent());

        artifact.set(Artifact.Attribute.TYPE, "package");
        Assert.assertEquals(ArtifactType.LINUX_PACKAGE, artifact.getArtifactType().get());
        Assert.assertTrue(artifact.getArtifactType().get().isOrHasParent(ArtifactType.CATEGORY_SOFTWARE_LIBRARY));

        artifact.set(Artifact.Attribute.TYPE, "python-module");
        Assert.assertEquals(ArtifactType.PYTHON_MODULE, artifact.getArtifactType().get());
        Assert.assertTrue(artifact.getArtifactType().get().isOrHasParent(ArtifactType.CATEGORY_SOFTWARE_LIBRARY));

        artifact.set(Artifact.Attribute.TYPE, "operating system");
        Assert.assertEquals(ArtifactType.OPERATING_SYSTEM, artifact.getArtifactType().get());
        Assert.assertFalse(artifact.getArtifactType().get().isOrHasParent(ArtifactType.CATEGORY_SOFTWARE_LIBRARY));
    }
}