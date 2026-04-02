/*
 * Copyright 2009-2026 the original author or authors.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.StringJoiner;

public class ArtifactTest {

    @Test
    public void completeVulnerabilityLengthTest() {
        Artifact artifact = new Artifact();
        StringJoiner cveData = new StringJoiner(", ");
        for (int i = 0; i < 4000; i++) cveData.add("CVE-2022-21907 (9.8)");
        artifact.setVulnerability(cveData.toString());
        Assertions.assertEquals(cveData.length(), artifact.get("Vulnerability").length());
        Assertions.assertEquals(cveData.toString(), artifact.getVulnerability());
    }

    @Test
    public void longVulnerabilityResetTest() {
        Artifact artifact = new Artifact();
        StringJoiner cveData = new StringJoiner(", ");
        for (int i = 0; i < 4000; i++) cveData.add("CVE-2022-21907 (9.8)");
        artifact.setVulnerability(cveData.toString());
        Assertions.assertNotNull(artifact.get("Vulnerability"));
        Assertions.assertNull(artifact.get("Vulnerability (split-1)"));
        artifact.setVulnerability(null);
        Assertions.assertNull(artifact.get("Vulnerability"));
        Assertions.assertNull(artifact.get("Vulnerability (split-1)"));
    }

    @Test
    public void completeVulnerabilityNullTest() {
        Artifact artifact = new Artifact();
        artifact.setVulnerability(null);
        Assertions.assertNull(artifact.getVulnerability());
    }

    @Test
    public void deriveClassifierTest() {
        {
            Artifact artifact = new Artifact();
            artifact.setId("guava-25.1-jre.jar");
            artifact.setVersion("25.1");
            Assertions.assertEquals("jre", artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("guava-25.1-jre.jar");
            artifact.setVersion("25.1-jre");
            Assertions.assertNull(artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId--classifier.txt");
            artifact.setVersion("");
            Assertions.assertNull(artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId-null-classifier.txt");
            artifact.setVersion(null);
            Assertions.assertNull(artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId-X-classifier.txt");
            artifact.setVersion("X");
            Assertions.assertEquals("classifier", artifact.getClassifier());
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
        Assertions.assertTrue(rootPaths.contains("A"));
        Assertions.assertTrue(rootPaths.contains("A,B"));
        Assertions.assertTrue(rootPaths.contains("A, B , C"));
        Assertions.assertTrue(rootPaths.contains("D"));
    }

    @Test
    public void artifactHardwareTypesTrueTest() {
        final Artifact artifact = new Artifact();
        artifact.set(Artifact.Attribute.ID, "some hardware component");

        for (ArtifactType type : ArtifactType.ARTIFACT_TYPES) {
            artifact.set(Artifact.Attribute.TYPE, type.getCategory());
            Assertions.assertEquals(type.isHardware(), artifact.isHardware());
        }
    }

    @Test
    public void artifactInvalidHardwareTypesTest() {
        final Artifact artifact = new Artifact();
        artifact.set(Artifact.Attribute.ID, "some non hardware component");

        artifact.set(Artifact.Attribute.TYPE, "package");
        Assertions.assertFalse(artifact.isHardware());

        artifact.set(Artifact.Attribute.TYPE, "library");
        Assertions.assertFalse(artifact.isHardware());

        artifact.set(Artifact.Attribute.TYPE, "python-module");
        Assertions.assertFalse(artifact.isHardware());
    }

    @Test
    public void artifactGetArtifactTypeTest() {
        final Artifact artifact = new Artifact();
        artifact.set(Artifact.Attribute.ID, "some non hardware component");

        artifact.set(Artifact.Attribute.TYPE, "library");
        Assertions.assertFalse(artifact.getArtifactType().isPresent());

        artifact.set(Artifact.Attribute.TYPE, "package");
        Assertions.assertEquals(ArtifactType.LINUX_PACKAGE, artifact.getArtifactType().get());
        Assertions.assertTrue(artifact.getArtifactType().get().isOrHasParent(ArtifactType.CATEGORY_SOFTWARE_LIBRARY));

        artifact.set(Artifact.Attribute.TYPE, "python-module");
        Assertions.assertEquals(ArtifactType.PYTHON_MODULE, artifact.getArtifactType().get());
        Assertions.assertTrue(artifact.getArtifactType().get().isOrHasParent(ArtifactType.CATEGORY_SOFTWARE_LIBRARY));

        artifact.set(Artifact.Attribute.TYPE, "operating system");
        Assertions.assertEquals(ArtifactType.OPERATING_SYSTEM, artifact.getArtifactType().get());
        Assertions.assertFalse(artifact.getArtifactType().get().isOrHasParent(ArtifactType.CATEGORY_SOFTWARE_LIBRARY));
    }

    @Test
    public void testDeriveArtifactId() {
        Artifact a = new Artifact();
        a.setId("a.b.c.d-1.0.0.jar");
        a.setGroupId("a.b.c");
        a.setVersion("1.0.0");
        a.deriveArtifactId();
        Assertions.assertEquals("d", a.getArtifactId());
    }

    @Test
    public void testDeriveArtifactIdAndClassifier() {
        Artifact a = new Artifact();
        a.setId("a.b.c.d-1.0.0-xyz.jar");
        a.setGroupId("a.b.c");
        a.setVersion("1.0.0");
        a.deriveArtifactId();
        Assertions.assertEquals("d", a.getArtifactId());
        Assertions.assertEquals("xyz", a.getClassifier());
    }

    @Test
    public void testDeriveArtifactAttributes_NoSuffix() {
        Artifact a = new Artifact();
        a.setId("commons-beanutil-1.8.3");
        a.setVersion("1.8.3");
        a.deriveArtifactId();
        Assertions.assertEquals("commons-beanutil", a.getArtifactId());
        Assertions.assertNull(a.getType());
    }

    @Test
    public void testDeriveArtifactAttributesWithSuffix() {
        Artifact a = new Artifact();
        a.setId("commons-beanutil-1.8.3.jar");
        a.setVersion("1.8.3");
        a.deriveArtifactId();
        Assertions.assertEquals("commons-beanutil", a.getArtifactId());
        Assertions.assertEquals("jar", a.getType());
    }

}