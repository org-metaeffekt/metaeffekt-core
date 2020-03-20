/**
 * Copyright 2009-2020 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates and structures all information to render notices for a given artifacts.
 */
public class ArtifactNotice {

    private final String notice;

    private final List<Artifact> artifacts;

    private final String discriminator;

    public ArtifactNotice(String notice, String discriminator) {
        this.notice = notice;
        this.discriminator = discriminator;
        this.artifacts = new ArrayList<>();
    }

    public boolean add(Artifact artifact) {
        return artifacts.add(artifact);
    }

    public String getNotice() {
        return notice;
    }

    public List<Artifact> getArtifacts() {
        Collections.sort(artifacts, (a1, a2) -> {
            return Objects.compare(deriveCompareString(a1), deriveCompareString(a2), String::compareToIgnoreCase);
        });

        return Collections.unmodifiableList(artifacts);
    }

    private String deriveCompareString(Artifact artifact) {
        return artifact.getId() + "-" + artifact.getVersion();
    }

    public String getDiscriminator() {
        return discriminator;
    }
}
