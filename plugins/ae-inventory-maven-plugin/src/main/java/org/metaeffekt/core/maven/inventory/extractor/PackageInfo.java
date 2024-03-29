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
package org.metaeffekt.core.maven.inventory.extractor;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.io.File;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;

/**
 * Simple container to aggregate information of an package.
 */
class PackageInfo {

    String id;
    String name;
    String component;
    String version;
    String arch;
    String summary;
    String description;
    String license;
    String url;
    String documentationDir;
    String licenseDir;
    String group;

    String status;

    public Artifact createArtifact(File shareDir) {
        Artifact artifact = new Artifact();
        artifact.setId(id);

        // the component information is either derived from group or the package name.
        if (!StringUtils.isNotBlank(component)) {
            if (StringUtils.isNotBlank(group)) {
                artifact.setComponent(group);
            } else if (StringUtils.isNotBlank(name)) {
                artifact.setComponent(name);
            } else {
                artifact.setComponent(name);
            }
        } else {
            artifact.setComponent(name);
        }

        artifact.setVersion(version);

        artifact.set(KEY_ARCHITECTURE, arch);
        artifact.set(KEY_SUMMARY, summary);
        artifact.set(KEY_DESCRIPTION, description);
        artifact.setUrl(url);
        artifact.set(KEY_DERIVED_LICENSE_PACKAGE, license);
        artifact.set(KEY_DOCUMENTATION_PATH_PACKAGE, documentationDir);
        artifact.set(KEY_LICENSE_PATH_PACKAGE, licenseDir);
        artifact.set(KEY_GROUP_PACKAGE, group);
        artifact.set(KEY_STATUS_PACKAGE, status);
        return artifact;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, component=%s, version=%s, desc=%s]", getClass().getSimpleName(), id, component, version, description);
    }

}
