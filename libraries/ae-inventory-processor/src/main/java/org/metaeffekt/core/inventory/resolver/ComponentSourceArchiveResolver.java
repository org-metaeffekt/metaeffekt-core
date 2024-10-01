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
package org.metaeffekt.core.inventory.resolver;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * This implementation of the {@link SourceArchiveResolver} interface derives the source archive from the artifact
 * component and version.
 */
public class ComponentSourceArchiveResolver extends AbstractMirrorSourceArchiveResolver {

    private List<Mapping> mappings;

    @Override
    protected List<String> matchAndReplace(Artifact artifact) {
        // ensure the artifactId is computed
        artifact.deriveArtifactId();

        List<String> matches = new ArrayList<>();

        for (Mapping mapping : mappings) {
            String pattern = mapping.getPattern();

            if (isNotEmpty(artifact.getComponent()) && isNotEmpty(artifact.getVersion())) {
                // legacy (dash separated)
                addMatchCandidate(artifact, mapping, artifact.getComponent() + "-" + artifact.getVersion(), pattern, matches);

                // colon separated
                addMatchCandidate(artifact, mapping, artifact.getComponent() + ":" + artifact.getVersion(), pattern, matches);
            }

            // artifact id based
            if (isNotEmpty(artifact.getId())) {
                addMatchCandidate(artifact, mapping, artifact.getId(), pattern, matches);
            }

            // groupId:artifactId:version based
            if (isNotEmpty(artifact.getGroupId()) && isNotEmpty(artifact.getArtifactId()) && isNotEmpty(artifact.getVersion())) {
                addMatchCandidate(artifact, mapping, artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion(), pattern, matches);
            }

        }
        return matches;
    }

    private static void addMatchCandidate(Artifact artifact, Mapping mapping, String componentPlusVersion, String pattern, List<String> matches) {
        if (componentPlusVersion.equals(pattern) || pattern.startsWith("^") && componentPlusVersion.matches(pattern)) {
            String replacement = mapping.getReplacement();

            replacement = replacement.replace("$id", replacementValue(artifact.getId()));
            replacement = replacement.replace("$version", replacementValue(artifact.getVersion()));
            replacement = replacement.replace("$artifactId", replacementValue(artifact.getArtifactId()));
            replacement = replacement.replace("$groupIdSlash", replacementValue(artifact.getGroupId()).replace(".", "/"));
            replacement = replacement.replace("$groupId", replacementValue(artifact.getGroupId()));
            replacement = replacement.replace("$component", replacementValue(artifact.getComponent()));
            replacement = replacement.replace("$classifier", replacementValue(artifact.getClassifier()));
            replacement = replacement.replace("$type", replacementValue(artifact.getType()));

            // the replacement may still contain further groups ($1 ...), by using replaceAll the groups can be bound
            matches.add(componentPlusVersion.replaceAll(pattern, replacement));
        }
    }

    private static String replacementValue(String value) {
        return isNotEmpty(value) ? value : "-";
    }

    public void addMapping(Mapping mapping) {
        if (mappings == null) {
            mappings = new ArrayList<>();
        }
        mappings.add(mapping);
    }
}
