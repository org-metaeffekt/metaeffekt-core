/**
 * Copyright 2009-2017 the original author or authors.
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


/**
 * Data container for artifact and {@link LicenseMetaData} tuple.
 *
 * @author Karsten Klein
 */
public class ArtifactLicenseData {
    private final Artifact artifact;
    private final LicenseMetaData licenseMetaData;

    public ArtifactLicenseData(Artifact artifact, LicenseMetaData licenseMetaData) {
        this.artifact = artifact;
        this.licenseMetaData = licenseMetaData;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public LicenseMetaData getLicenseMetaData() {
        return licenseMetaData;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(licenseMetaData.getComponent());
        sb.append('/').append(artifact.getArtifactId());
        return sb.toString();
    }
}
