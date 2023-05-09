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


import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OsgiJarVersionInspectorTest {
    private final File projectDir = new File("src/test/resources/test-osgi-jar-version-extractor");

    @Test
    public void example0000() {
        File testJar = new File(new File("dummy-osgi-0.0.1-0000"), "dummy-osgi-0.0.1.jar");

        String jarPath = testJar.getPath();
        Artifact artifact = new Artifact();

        artifact.setId(testJar.getName());
        artifact.setArtifactId("dummy-osgi");
        artifact.setProjects(Collections.singleton(jarPath));

        // create mock inventory for inspector
        Inventory inventory = new Inventory();
        inventory.getArtifacts().add(artifact);

        // create mock properties for inspector
        final Properties properties = new Properties();
        properties.setProperty("project.path", projectDir.getPath());

        OsgiJarVersionInspector inspector = new OsgiJarVersionInspector();
        inspector.run(inventory, properties);

        assertEquals("0.0.1", artifact.getVersion());
        assertTrue(StringUtils.isBlank(artifact.get("Errors")));
    }

    @Test
    public void example0001() {
        File testJar = new File("dummy-osgi-0.0.1-0001.jar");

        String jarPath = testJar.getPath();
        Artifact artifact = new Artifact();
        artifact.setId(jarPath);
        artifact.setArtifactId("dummy-osgi");
        artifact.setProjects(Collections.singleton(jarPath));

        // set version to check that it isn't changed
        artifact.setVersion("4.2.4.4.2.2.2.2asd");

        // create mock inventory for inspector
        Inventory inventory = new Inventory();
        inventory.getArtifacts().add(artifact);

        // create mock properties for inspector
        final Properties properties = new Properties();
        properties.setProperty("project.path", projectDir.getPath());

        OsgiJarVersionInspector inspector = new OsgiJarVersionInspector();
        inspector.run(inventory, properties);
        assertEquals("4.2.4.4.2.2.2.2asd", artifact.getVersion());
        assertTrue(StringUtils.isBlank(artifact.get("Errors")));
    }

    @Test
    public void example0002() {
        File testJar = new File("dummy-osgi-0.0.1-0002.jar");

        String jarPath = testJar.getPath();
        Artifact artifact = new Artifact();
        artifact.setId(jarPath);
        artifact.setArtifactId("dummy-osgi");
        artifact.setProjects(Collections.singleton(jarPath));

        // set version to check that an error is recorded
        artifact.setVersion("4.2.4.4.2.2.2.2asd");

        // create mock inventory for inspector
        Inventory inventory = new Inventory();
        inventory.getArtifacts().add(artifact);

        // create mock properties for inspector
        final Properties properties = new Properties();
        properties.setProperty("project.path", projectDir.getPath());

        OsgiJarVersionInspector inspector = new OsgiJarVersionInspector();
        inspector.run(inventory, properties);

        assertEquals("4.2.4.4.2.2.2.2asd", artifact.getVersion());
        assertTrue(StringUtils.isBlank(artifact.get("Errors")));
    }
}
