/*
 * Copyright 2009-2026 the original author or authors.
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GoLangComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(GoLangComponentPatternContributor.class);
    private static final String GOLANG_PACKAGE_TYPE = "golang";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("go.mod");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("go.mod");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        File goModFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!goModFile.exists()) {
            LOG.warn("GoLang module file does not exist: {}", goModFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(goModFile.toPath(), StandardCharsets.UTF_8)) {
            processGoModFile(lines, components, relativeAnchorPath, anchorChecksum);
        } catch (Exception e) {
            LOG.warn("Error processing GoLang module file", e);
        }

        return components;
    }

    private void processGoModFile(Stream<String> lines, List<ComponentPatternData> components, String relativeAnchorPath, String anchorChecksum) {
        AtomicBoolean inRequireBlock = new AtomicBoolean(false);
        Pattern pattern = Pattern.compile("^\\s*(\\S+)\\s+(\\S+).*");

        lines.forEach(line -> {
            line = line.trim();
            if (line.startsWith("require (")) {
                inRequireBlock.set(true);
            } else if (inRequireBlock.get() && line.startsWith(")")) {
                inRequireBlock.set(false);
            } else if (inRequireBlock.get()) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    String fullModuleName = matcher.group(1);
                    String version = matcher.group(2);
                    String moduleName = extractPackageName(fullModuleName);
                    addComponent(components, moduleName, version, relativeAnchorPath, anchorChecksum);
                }
            }
        });
    }

    private void addComponent(List<ComponentPatternData> components, String moduleName, String version, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, moduleName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, moduleName + "-" + version);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, new File(relativeAnchorPath).getName());
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, GOLANG_PACKAGE_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(moduleName, version));

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

    private String extractPackageName(String fullModuleName) {
        String[] parts = fullModuleName.split("/");
        return parts[parts.length - 1];
    }

    private String buildPurl(String moduleName, String version) {
        return "pkg:golang/" + moduleName + "@" + version;
    }
}


