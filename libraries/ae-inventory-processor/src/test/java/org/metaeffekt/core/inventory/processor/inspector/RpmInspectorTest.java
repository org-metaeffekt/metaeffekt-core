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
package org.metaeffekt.core.inventory.processor.inspector;

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class RpmInspectorTest {
    private final File projectDir = new File("src/test/resources/rpm-inspector");

    @Ignore // please do not commit / use local binaries; move test to resolver or integration tests
    @Test
    public void example() {
        File testRpm = new File("git-2.45.2-2.fc40.x86_64.rpm");

        String rpmPath = testRpm.getPath();
        Artifact artifact = new Artifact();
        artifact.setId(rpmPath);
        artifact.setPathInAsset(rpmPath);

        // create mock inventory for inspector
        Inventory inventory = new Inventory();
        inventory.setArtifacts(Collections.singletonList(artifact));

        // create mock properties for inspector
        final Properties properties = new Properties();
        properties.setProperty("project.path", projectDir.getPath());

        RpmMetadataInspector inspector = new RpmMetadataInspector();
        inspector.run(inventory, properties);

        // should have extracted
        assertEquals("my.test.dummy.package", artifact.getGroupId());
        assertEquals("dummy-artifact", artifact.getArtifactId());
        assertEquals("0.0.1", artifact.getVersion());
    }
}
