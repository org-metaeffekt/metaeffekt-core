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

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MsiComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(MsiComponentPatternContributor.class);
    private static final String MSI_CST = "msi";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".rsrc/1033/version.txt");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".rsrc/1033/version.txt");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File versionFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!versionFile.exists()) {
            LOG.warn("MSI version file does not exist: {}", versionFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(versionFile.toPath(), StandardCharsets.UTF_16LE)) {
            String productName = null;
            String productVersion = null;

            Pattern productNamePattern = Pattern.compile("VALUE\\s+\"ProductName\"\\s*,\\s*\"([^\"]*)\"");
            Pattern productVersionPattern = Pattern.compile("VALUE\\s+\"ProductVersion\"\\s*,\\s*\"([^\"]*)\"");

            for (String line : lines.collect(Collectors.toList())) {
                Matcher nameMatcher = productNamePattern.matcher(line);
                if (nameMatcher.find()) {
                    productName = nameMatcher.group(1).replace("\0", "").trim();
                }

                Matcher versionMatcher = productVersionPattern.matcher(line);
                if (versionMatcher.find()) {
                    productVersion = versionMatcher.group(1).replace("\0", "").trim();
                }

                // If both values are found, we can stop processing further lines
                if (productName != null && productVersion != null) {
                    break;
                }
            }

            if (productName != null && productVersion != null) {
                addComponent(components, productName, productVersion, relativeAnchorPath, anchorChecksum);
            } else {
                LOG.warn("Could not find ProductName or ProductVersion in MSI version file: {}", versionFile.getAbsolutePath());
                return Collections.emptyList();
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Error reading MSI version file: {}", versionFile.getAbsolutePath(), e);
            return Collections.emptyList();
        }
    }

    private void addComponent(List<ComponentPatternData> components, String productName, String productVersion, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, productName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, productVersion);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, productName + "-" + productVersion);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, MSI_CST);
        cpd.set(Artifact.Attribute.PURL, buildPurl(productName, productVersion));

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
        return "pkg:msi/" + productName + "@" + productVersion;
    }
}

