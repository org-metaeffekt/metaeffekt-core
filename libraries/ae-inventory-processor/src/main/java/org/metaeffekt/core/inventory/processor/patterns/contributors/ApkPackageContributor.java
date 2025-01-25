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
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class ApkPackageContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(ApkPackageContributor.class);

    private static final String APK_PACKAGE_TYPE = "apk";

    private static final List<String> suffixes = Collections.singletonList("lib/apk/db/installed");

    @Override
    public boolean applies(String pathInContext) {
        // this contributor applies if it's the APK package database
        return pathInContext.endsWith("lib/apk/db/installed");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File apkDbFile = new File(baseDir, relativeAnchorPath);

        if (!apkDbFile.exists()) {
            LOG.warn("APK database file does not exist: [{}]", apkDbFile.getAbsolutePath());
            return Collections.emptyList();
        }

        String virtualRootPath = modulateVirtualRootPath(baseDir, relativeAnchorPath, suffixes);

        try (Stream<String> lineStream = Files.lines(apkDbFile.toPath(), StandardCharsets.UTF_8)) {

            final List<ComponentPatternData> components = new ArrayList<>();

            // get path of virtual root
            final Path virtualRoot = new File(virtualRootPath).toPath();
            final Path relativeAnchorFile = new File(relativeAnchorPath).toPath();

            String packageName = null;
            String version = null;
            String architecture = null;
            String license = null;
            StringJoiner includePatterns = new StringJoiner(", ");
            includePatterns.add("lib/apk/db/**/*");
            String currentFolder = null;
            List<String> lines = lineStream.collect(Collectors.toList());
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("P:")) {
                    packageName = line.substring(2).trim();
                } else if (line.startsWith("V:")) {
                    version = line.substring(2).trim();
                } else if (line.startsWith("A:")) {
                    architecture = line.substring(2).trim();
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
                    license = line.substring(2).trim();
                } else if (line.isEmpty()) {
                    // end of package block, process collected data

                    // determine the root of the distro (may be in an nested folder structure)
                    final String modulatedPath = modulateVirtualRootPath(baseDir, relativeAnchorPath, suffixes);
                    final String modulatedAnchorPath = FileUtils.asRelativePath(modulatedPath, relativeAnchorPath);

                    if (packageName != null && version != null && architecture != null) {
                        if (includePatterns.length() > 0) {
                            processCollectedData(components, packageName, version, architecture, includePatterns.toString(), modulatedAnchorPath, anchorChecksum, license);
                            includePatterns = new StringJoiner(", ");
                        } else {
                            LOG.warn("No include patterns found for package: [{}-{}-{}]", packageName, version, architecture);
                            // FIXME: collect only package-specific folders
                            // FIXME: check names of folder (distribution-specific)
                            includePatterns.add("usr/share/doc/" + packageName + "/**/*");
                            includePatterns.add("usr/share/licenses/" + packageName + "/**/*");
                            includePatterns.add("usr/share/man/" + packageName + "/**/*");
                            processCollectedData(components, packageName, version, architecture, includePatterns.toString(), modulatedAnchorPath, anchorChecksum, license);
                        }
                    }
                    packageName = null;
                    version = null;
                    architecture = null;
                    currentFolder = null;
                    license = null;
                }
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Could not process APK database file [{}]", apkDbFile.getAbsolutePath());
            return Collections.emptyList();
        }
    }

    private void processCollectedData(List<ComponentPatternData> components, String packageName, String version,
                                      String architecture, String includePatterns, String path, String checksum,
                                      String license) {

        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName + "-" + version);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, path);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePatterns);
        cpd.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, license);
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, APK_PACKAGE_TYPE);
        cpd.set(Constants.KEY_NO_FILE_MATCH_REQUIRED, Constants.MARKER_CROSS);

        cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");
        cpd.set(ComponentPatternData.Attribute.SHARED_EXCLUDE_PATTERN, "**/dad, **/*.pub");

        cpd.set(Artifact.Attribute.PURL, buildPurl(packageName, version, architecture));
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
        return "pkg:apk/alpine/" + name + "@" + version + "?arch=" + architecture;
    }
}
