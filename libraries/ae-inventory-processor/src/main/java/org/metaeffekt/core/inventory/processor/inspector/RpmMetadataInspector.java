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

import org.eclipse.packagedrone.utils.rpm.RpmTag;
import org.eclipse.packagedrone.utils.rpm.parse.RpmInputStream;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RpmMetadataInspector implements ArtifactInspector {

    private static final Logger LOG = LoggerFactory.getLogger(RpmMetadataInspector.class);

    @Override
    public void run(Inventory inventory, Properties properties) {
        final ProjectPathParam projectPathParam = new ProjectPathParam(properties);
        final Stream<Artifact> filteredArtifacts = inventory.getArtifacts().stream()
                .filter(artifact -> artifact.getPathInAsset() != null && artifact.getPathInAsset().endsWith(".rpm"));

            for (Artifact artifact : filteredArtifacts.collect(Collectors.toList())) {
                final File file = new File(projectPathParam.getProjectPath().getAbsolutePath() + "/" + artifact.getPathInAsset());
                if (file.exists()) {
                    try {
                        try (RpmInputStream in = new RpmInputStream(Files.newInputStream(file.toPath()))) {
                            artifact.set(Artifact.Attribute.LICENSE, (String) in.getPayloadHeader().getTag(RpmTag.LICENSE));
                            artifact.set(Artifact.Attribute.ORGANIZATION, (String) in.getPayloadHeader().getTag(RpmTag.VENDOR));
                            artifact.set(Artifact.Attribute.SOURCE, (String) in.getPayloadHeader().getTag(RpmTag.SOURCE_PACKAGE));
                        }
                    } catch (IOException e) {
                        LOG.info("Failed to read RPM metadata", e);
                }
            }
        }
    }
}

