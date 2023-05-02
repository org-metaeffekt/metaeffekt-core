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
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.util.Set;
import java.util.StringJoiner;

public class ArtifactTest {

    @Test
    public void completeVulnerabilityLengthTest() {
        Artifact artifact = new Artifact();
        StringJoiner cveData = new StringJoiner(", ");
        for (int i = 0; i < 4000; i++) cveData.add("CVE-2022-21907 (9.8)");
        artifact.setCompleteVulnerability(cveData.toString());
        Assert.assertEquals(InventoryWriter.MAX_CELL_LENGTH, artifact.get("Vulnerability (split-1)").length());
        Assert.assertEquals(cveData.toString(), artifact.getCompleteVulnerability());
    }

    @Test
    public void completeVulnerabilityResetTest() {
        Artifact artifact = new Artifact();
        StringJoiner cveData = new StringJoiner(", ");
        for (int i = 0; i < 4000; i++) cveData.add("CVE-2022-21907 (9.8)");
        artifact.setCompleteVulnerability(cveData.toString());
        Assert.assertNotNull(artifact.get("Vulnerability (split-1)"));
        artifact.setCompleteVulnerability(null);
        Assert.assertNull(artifact.get("Vulnerability (split-1)"));
    }

    @Test
    public void completeVulnerabilityNullTest() {
        Artifact artifact = new Artifact();
        artifact.setCompleteVulnerability(null);
        Assert.assertNull(artifact.getCompleteVulnerability());
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
            Assert.assertEquals(null, artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId--classifier.txt");
            artifact.setVersion("");
            Assert.assertEquals(null, artifact.getClassifier());
        }
        {
            Artifact artifact = new Artifact();
            artifact.setId("artifactId-null-classifier.txt");
            artifact.setVersion(null);
            Assert.assertEquals(null, artifact.getClassifier());
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

}