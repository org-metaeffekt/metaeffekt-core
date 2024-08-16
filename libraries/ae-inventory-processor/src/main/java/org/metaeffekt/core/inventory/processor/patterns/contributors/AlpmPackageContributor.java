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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class AlpmPackageContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(AlpmPackageContributor.class);
    private static final String ALPM_PACKAGE_TYPE = "alpm";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("/desc");
    }});

    @Override
    public boolean applies(String pathInContext) {
        // this contributor applies if it's the ALPM package database directory
        return pathInContext.endsWith("/desc");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        final File packageDir = new File(baseDir, relativeAnchorPath).getParentFile();

        if (!packageDir.exists() || !packageDir.isDirectory()) {
            LOG.warn("ALPM package directory does not exist: {}", packageDir.getAbsolutePath());
            return Collections.emptyList();
        }

        try {
            final List<ComponentPatternData> components = new ArrayList<>();

            Path virtualRoot = new File(virtualRootPath).toPath();
            Path relativeAnchorFile = new File(relativeAnchorPath).toPath();

            String packageName = null;
            String version = null;
            String architecture = null;
            StringJoiner includePatterns = new StringJoiner(",");

            // add the parent directory of the package directory to the include patterns
            Path relativeParentPath = virtualRoot.relativize(new File(relativeAnchorPath).getParentFile().toPath());
            includePatterns.add(relativeParentPath + "/**/*");

            // read the desc file
            File descFile = new File(packageDir, "desc");
            if (descFile.exists()) {
                try (Stream<String> lines = Files.lines(descFile.toPath())) {
                    Iterator<String> iterator = lines.iterator();
                    while (iterator.hasNext()) {
                        String line = iterator.next();
                        if (line.startsWith("%NAME%")) {
                            if (iterator.hasNext()) {
                                String nextLine = iterator.next();
                                packageName = nextLine.trim();
                            }
                        } else if (line.startsWith("%VERSION%")) {
                            if (iterator.hasNext()) {
                                String nextLine = iterator.next();
                                version = nextLine.trim();
                            }
                        } else if (line.startsWith("%ARCH%")) {
                            if (iterator.hasNext()) {
                                String nextLine = iterator.next();
                                architecture = nextLine.trim();
                            }
                        }
                    }
                }
            }

            // read the files file
            File filesFile = new File(packageDir, "files");
            if (filesFile.exists()) {
                try (Stream<String> lines = Files.lines(filesFile.toPath())) {
                    Iterator<String> iterator = lines.iterator();
                    List<String> specificPaths = new ArrayList<>();

                    while (iterator.hasNext()) {
                        String currentLine = iterator.next().trim();

                        if (!currentLine.startsWith("%FILES%") && !currentLine.startsWith("%BACKUP%")) {
                            boolean isMostSpecific = true;
                            Iterator<String> existingPathsIterator = specificPaths.iterator();

                            while (existingPathsIterator.hasNext()) {
                                String existingPath = existingPathsIterator.next();

                                if (currentLine.startsWith(existingPath)) {
                                    // If currentLine is more specific, remove the broader existing path
                                    existingPathsIterator.remove();
                                } else if (existingPath.startsWith(currentLine)) {
                                    // If an existing path is more specific, skip adding currentLine
                                    isMostSpecific = false;
                                    break;
                                }
                            }

                            if (isMostSpecific) {
                                specificPaths.add(currentLine);
                            }
                        }
                    }

                    // Include specificPaths in includePatterns
                    for (String specificPath : specificPaths) {
                        File specificFile = new File(new File(baseDir, virtualRootPath), specificPath);
                        // check if the specific file exists and is not a symlink
                        if (specificFile.exists() && specificFile.isFile() && !FileUtils.isSymlink(specificFile)) {
                            includePatterns.add(specificPath);
                        }
                    }
                }
            }




            if (includePatterns.length() == 0) {
                LOG.warn("No include patterns found for package: [{}]-[{}]-[{}]", packageName, version, architecture);
                includePatterns.add(relativeAnchorPath);
            }

            if (packageName != null && version != null && architecture != null) {
                processCollectedData(components, packageName, version, architecture, includePatterns.toString(), virtualRoot.relativize(relativeAnchorFile).toString(), anchorChecksum);
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Failure processing ALPM package directory [{}]: [{}]", packageDir.getAbsolutePath(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private void processCollectedData(List<ComponentPatternData> components, String packageName, String version, String architecture, String includePatterns, String path, String checksum) {
        ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName + "-" + version);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, path);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePatterns);
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, ALPM_PACKAGE_TYPE);
        cpd.set(Constants.KEY_NO_MATCHING_FILE, Constants.MARKER_CROSS);

        cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");

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
        return "pkg:alpm/arch/" + name + "@" + version + "?arch=" + architecture;
    }
}

