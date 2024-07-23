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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HexComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(HexComponentPatternContributor.class);
    private static final String MIX_PACKAGE_TYPE = "mix";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("mix.exs");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("mix.exs");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File mixFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();
        boolean inDepsBlock = false;

        if (!mixFile.exists()) {
            LOG.warn("Mix package file does not exist: {}", mixFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(mixFile.toPath())) {
            String packageName = null;
            String version = null;
            String repositoryUrl = null;
            String licenses = null;
            for (String line : lines.collect(Collectors.toList())) {
                if (line.contains("name:")) {
                    Matcher matcher = Pattern.compile("name:\\s*\"([^\"]+)\"").matcher(line);
                    if (matcher.find()) {
                        packageName = matcher.group(1);
                    }
                } else if (line.contains("version:")) {
                    Matcher matcher = Pattern.compile("version:\\s*\"([^\"]+)\"").matcher(line);
                    if (matcher.find()) {
                        version = matcher.group(1);
                        if ("@version".equals(version)) {
                            version = null;
                        }
                    }
                } else if (line.contains("source:")) {
                    Matcher matcher = Pattern.compile("source:\\s*\\[\\s*url:\\s*\"([^\"]+)\"").matcher(line);
                    if (matcher.find()) {
                        repositoryUrl = matcher.group(1);
                        if ("@scm_url".equals(repositoryUrl)) {
                            repositoryUrl = null;
                        }
                    }
                } else if (line.contains("@version") && version == null) {
                    Matcher matcher = Pattern.compile("@version\\s+\"([^\"]+)\"").matcher(line);
                    if (matcher.find()) {
                        version = matcher.group(1);
                    }
                } else if (line.contains("@scm_url") && repositoryUrl == null) {
                    Matcher matcher = Pattern.compile("@scm_url\\s+\"([^\"]+)\"").matcher(line);
                    if (matcher.find()) {
                        repositoryUrl = matcher.group(1);
                    }
                } else if (line.contains("licenses:")) {
                    Matcher matcher = Pattern.compile("licenses:\\s*\\[\\s*\"([^\"]+)\"").matcher(line);
                    if (matcher.find()) {
                        licenses = matcher.group(1);
                    }
                } else if (line.contains("defp deps do")) {
                    inDepsBlock = true;
                } else if (inDepsBlock && line.contains("{")) {
                    Pattern pattern = Pattern.compile("\\{:(\\w+),\\s*\"~> ([^\"]+)\",?\\s*(.*)?}");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        ComponentPatternData cpd = new ComponentPatternData();
                        String dependency = matcher.group(1);
                        String depVersion = matcher.group(2);
                        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, dependency);
                        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, depVersion);
                        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, dependency + "-" + depVersion);
                        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
                        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, dependency + "/**/*");
                        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
                        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, MIX_PACKAGE_TYPE);
                        cpd.set(Constants.KEY_NO_MATCHING_FILE, Constants.MARKER_CROSS);
                        cpd.set(Artifact.Attribute.PURL, buildPurl(dependency, depVersion));
                        components.add(cpd);
                    }
                } else if (inDepsBlock && line.endsWith("]")) {
                    inDepsBlock = false;
                }

                if (packageName != null && version != null && repositoryUrl != null && !inDepsBlock) {
                    ComponentPatternData cpd = new ComponentPatternData();
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName);
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName + "-" + version);
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                    cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, packageName + "/**/*");
                    cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
                    cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, MIX_PACKAGE_TYPE);
                    cpd.set(Constants.KEY_NO_MATCHING_FILE, Constants.MARKER_CROSS);
                    cpd.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, licenses);
                    cpd.set(Artifact.Attribute.PURL, buildPurl(packageName, version));

                    components.add(cpd);
                    packageName = null;
                    version = null;
                    repositoryUrl = null;
                    licenses = null;
                }
            }
            return components;
        } catch (IOException e) {
            LOG.warn("Error reading mix file: {}", mixFile.getAbsolutePath());
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private String buildPurl(String packageName, String version) {
        return String.format("pkg:hex/%s@%s", packageName, version);
    }
}
