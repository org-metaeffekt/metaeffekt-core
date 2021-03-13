/**
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.inventory.resolver;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This implementation of the {@link SourceArchiveResolver} interface derives the source archive from the artifact
 * component and version.
 */
public class ComponentSourceArchiveResolver extends AbstractMirrorSourceArchiveResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentSourceArchiveResolver.class);

    private List<Mapping> mappings;

    @Override
    protected List<String> matchAndReplace(Artifact artifact) {
        List<String> matches = new ArrayList<>();
        for (Mapping mapping : mappings) {
            String pattern = mapping.getPattern();
            String componentPlusVersion = artifact.getComponent() + "-" + artifact.getVersion();
            if (componentPlusVersion.equals(pattern) || pattern.startsWith("^") && componentPlusVersion.matches(pattern)) {
                String replacement = mapping.getReplacement();
                if (artifact.getId() != null) {
                    replacement = replacement.replace("$id", artifact.getId());
                }
                if (artifact.getVersion() != null) {
                    replacement = replacement.replace("$version", artifact.getVersion());
                }
                if (artifact.getArtifactId() != null) {
                    replacement = replacement.replace("$artifactId", artifact.getArtifactId());
                }
                if (artifact.getGroupId() != null) {
                    replacement = replacement.replace("$groupId", artifact.getGroupId());
                }
                matches.add(componentPlusVersion.replaceAll(pattern, replacement));
            }
         }
        return matches;
    }

    public void addMapping(Mapping mapping) {
        if (mappings == null) {
            mappings = new ArrayList<>();
        }
        mappings.add(mapping);
    }
}
