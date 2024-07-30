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

public class LinuxKernelArchiveContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxKernelArchiveContributor.class);
    private static final String LINUX_KERNEL_TYPE = "linux-kernel";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("vmlinuz");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.contains("vmlinuz");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File kernelFile = new File(baseDir, relativeAnchorPath);
        List<ComponentPatternData> components = new ArrayList<>();

        if (!kernelFile.exists()) {
            LOG.warn("Linux kernel file does not exist: {}", kernelFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try {
            ElfFile elfFile = ElfFile.from(kernelFile);
            String kernelVersion = extractKernelVersion(elfFile);
            if (kernelVersion != null) {
                addComponent(components, kernelVersion, relativeAnchorPath, anchorChecksum);
            } else {
                LOG.warn("Kernel version not found in file: {}", kernelFile.getAbsolutePath());
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Error processing Linux kernel file", e);
            return Collections.emptyList();
        }
    }

    private String extractKernelVersion(ElfFile elfFile) {
        Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");

        for (int i = 0; i < elfFile.e_shnum; i++) {
            ElfSection section = elfFile.getSection(i);
            if (section instanceof ElfStringTable) {
                ElfStringTable stringTable = (ElfStringTable) section;
                for (int j = 0; j < stringTable.numStrings; j++) {
                    String string = stringTable.get(j);
                    Matcher matcher = versionPattern.matcher(string);
                    if (matcher.find()) {
                        return matcher.group();
                    }
                }
            }
        }
        return null;
    }

    private void addComponent(List<ComponentPatternData> components, String kernelVersion, String relativeAnchorPath, String anchorChecksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, "linux-kernel");
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, kernelVersion);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, "linux-kernel-" + kernelVersion);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, LINUX_KERNEL_TYPE);
        cpd.set(Artifact.Attribute.PURL, buildPurl(kernelVersion));

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

    private String buildPurl(String kernelVersion) {
        return "pkg:generic/linux-kernel@" + kernelVersion;
    }
}

