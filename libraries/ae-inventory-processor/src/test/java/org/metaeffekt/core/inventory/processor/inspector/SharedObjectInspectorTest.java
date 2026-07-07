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
package org.metaeffekt.core.inventory.processor.inspector;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.util.Properties;

import static org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam.KEY_PROJECT_PATH;
import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_PATH_IN_ASSET;

public class SharedObjectInspectorTest {

    @Disabled
    @Test
    public void test01() {
        File projectPath = new File("../../.examples/scan/case-xxx");
        inspectAndAssert(projectPath, new File("<some-path>/libxerces-c-3.1.so"), "3.1");
    }

    private void inspectAndAssert(File projectPath, File file, String expectedVersion) {
        Properties properties = new Properties();
        properties.put(KEY_PROJECT_PATH, projectPath.getAbsolutePath());

        Inventory inventory = new Inventory();

        Artifact artifact = new Artifact();
        artifact.setId(file.getName());
        artifact.set(KEY_PATH_IN_ASSET, file.getPath());

        inventory.getArtifacts().add(artifact);

        SharedObjectInspector sharedObjectInspector = new SharedObjectInspector();
        sharedObjectInspector.run(inventory, properties);

        Assertions.assertThat(inventory.getArtifacts().get(0).getVersion()).isEqualTo(expectedVersion);
    }

}
