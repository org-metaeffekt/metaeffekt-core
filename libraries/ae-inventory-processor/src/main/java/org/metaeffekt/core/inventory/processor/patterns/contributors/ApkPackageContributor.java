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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.linux.LinuxDistributionInfo;
import org.metaeffekt.core.inventory.processor.linux.LinuxDistributionUtil;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.purl.PackageInfo;
import org.metaeffekt.core.util.purl.PackageManager;
import org.metaeffekt.core.util.purl.PurlUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ApkPackageContributor extends ComponentPatternContributor {

    private static final String APK_PACKAGE_TYPE = "apk";

    private static final List<String> suffixes = Collections.singletonList("lib/apk/db/installed");

    @Getter
    @Setter
    public static class ApkDbFileEntry {
        private String packageName;
        private String version;
        private String architecture;
        private String license;
        private String url;
        private String description;
        private String group;
    }

    @Override
    public boolean applies(String pathInContext) {
        // this contributor applies if it's the APK package database
        return pathInContext.endsWith("lib/apk/db/installed");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File apkDbFile = new File(baseDir, relativeAnchorPath);

        if (!apkDbFile.exists()) {
            log.warn("APK database file does not exist: [{}]", apkDbFile.getAbsolutePath());
            return Collections.emptyList();
        }

        String virtualRootPath = modulateVirtualRootPath(baseDir, relativeAnchorPath, suffixes);
        final LinuxDistributionUtil.LinuxDistro distro = LinuxDistributionUtil.parseDistro(new File(baseDir, virtualRootPath));

        try (Stream<String> lineStream = Files.lines(apkDbFile.toPath(), StandardCharsets.UTF_8)) {

            final List<ComponentPatternData> components = new ArrayList<>();

            // get path of virtual root
            final Path virtualRoot = new File(virtualRootPath).toPath();
            final Path relativeAnchorFile = new File(relativeAnchorPath).toPath();

            StringJoiner includePatterns = new StringJoiner(", ");
            includePatterns.add("lib/apk/db/**/*");
            String currentFolder = null;
            List<String> lines = lineStream.collect(Collectors.toList());
            ApkDbFileEntry apkDbFileEntry = new ApkDbFileEntry();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("P:")) {
                    apkDbFileEntry.setPackageName(line.substring(2).trim());
                } else if (line.startsWith("V:")) {
                    apkDbFileEntry.setVersion(line.substring(2).trim());
                } else if (line.startsWith("A:")) {
                    apkDbFileEntry.setArchitecture(line.substring(2).trim());
                } else if (line.startsWith("U:")) {
                    apkDbFileEntry.setUrl(line.substring(2).trim());
                } else if (line.startsWith("T:")) {
                    apkDbFileEntry.setDescription(line.substring(2).trim());
                } else if (line.startsWith("o:")) {
                    // use the source information (if available) as group (like in Debian extraction)
                    apkDbFileEntry.setGroup(line.substring(2).trim());
                } else if (line.startsWith("F:")) {
                    String folder = line.substring(2).trim();
                    if (currentFolder != null && folder.contains(currentFolder)) {
                        if (i + 1 < lines.size() && lines.get(i + 1).startsWith("R:")) {
                            // handle the case where the next line starts with "R:"
                            i++; // move to the next line which starts with "R:"
                            while (i < lines.size() && lines.get(i).startsWith("R:")) {
                                String fileName = lines.get(i).substring(2).trim();
                                includePatterns.add(folder + "/" + fileName);
                                currentFolder = folder;
                                i++;
                            }
                            i--; // adjust the index back since the loop increment will move it forward
                        } else {
                            if (i + 1 < lines.size() && lines.get(i + 1).startsWith("F:") && lines.get(i + 1).contains(folder)) {
                                // handle the case where the next line starts with "F:"
                                i++; // move to the next line which starts with "F:"
                                while (i < lines.size() && lines.get(i).startsWith("F:") && lines.get(i).contains(folder)) {
                                    currentFolder = lines.get(i).substring(2).trim();
                                    i++;
                                }
                                i--; // adjust the index back since the loop increment will move it forward
                            } else {
                                // check if the folder is actually a file
                                File rootPath = new File(baseDir, virtualRootPath);
                                File file = new File(rootPath, folder);
                                if (file.exists() && file.isFile() && !FileUtils.isSymlink(file)) {
                                    includePatterns.add(folder);
                                }
                            }
                        }
                    } else {
                        currentFolder = folder;
                    }

                } else if (line.startsWith("R:")) {
                    String fileName = line.substring(2).trim();
                    includePatterns.add(currentFolder + "/" + fileName);
                    if (i + 1 < lines.size() && lines.get(i + 1).startsWith("R:")) {
                        // handle the case where the next line starts with "R:"
                        i++; // move to the next line which starts with "R:"
                        while (i < lines.size() && lines.get(i).startsWith("R:")) {
                            String nextFileName = lines.get(i).substring(2).trim();
                            includePatterns.add(currentFolder + "/" + nextFileName);
                            i++;
                        }
                        i--; // adjust the index back since the loop increment will move it forward
                    }
                } else if (line.startsWith("L:")) {
                    apkDbFileEntry.setLicense(line.substring(2).trim());
                } else if (line.isEmpty()) {
                    // end of package block, process collected data

                    // determine the root of the distro (may be in an nested folder structure)
                    final String modulatedPath = modulateVirtualRootPath(baseDir, relativeAnchorPath, suffixes);
                    final String modulatedAnchorPath = FileUtils.asRelativePath(modulatedPath, relativeAnchorPath);

                    final String packageName = apkDbFileEntry.getPackageName();
                    if (packageName != null && apkDbFileEntry.getVersion() != null && apkDbFileEntry.getArchitecture() != null) {
                        if (includePatterns.length() > 0) {
                            processCollectedData(components, apkDbFileEntry, includePatterns.toString(), modulatedAnchorPath, anchorChecksum, distro);
                            includePatterns = new StringJoiner(", ");
                        } else {
                            log.warn("No include patterns found for package: [{}-{}-{}]", packageName, apkDbFileEntry.getVersion(), apkDbFileEntry.getArchitecture());
                            // FIXME: collect only package-specific folders
                            // FIXME: check names of folder (distribution-specific)
                            includePatterns.add("usr/share/doc/" + packageName + "/**/*");
                            includePatterns.add("usr/share/licenses/" + packageName + "/**/*");
                            includePatterns.add("usr/share/man/" + packageName + "/**/*");
                            processCollectedData(components, apkDbFileEntry, includePatterns.toString(), modulatedAnchorPath, anchorChecksum, distro);
                        }
                    }
                    currentFolder = null;
                    apkDbFileEntry = new ApkDbFileEntry();
                }
            }
            return components;
        } catch (Exception e) {
            log.warn("Could not process APK database file [{}]", apkDbFile.getAbsolutePath());
            return Collections.emptyList();
        }
    }

    private void processCollectedData(List<ComponentPatternData> components, ApkDbFileEntry apkDbFileEntry, String includePatterns, String path, String checksum, LinuxDistributionUtil.LinuxDistro distro) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, apkDbFileEntry.getPackageName());
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, apkDbFileEntry.getVersion());
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, apkDbFileEntry.getPackageName() + "-" + apkDbFileEntry.getVersion());
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, path);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePatterns);
        if (distro != null) {
            cpd.set(Constants.KEY_ISSUE, distro.issue);
            cpd.set(Constants.KEY_DISTRO_NAME, distro.name);
            cpd.set(Constants.KEY_DISTRO_VERSION, distro.versionId);
        }
        cpd.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, apkDbFileEntry.getLicense());
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_URL, apkDbFileEntry.getUrl());
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, LinuxDistributionInfo.ALPINE.getPackageTool());
        cpd.set(Constants.KEY_NO_FILE_MATCH_REQUIRED, Constants.MARKER_CROSS);
        cpd.set(Constants.KEY_ARCHITECTURE, apkDbFileEntry.getArchitecture());
        cpd.set(Constants.KEY_DESCRIPTION, apkDbFileEntry.getDescription());
        cpd.set(Constants.KEY_GROUP_PACKAGE, apkDbFileEntry.getGroup());
        cpd.set(Constants.KEY_STATUS_PACKAGE, "installed");
        cpd.set(Artifact.Attribute.PURL, buildPurl(apkDbFileEntry.getPackageName(), apkDbFileEntry.getVersion(), apkDbFileEntry.getArchitecture()));

        cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");
        cpd.set(ComponentPatternData.Attribute.SHARED_EXCLUDE_PATTERN, "**/dad, **/*.pub");

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

    private String buildPurl(String name, String version, String architecture) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        return PurlUtils.buildPurl(PackageInfo.of(PackageManager.APK, name).namespace(LinuxDistributionInfo.ALPINE.getPurlNamespace()).version(version).arch(architecture).build());
    }
}
