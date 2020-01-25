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
package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.extractor.InventoryExtractor;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.io.File;

/**
 * Simple container to aggregate information of an package.
 */
class PackageInfo {

    String id;
    String component;
    String version;
    String arch;
    String summary;
    String description;
    String license;
    String url;

    public Artifact createArtifact(File shareDir) {
        Artifact artifact = new Artifact();
        artifact.setId(id);
        artifact.setComponent(component);
        artifact.addProject(shareDir.getAbsolutePath());
        artifact.setVersion(version);

        artifact.set(InventoryExtractor.KEY_ATTRIBUTE_ARCHITECTURE, arch);
        artifact.set(InventoryExtractor.KEY_ATTRIBUTE_SUMMARY, summary);
        artifact.set(InventoryExtractor.KEY_ATTRIBUTE_DESCRIPTION, description);
        artifact.setUrl(url);
        artifact.set(InventoryExtractor.KEY_DERIVED_LICENSE_PACKAGE, license);
        return artifact;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, component=%s, version=%s, desc=%s]", getClass().getSimpleName(), id, component, version, description);
    }

}
