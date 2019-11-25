/**
 * Copyright 2009-2019 the original author or authors.
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
import java.util.List;

/**
 * Data container for artifacts and {@link LicenseMetaData}.
 *
 * @author Karsten Klein
 */
public class ArtifactLicenseData {
    private final List<Artifact> artifacts;
    private final LicenseMetaData licenseMetaData;
    private final String componentName;
    private final String componentVersion;

    public ArtifactLicenseData(String componentName, String componentVersion, LicenseMetaData licenseMetaData) {
        this.licenseMetaData = licenseMetaData;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.artifacts = new ArrayList<>();
    }

    public LicenseMetaData getLicenseMetaData() {
        return licenseMetaData;
    }

    public void add(Artifact artifact) {
        artifacts.add(artifact);
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public String deriveId() {
        return normalizeTokenId(LicenseMetaData.normalizeId(licenseMetaData.deriveQualifier()));
    }

    public static String normalizeTokenId(String string) {
        String result = string.replace(" ", "-");
        result = result.replace("(", "");
        result = result.replace(")", "");
        return result;
    }

    public String getLicenseInEffect() {
        return licenseMetaData.deriveLicenseInEffect();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(licenseMetaData.getComponent());
        return sb.toString();
    }
}
