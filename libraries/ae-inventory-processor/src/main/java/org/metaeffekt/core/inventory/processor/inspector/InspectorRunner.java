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
package org.metaeffekt.core.inventory.processor.inspector;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The {@link InspectorRunner} can be configured with different inspectors and then executed.
 */
public class InspectorRunner {
    private final List<Class<? extends ArtifactInspector>> inspectors;

    public static class InspectorRunnerBuilder {
        final List<Class<? extends ArtifactInspector>> inspectorList = new ArrayList<>();

        private InspectorRunnerBuilder() {}

        public InspectorRunnerBuilder queue(Class<? extends ArtifactInspector> inspector) {
            inspectorList.add(inspector);
            return this;
        }

        public InspectorRunner build() {
            return new InspectorRunner(inspectorList);
        }
    }

    private InspectorRunner(List<Class<? extends ArtifactInspector>> inspectors) {
        this.inspectors = inspectors;
    }

    public static InspectorRunnerBuilder builder() {
        return new InspectorRunnerBuilder();
    }

    public void executeAll(Inventory inventory, Properties properties) {
        for (Class<? extends ArtifactInspector> inspector : inspectors) {
            try {
                inspector.newInstance().run(inventory, properties);
            } catch (InstantiationException | IllegalAccessException e) {
                // something grave must have gone wrong. construction should execute just fine once set up.
                throw new RuntimeException(e);
            }
        }
    }

}
