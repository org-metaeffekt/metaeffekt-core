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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnwrappedEclipseBundleContributor extends ComponentPatternContributor {
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("/about.html");
        add("/about.ini");
        add("/about.properties");
        add("/about.mappings");
    }});


    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("about.html")
                || pathInContext.endsWith("about.ini")
                || pathInContext.endsWith("about.properties")
                || pathInContext.endsWith("about.mappings");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile();

        final String id = contextBaseDir.getName();
        final int i = id.lastIndexOf("_");

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                FileUtils.asRelativePath(contextBaseDir, anchorFile));
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, id.substring(i + 1));
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, id.substring(0, i));
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, id);
        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/" + id + "/**/*");

        return Collections.singletonList(componentPatternData);
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }
}
