/*
 * Copyright 2022 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.probe;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.MavenJarMetadataExtractor;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.*;

public class MavenJarIdProbeTest {
    @Test
    public void example0000() {
        File testJar = new File("src/test/resources/test-maven-jar-meta-extractor/dummy-artifact-0.0.1-0000.jar");

        Artifact artifact = new Artifact();
        artifact.setProjects(Collections.singleton(testJar.getPath()));

        MavenJarIdProbe probe = new MavenJarIdProbe(artifact);
        probe.runCompletion();

        // should have extracted
        assertEquals("my.test.dummy.package", artifact.getGroupId());
        assertEquals("dummy-artifact", artifact.getArtifactId());
        assertEquals("0.0.1", artifact.getVersion());
    }

    @Test
    public void example0001() {
        File testJar = new File("src/test/resources/test-maven-jar-meta-extractor/dummy-artifact-0.0.1-0001.jar");

        Artifact artifact = new Artifact();
        artifact.setProjects(Collections.singleton(testJar.getPath()));

        MavenJarIdProbe probe = new MavenJarIdProbe(artifact);
        probe.runCompletion();

        // should have gotten correct data and ignored the parent
        assertEquals("my.test.dummy.package", artifact.getGroupId());
        assertEquals("dummy-artifact", artifact.getArtifactId());
        assertEquals("0.0.1", artifact.getVersion());
    }

    @Test
    public void example0002() {
        File testJar = new File("src/test/resources/test-maven-jar-meta-extractor/dummy-artifact-0.0.2-0002.jar");

        Artifact artifact = new Artifact();
        artifact.setProjects(Collections.singleton(testJar.getPath()));

        MavenJarIdProbe probe = new MavenJarIdProbe(artifact);
        probe.runCompletion();

        // should have gotten correct data, partially inherited from parent (like maven does it)
        assertEquals("good.parent.groupid", artifact.getGroupId());
        assertEquals("dummy-artifact", artifact.getArtifactId());
        assertEquals("0.0.2", artifact.getVersion());
    }

    @Test
    public void example0003() {
        File testJar = new File("src/test/resources/test-maven-jar-meta-extractor/dummy-artifact-0003.jar");

        Artifact artifact = new Artifact();
        artifact.setProjects(Collections.singleton(testJar.getPath()));

        MavenJarIdProbe probe = new MavenJarIdProbe(artifact);
        probe.runCompletion();

        // should have logged an error to the artifact
        assertTrue(StringUtils.isNotBlank(artifact.get("Errors")));
    }

    @Test
    public void example0004() {
        File testJar = new File("src/test/resources/test-maven-jar-meta-extractor/dummy-artifact-0004.jar");

        Artifact artifact = new Artifact();
        artifact.setProjects(Collections.singleton(testJar.getPath()));

        MavenJarIdProbe probe = new MavenJarIdProbe(artifact);
        probe.runCompletion();

        // should have logged an error to the artifact
        System.err.println(artifact.get("Errors"));


        assertTrue(StringUtils.isNotBlank(artifact.get("Errors")));
    }
}
