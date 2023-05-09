/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;

import java.io.File;

public class DefaultComponentPatternContributor extends ComponentPatternContributor {

    @Override
    public boolean applies(File contextBaseDir, String file, Artifact artifact) {
        return true;
    }

    @Override
    public void contribute(File contextBaseDir, String anchorFilePath, Artifact artifact, ComponentPatternData componentPatternData) {
        final File anchorFile = new File(contextBaseDir, anchorFilePath);
        final File anchorParentDir = anchorFile.getParentFile();

        // add missing if required
        if (StringUtils.isEmpty(artifact.getVersion())) {
            artifact.setVersion("unspecific");
        }
        if (StringUtils.isEmpty(artifact.getComponent())) {
            artifact.setComponent(anchorParentDir.getName());
        }

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, anchorParentDir.getName() + "/**/node_modules/**/*" + "," + anchorParentDir.getName() + "/**/bower_components/**/*");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/" + anchorParentDir.getName() + "/**/*");
    }
}
