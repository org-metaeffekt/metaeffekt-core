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
package org.metaeffekt.core.inventory.resolver;

/**
 * Artifact Pattern to identify certain artifacts in an inventory. The {@link ArtifactPattern} class can be used to
 * identify defined groups of artifacts for processing.
 */
public class ArtifactPattern {

    private String artifactPattern;
    private String componentPattern;
    private String versionPattern;
    private String effectiveLicensePattern;

    public ArtifactPattern(String artifactPattern, String componentPattern, String versionPattern, String effectiveLicensePattern) {
        this.artifactPattern = artifactPattern;
        this.componentPattern = componentPattern;
        this.versionPattern = versionPattern;
        this.effectiveLicensePattern = effectiveLicensePattern;
    }

    public static String deriveQualifier(String artifact, String component, String version, String effectiveLicense) {
        return artifact + "-" + component + "-" + version + "-" + effectiveLicense;
    }

    public String getArtifactPattern() {
        return artifactPattern;
    }

    public String getComponentPattern() {
        return componentPattern;
    }

    public String getVersionPattern() {
        return versionPattern;
    }

    public String getEffectiveLicensePattern() {
        return effectiveLicensePattern;
    }

    public String getQualifier() {
        return deriveQualifier(artifactPattern, componentPattern, versionPattern, effectiveLicensePattern);
    }
}
