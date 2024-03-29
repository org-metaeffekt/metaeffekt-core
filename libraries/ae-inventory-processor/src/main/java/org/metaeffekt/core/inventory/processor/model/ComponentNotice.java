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
package org.metaeffekt.core.inventory.processor.model;

import java.util.*;

/**
 * Aggregates and structures all information to render notices for a given components.
 * <p>
 * Component notices are on the first level version-agnostic. They organize contain various artifacts of different
 * versions that share the component name.
 */
public class ComponentNotice {

    private final String componentName;

    private String license;

    private final Map<String, ArtifactNotice> noticeArtifactNoticeMap;

    public ComponentNotice(String componentName, String license) {
        this.componentName = componentName;
        this.noticeArtifactNoticeMap = new HashMap<>();
        this.license = license;
    }

    public String getComponentName() {
        return componentName;
    }

    public List<ArtifactNotice> getArtifactNotices() {
        List<ArtifactNotice> artifactNotices = new ArrayList<>(noticeArtifactNoticeMap.values());
        Collections.sort(artifactNotices,
                (an1, an2) -> Objects.compare(an1.getDiscriminator(), an2.getDiscriminator(), String::compareToIgnoreCase));
        return Collections.unmodifiableList(artifactNotices);
    }

    public boolean add(Artifact artifact, LicenseMetaData licenseMetaData) {
        String notice = licenseMetaData.getCompleteNotice();
        if (notice == null) notice = "";

        ArtifactNotice artifactNotice = noticeArtifactNoticeMap.get(notice);
        if (artifactNotice == null) {
            String discriminator = deriveDiscriminator(licenseMetaData);
            artifactNotice = new ArtifactNotice(notice, discriminator);
            license = licenseMetaData.getLicense();
            noticeArtifactNoticeMap.put(notice, artifactNotice);
        }

        return artifactNotice.add(artifact);
    }

    public boolean add(Artifact artifact) {
        final String notice = "";
        ArtifactNotice artifactNotice = noticeArtifactNoticeMap.get(notice);
        if (artifactNotice == null) {
            String discriminator = "";
            artifactNotice = new ArtifactNotice(notice, discriminator);
            noticeArtifactNoticeMap.put(notice, artifactNotice);
        }

        return artifactNotice.add(artifact);
    }

    private String deriveDiscriminator(LicenseMetaData licenseMetaData) {
        String discriminator = licenseMetaData.get("Discriminator");
        discriminator = discriminator == null ? discriminator = "" : " " + discriminator;
        return discriminator;
    }

    public String getLicense() {
        return license;
    }
}
