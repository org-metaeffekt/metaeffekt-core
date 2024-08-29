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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CocoapodsComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(CocoapodsComponentPatternContributor.class);
    private static final String COCOAPODS_PACKAGE_TYPE = "cocoapods";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".podspec");
    }});

    private static final Pattern SPEC_NAME = Pattern.compile(".*\\.name\\s*=\\s*['\"](.+?)['\"]");
    private static final Pattern PODSPEC_VERSION = Pattern.compile(".*\\.version\\s*=\\s*['\"](.+?)['\"]");


    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".podspec");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File podspecFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!podspecFile.exists()) {
            LOG.warn("Podspec file does not exist: {}", podspecFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(podspecFile.toPath())) {
            String packageName = null;
            String version = null;


            for (String line : lines.collect(Collectors.toList())) {
                Matcher nameMatcher = SPEC_NAME.matcher(line);
                Matcher versionMatcher = PODSPEC_VERSION.matcher(line);

                if (nameMatcher.find()) {
                    packageName = nameMatcher.group(1);
                }

                if (versionMatcher.find()) {
                    version = versionMatcher.group(1);
                }

                if (packageName != null && version != null) {
                    addComponent(components, packageName, version, relativeAnchorPath, anchorChecksum);
                    packageName = null;
                    version = null;
                }
            }

            return components;
        } catch (Exception e) {
            LOG.warn("Failure processing processing Podspec file: [{}]", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void addComponent(List<ComponentPatternData> components, String packageName, String version, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName + "-" + version);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, COCOAPODS_PACKAGE_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(packageName, version));

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

    private String buildPurl(String name, String version) {
        return "pkg:cocoapods/" + name + "@" + version;
    }
}

