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

import com.github.luben.zstd.ZstdInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.eclipse.packagedrone.utils.rpm.RpmTag;
import org.eclipse.packagedrone.utils.rpm.parse.RpmInputStream;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RpmMetadataInspector implements ArtifactInspector {

    private static final Logger LOG = LoggerFactory.getLogger(RpmMetadataInspector.class);

    @Override
    public void run(Inventory inventory, Properties properties) {
        ProjectPathParam projectPathParam = new ProjectPathParam(properties);
        Stream<Artifact> filteredArtifacts = inventory.getArtifacts().stream()
                .filter(artifact -> artifact.getPathInAsset().endsWith(".rpm"));

        for (Artifact artifact : filteredArtifacts.collect(Collectors.toList())) {
            File file = new File(projectPathParam.getProjectPath().getAbsolutePath() + "/" + artifact.getPathInAsset());
            if (file.exists()) {
                try {
                    try (RpmInputStream in = new RpmInputStream(Files.newInputStream(file.toPath()))) {
                        artifact.set(Artifact.Attribute.LICENSE, (String) in.getPayloadHeader().getTag(RpmTag.LICENSE));
                        artifact.set(Artifact.Attribute.ORGANIZATION, (String) in.getPayloadHeader().getTag(RpmTag.VENDOR));
                        artifact.set(Artifact.Attribute.SOURCE, (String) in.getPayloadHeader().getTag(RpmTag.SOURCE_PACKAGE));
                    }
                } catch (IOException e) {
                    LOG.info("Failed to read RPM metadata directly, attempting with decompression", e);
                    boolean success = false;

                    try (FileInputStream fis = new FileInputStream(file);
                         GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fis);
                         RpmInputStream in = new RpmInputStream(gzipInputStream)) {
                        artifact.set(Artifact.Attribute.LICENSE, (String) in.getPayloadHeader().getTag(RpmTag.LICENSE));
                        artifact.set(Artifact.Attribute.ORGANIZATION, (String) in.getPayloadHeader().getTag(RpmTag.VENDOR));
                        artifact.set(Artifact.Attribute.SOURCE, (String) in.getPayloadHeader().getTag(RpmTag.SOURCE_PACKAGE));
                        success = true;
                    } catch (IOException ignored) {
                    }

                    if (!success) {
                        try (FileInputStream fis = new FileInputStream(file);
                             XZCompressorInputStream xzInputStream = new XZCompressorInputStream(fis);
                             RpmInputStream in = new RpmInputStream(xzInputStream)) {
                            artifact.set(Artifact.Attribute.LICENSE, (String) in.getPayloadHeader().getTag(RpmTag.LICENSE));
                            artifact.set(Artifact.Attribute.ORGANIZATION, (String) in.getPayloadHeader().getTag(RpmTag.VENDOR));
                            artifact.set(Artifact.Attribute.SOURCE, (String) in.getPayloadHeader().getTag(RpmTag.SOURCE_PACKAGE));
                            success = true;
                        } catch (IOException ignored) {
                        }
                    }

                    if (!success) {
                        try (FileInputStream fis = new FileInputStream(file);
                             ZstdInputStream zstdInputStream = new ZstdInputStream(fis);
                             RpmInputStream in = new RpmInputStream(zstdInputStream)) {
                            artifact.set(Artifact.Attribute.LICENSE, (String) in.getPayloadHeader().getTag(RpmTag.LICENSE));
                            artifact.set(Artifact.Attribute.ORGANIZATION, (String) in.getPayloadHeader().getTag(RpmTag.VENDOR));
                            artifact.set(Artifact.Attribute.SOURCE, (String) in.getPayloadHeader().getTag(RpmTag.SOURCE_PACKAGE));
                        } catch (IOException ex) {
                            LOG.warn("Could not read RPM metadata from file: [{}]", file.getAbsolutePath(), ex);
                        }
                    }
                }
            }
        }
    }
}

