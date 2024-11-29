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
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ExeComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(ExeComponentPatternContributor.class);
    private static final String EXE_SOURCE_TYPE = "exe";

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("/.text");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("/.text");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File detectedFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        File parentDir = detectedFile.getParentFile();
        while (!parentDir.getName().toLowerCase(Locale.ROOT).endsWith(".exe]")) {
            parentDir = parentDir.getParentFile();
        }

        String relativePath = FileUtils.asRelativePath(parentDir, detectedFile);

        addComponent(parentDir, components, relativePath, anchorChecksum);

        return components;
    }

    private void addComponent(File parentDir, List<ComponentPatternData> components, String relativeAnchorPath, String anchorChecksum) {
        final String name = parentDir.getName();
        final String exeName = name.substring(1, name.length() - 1);

        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, exeName);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");

        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, EXE_SOURCE_TYPE);

        components.add(cpd);
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private String buildPurl(String productName, String productVersion) {
        return "pkg:generic/" + productName + "@" + productVersion;
    }
}

