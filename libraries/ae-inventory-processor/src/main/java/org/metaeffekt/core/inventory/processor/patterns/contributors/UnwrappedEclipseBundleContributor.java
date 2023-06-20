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

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;

import java.io.File;

public class UnwrappedEclipseBundleContributor extends ComponentPatternContributor {
    @Override
    public boolean applies(File contextBaseDir, String file, Artifact artifact) {
        return file.endsWith("about.html")
                || file.endsWith("about.ini")
                || file.endsWith("about.properties")
                || file.endsWith("about.mappings");
    }

    @Override
    public void contribute(File contextBaseDir, String anchorFile, Artifact artifact, ComponentPatternData componentPatternData) {
        System.out.println("Hello Eclipse Bundle: " + contextBaseDir + " / " + anchorFile);

        String id = contextBaseDir.getName();
        final int i = id.lastIndexOf("_");

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, id.substring(i + 1));
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, id.substring(0, i));
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, id);
        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/" + id + "/**/*");
    }
}