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
            artifact.set(Artifact.Attribute.FILE_NAME, "guava-25.1-jre.jar");
            artifact.setVersion("25.1");
            String classifier = artifact.inferClassifierFromFileNameAndVersion();
            Assert.assertEquals("jre", classifier);
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("guava-25.1-jre.jar");
            artifact.set(Artifact.Attribute.FILE_NAME, "guava-25.1-jre.jar");
            artifact.setVersion("25.1-jre");
            String classifier = artifact.inferClassifierFromFileNameAndVersion();
            Assert.assertNull(classifier);
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId--classifier.txt");
            artifact.set(Artifact.Attribute.FILE_NAME, "artifactId--classifier.txt");
            artifact.setVersion("");
            String classifier = artifact.inferClassifierFromFileNameAndVersion();
            Assert.assertNull(classifier);
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId-null-classifier.txt");
            artifact.set(Artifact.Attribute.FILE_NAME, "artifactId-null-classifier.txt");
            artifact.setVersion(null);
            String classifier = artifact.inferClassifierFromFileNameAndVersion();
            Assert.assertNull(classifier);
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId-X-classifier.txt");
            artifact.set(Artifact.Attribute.FILE_NAME, "artifactId-X-classifier.txt");
            artifact.setVersion("X");
            String classifier = artifact.inferClassifierFromFileNameAndVersion();
            Assert.assertEquals("classifier", classifier);
        }
    }

    @Test
    public void testProjects() {
        Artifact artifact = new Artifact();
        artifact.addRootPath("A");
        artifact.addRootPath("A,B");
        artifact.addRootPath("A, B , C");
        artifact.addRootPath("D");

        final Set<String> rootPaths = artifact.getRootPaths();
        Assert.assertTrue(rootPaths.contains("A"));
        Assert.assertTrue(rootPaths.contains("A,B"));
        Assert.assertTrue(rootPaths.contains("A, B , C"));
        Assert.assertTrue(rootPaths.contains("D"));
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

    @Test
    public void testDeriveArtifactId() {
        Artifact a = new Artifact();
        a.setId("a.b.c.d-1.0.0.jar");
        a.setGroupId("a.b.c");
        a.setVersion("1.0.0");
        a.deriveArtifactId();
        Assert.assertEquals("d", a.getArtifactId());
    }

    @Test
    public void testDeriveArtifactIdAndClassifier() {
        Artifact a = new Artifact();
        a.setId("a.b.c.d-1.0.0-xyz.jar");
        a.setGroupId("a.b.c");
        a.set(Artifact.Attribute.FILE_NAME, "a.b.c.d-1.0.0-xyz.jar");
        a.setVersion("1.0.0");
        a.deriveArtifactId();
        Assert.assertEquals("d", a.getArtifactId());
        String classifier = a.inferClassifierFromFileNameAndVersion();
        Assert.assertEquals("xyz", classifier);
    }

    @Test
    public void testDeriveArtifactAttributes_NoSuffix() {
        Artifact a = new Artifact();
        a.setId("commons-beanutil-1.8.3");
        a.setVersion("1.8.3");
        a.deriveArtifactId();
        Assert.assertEquals("commons-beanutil", a.getArtifactId());
        Assert.assertNull(a.getType());
    }

    @Test
    public void testDeriveArtifactAttributesWithSuffix() {
        Artifact a = new Artifact();
        a.setId("commons-beanutil-1.8.3.jar");
        a.setVersion("1.8.3");
        a.deriveArtifactId();
        Assert.assertEquals("commons-beanutil", a.getArtifactId());
        Assert.assertEquals("jar", a.getType());
    }

}