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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.patterns.ComponentPatternProducer.localeConstants.PATH_LOCALE;

public class JettyComponentPatternContributor extends ComponentPatternContributor {

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("/jetty/version.txt");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.toLowerCase(PATH_LOCALE).endsWith("/jetty/version.txt");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        try {
            final File anchorFile = new File(baseDir, relativeAnchorPath);

            final String version = parseVersionFromVersionFile(anchorFile);

            if (StringUtils.isNotEmpty(version)) {

                // construct component pattern
                final ComponentPatternData componentPatternData = new ComponentPatternData();
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorFile.getName());
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, "Jetty");
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, "jetty-" + version);

                componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");

                componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/node_modules/**/*,**/*.jar");

                componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);

                return Collections.singletonList(componentPatternData);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    private static String parseVersionFromVersionFile(File anchorFile) throws IOException {
        // parse file in lines
        final List<String> lines = FileUtils.readLines(anchorFile, FileUtils.ENCODING_UTF_8);

        for (String line : lines) {
            final String trim = line.trim();
            if (trim.startsWith("jetty-")) {
                String version = trim.substring("jetty-".length());
                int index = version.indexOf(" - ");
                version = version.substring(0, index);

                return version;
            }
        }
        return null;
    }
}
