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
package org.metaeffekt.core.inventory.processor.inspector;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NestedJavaInspectorTest {

    static final File testDir = new File("src/test/resources/test-nested-java-inspector");
    static final Path testDirPath = testDir.toPath().toAbsolutePath();
    static final Properties projectPathParamProperties = new Properties();

    static {
        projectPathParamProperties.setProperty(ProjectPathParam.KEY_PROJECT_PATH, testDirPath.toString());
    }

    private Artifact getDummyArtifact(File projectFile) {
        // try to catch my own studpidity while writing tests
        if (!projectFile.exists()) {
            throw new RuntimeException("projectFile needs to exist.");
        }

        Artifact artifact = new Artifact();
        artifact.setId(projectFile.getName());
        artifact.deriveArtifactId();
        artifact.setProjects(Collections.singleton(projectFile.getPath()));

        return artifact;
    }

    private Inventory getDummyInventory(Artifact artifact) {
        Inventory inventory = new Inventory();
        inventory.setArtifacts(Collections.singletonList(artifact));

        return inventory;
    }

    @Test
    public void jarInJar() {
        File jarFileWithJar = new File(testDir, "dummy-0.0.1-0000.jar");

        NestedJavaInspector nestedJavaInspector = new NestedJavaInspector();

        Artifact artifact = getDummyArtifact(jarFileWithJar);
        Inventory inventory = getDummyInventory(artifact);
        nestedJavaInspector.run(inventory, new Properties());

        assertEquals("scan", artifact.getClassification());
    }

    @Test
    public void jarInEar() {
        File earFileWithJar = new File(testDir, "dummy-0.0.1-0001.ear");

        NestedJavaInspector nestedJavaInspector = new NestedJavaInspector();

        Artifact artifact = getDummyArtifact(earFileWithJar);
        Inventory inventory = getDummyInventory(artifact);
        nestedJavaInspector.run(inventory, new Properties());

        assertEquals("scan", artifact.getClassification());
    }

    @Test
    public void warInJar() {
        File earFileWithJar = new File(testDir, "dummy-0.0.1-0002.jar");

        NestedJavaInspector nestedJavaInspector = new NestedJavaInspector();

        Artifact artifact = getDummyArtifact(earFileWithJar);
        Inventory inventory = getDummyInventory(artifact);
        nestedJavaInspector.run(inventory, new Properties());

        assertEquals("scan", artifact.getClassification());
    }

    @Test
    public void onlyTxtInJar() {
        File fileWithOnlyTextInside = new File(testDir, "inner-file.jar");

        NestedJavaInspector nestedJavaInspector = new NestedJavaInspector();

        Artifact artifact = getDummyArtifact(fileWithOnlyTextInside);
        Inventory inventory = getDummyInventory(artifact);
        nestedJavaInspector.run(inventory, new Properties());

        assertNotEquals("scan", artifact.getClassification());
    }
}
