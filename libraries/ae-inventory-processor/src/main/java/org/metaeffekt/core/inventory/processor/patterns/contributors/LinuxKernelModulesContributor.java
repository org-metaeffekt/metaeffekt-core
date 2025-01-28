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

import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSection;
import net.fornwall.jelf.ElfStringTable;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxKernelModulesContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxKernelModulesContributor.class);
    private static final String LINUX_KERNEL_MODULE_TYPE = "linux-kernel-module";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".ko");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".ko");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        File moduleFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!moduleFile.exists()) {
            LOG.warn("Linux kernel module file does not exist: {}", moduleFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try {
            ElfFile elfFile = ElfFile.from(moduleFile);
            String moduleName = extractModuleName(elfFile);
            String moduleVersion = extractModuleVersion(elfFile);

            if (moduleName != null && moduleVersion != null) {
                addComponent(components, moduleName, moduleVersion, relativeAnchorPath, anchorChecksum);
            } else {
                LOG.warn("Module name or version not found in file: {}", moduleFile.getAbsolutePath());
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Error processing Linux kernel module file", e);
            return Collections.emptyList();
        }
    }

    private String extractModuleName(ElfFile elfFile) {
        Pattern namePattern = Pattern.compile("^name=(.*)$", Pattern.MULTILINE);

        for (int i = 0; i < elfFile.e_shnum; i++) {
            ElfSection section = elfFile.getSection(i);
            if (section instanceof ElfStringTable) {
                ElfStringTable stringTable = (ElfStringTable) section;
                for (int j = 0; j < stringTable.numStrings; j++) {
                    String string = stringTable.get(j);
                    Matcher matcher = namePattern.matcher(string);
                    if (matcher.find()) {
                        return matcher.group(1).trim();
                    }
                }
            }
        }
        return null;
    }

    private String extractModuleVersion(ElfFile elfFile) {
        Pattern versionPattern = Pattern.compile("^vermagic=(.*)$", Pattern.MULTILINE);

        for (int i = 0; i < elfFile.e_shnum; i++) {
            ElfSection section = elfFile.getSection(i);
            if (section instanceof ElfStringTable) {
                ElfStringTable stringTable = (ElfStringTable) section;
                for (int j = 0; j < stringTable.numStrings; j++) {
                    String string = stringTable.get(j);
                    Matcher matcher = versionPattern.matcher(string);
                    if (matcher.find()) {
                        return matcher.group(1).trim();
                    }
                }
            }
        }
        return null;
    }

    private void addComponent(List<ComponentPatternData> components, String moduleName, String moduleVersion, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, moduleName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, moduleVersion);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, moduleName + "-" + moduleVersion);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, LINUX_KERNEL_MODULE_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(moduleName, moduleVersion));

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

    private String buildPurl(String moduleName, String moduleVersion) {
        return "pkg:generic/linux-kernel-module/" + moduleName + "@" + moduleVersion;
    }
}

