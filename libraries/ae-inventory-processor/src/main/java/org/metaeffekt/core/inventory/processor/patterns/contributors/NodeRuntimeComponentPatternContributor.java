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
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class NodeRuntimeComponentPatternContributor extends ComponentPatternContributor {

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("/node/node_version.h");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        try {
            final File anchorFile = new File(baseDir, relativeAnchorPath);

            final String version = parseVersionFromAnchorFile(anchorFile);

            if (StringUtils.isNotEmpty(version)) {

                // construct component pattern
                final ComponentPatternData componentPatternData = new ComponentPatternData();
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorFile.getName());
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, "node");
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, "node-" + version);

                componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN,
                    "**/*,/usr/local/bin/node," +
                    "/usr/local/share/man/man1/node.*," +
                    "/usr/local/bin/nodejs," +
                    "/usr/local/share/doc/node/**/*," +
                    "/home/node/**/*");

                componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/node_modules/**/*");

                componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);

                return Collections.singletonList(componentPatternData);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    private static String parseVersionFromAnchorFile(File anchorFile) throws IOException {
        // parse file in lines
        final List<String> lines = FileUtils.readLines(anchorFile, FileUtils.ENCODING_UTF_8);

        // #define NODE_MAJOR_VERSION 20
        // #define NODE_MINOR_VERSION 9
        // #define NODE_PATCH_VERSION 0
        String major = null;
        String minor = null;
        String patch = null;

        final String majorPrefix = "#define NODE_MAJOR_VERSION ";
        final String minorPrefix = "#define NODE_MINOR_VERSION ";
        final String patchPrefix = "#define NODE_PATCH_VERSION ";
        for (String line : lines) {
            if (line.startsWith(majorPrefix)) {
               major = line.substring(majorPrefix.length()).trim();
           }
            if (line.startsWith(minorPrefix)) {
                minor = line.substring(minorPrefix.length()).trim();
            }
            if (line.startsWith(patchPrefix)) {
                patch = line.substring(patchPrefix.length()).trim();
            }

            if (major != null && minor != null && patch != null) break;
        }

        String version = null;
        if (major != null && minor != null && patch != null) {
            version = major + "." + minor + "." + patch;
        }
        return version;
    }
}