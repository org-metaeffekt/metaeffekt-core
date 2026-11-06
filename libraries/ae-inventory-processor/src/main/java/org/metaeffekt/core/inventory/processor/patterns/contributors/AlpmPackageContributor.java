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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class AlpmPackageContributor extends ComponentPatternContributor {

    private static final String ALPM_PACKAGE_TYPE = "alpm";

    private static final List<String> SUFFIXES = Collections.singletonList("/desc");

    private static final List<String> PATHS = Collections.singletonList("var/lib");

    @Getter
    @Setter
    public static class AlpmDescFileEntry {
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
        // this contributor applies if it's the ALPM package database directory
        return pathInContext.endsWith("/desc");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File packageDir = new File(baseDir, relativeAnchorPath).getParentFile();

        if (!packageDir.exists() || !packageDir.isDirectory()) {
            log.warn("ALPM package directory does not exist: {}", packageDir.getAbsolutePath());
            return Collections.emptyList();
        }

        try {
            final List<ComponentPatternData> components = new ArrayList<>();
            final String virtualRootPath = modulateVirtualRootPath(baseDir, relativeAnchorPath, PATHS);
            final File distroBaseDir = new File(baseDir, virtualRootPath);
            final AlpmDescFileEntry alpmDescFileEntry = new AlpmDescFileEntry();
            final LinuxDistributionUtil.LinuxDistro distro = LinuxDistributionUtil.parseDistro(distroBaseDir);

            Path virtualRoot = new File(virtualRootPath).toPath();
            Path relativeAnchorFile = new File(relativeAnchorPath).toPath();
            StringJoiner includePatterns = new StringJoiner(", ");

            // add the parent directory of the package directory to the include patterns
            Path relativeParentPath = virtualRoot.relativize(new File(relativeAnchorPath).getParentFile().toPath());
            includePatterns.add("**/*");

            // read the desc file
            final File descFile = new File(packageDir, "desc");
            if (descFile.exists()) {
                parseDescFile(descFile, alpmDescFileEntry);
            }

            // read the files file
            final File filesFile = new File(packageDir, "files");
            if (filesFile.exists()) {
                parseFilesFile(filesFile, distroBaseDir, includePatterns);
            }

            if (includePatterns.length() == 0) {
                log.warn("No include patterns found for package: [{}]-[{}]-[{}]", alpmDescFileEntry.getPackageName(), alpmDescFileEntry.getVersion(), alpmDescFileEntry.getArchitecture());
                includePatterns.add(relativeAnchorPath);
            }

            if (alpmDescFileEntry.getPackageName() != null && alpmDescFileEntry.getVersion() != null && alpmDescFileEntry.getArchitecture() != null) {
                processCollectedData(components, alpmDescFileEntry, includePatterns.toString(), virtualRoot.relativize(relativeAnchorFile).toString(), anchorChecksum, distro);
            }
            return components;
        } catch (Exception e) {
            log.warn("Failure processing ALPM package directory [{}]: [{}]", packageDir.getAbsolutePath(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private static void parseDescFile(File descFile, AlpmDescFileEntry alpmDescFileEntry) throws IOException {
        try (Stream<String> lines = Files.lines(descFile.toPath())) {
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line.startsWith("%NAME%")) {
                    if (iterator.hasNext()) {
                        String nextLine = iterator.next();
                        alpmDescFileEntry.setPackageName(nextLine.trim());
                    }
                } else if (line.startsWith("%VERSION%")) {
                    if (iterator.hasNext()) {
                        String nextLine = iterator.next();
                        alpmDescFileEntry.setVersion(nextLine.trim());
                    }
                } else if (line.startsWith("%ARCH%")) {
                    if (iterator.hasNext()) {
                        String nextLine = iterator.next();
                        alpmDescFileEntry.setArchitecture(nextLine.trim());
                    }
                } else if (line.startsWith("%LICENSE%")) {
                    if (iterator.hasNext()) {
                        String nextLine = iterator.next();
                        alpmDescFileEntry.setLicense(nextLine.trim());
                    }
                } else if (line.startsWith("%URL%")) {
                    if (iterator.hasNext()) {
                        String nextLine = iterator.next();
                        alpmDescFileEntry.setUrl(nextLine.trim());
                    }
                } else if (line.startsWith("%DESC%")) {
                    if (iterator.hasNext()) {
                        String nextLine = iterator.next();
                        alpmDescFileEntry.setDescription(nextLine.trim());
                    }
                } else if (line.startsWith("%GROUPS%")) {
                    if (iterator.hasNext()) {
                        String nextLine = iterator.next();
                        alpmDescFileEntry.setGroup(nextLine.trim());
                    }
                }
            }
        }
    }

    private static void parseFilesFile(File filesFile, File distroBaseDir, StringJoiner includePatterns) throws IOException {
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
                            // if currentLine is more specific, remove the broader existing path
                            existingPathsIterator.remove();
                        } else if (existingPath.startsWith(currentLine)) {
                            // if an existing path is more specific, skip adding currentLine
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
                File specificFile = new File(distroBaseDir, specificPath);
                // check if the specific file exists and is not a symlink
                if (specificFile.exists() && specificFile.isFile() && !FileUtils.isSymlink(specificFile)) {
                    includePatterns.add(specificPath);
                }
            }
        }
    }

    private void processCollectedData(List<ComponentPatternData> components, AlpmDescFileEntry alpmDescFileEntry, String includePatterns, String path, String checksum, LinuxDistributionUtil.LinuxDistro distro) {
        final ComponentPatternData cpd = new ComponentPatternData();
        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, alpmDescFileEntry.getPackageName());
        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, alpmDescFileEntry.getVersion());
        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, alpmDescFileEntry.getPackageName() + "-" + alpmDescFileEntry.getVersion());
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, path);
        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);
        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePatterns);
        if (distro != null) {
            cpd.set(Constants.KEY_ISSUE, distro.issue);
            cpd.set(Constants.KEY_DISTRO_NAME, distro.name);
            cpd.set(Constants.KEY_DISTRO_VERSION, distro.versionId);
        }
        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, ALPM_PACKAGE_TYPE);
        cpd.set(Constants.KEY_NO_FILE_MATCH_REQUIRED, Constants.MARKER_CROSS);
        cpd.set(Constants.KEY_ARCHITECTURE, alpmDescFileEntry.getArchitecture());
        cpd.set(Constants.KEY_URL, alpmDescFileEntry.getUrl());
        cpd.set(Constants.KEY_DESCRIPTION, alpmDescFileEntry.getDescription());
        cpd.set(Constants.KEY_GROUP_PACKAGE, alpmDescFileEntry.getGroup());
        cpd.set(Constants.KEY_STATUS_PACKAGE, "installed");

        cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");

        cpd.set(Artifact.Attribute.PURL, buildPurl(alpmDescFileEntry.getPackageName(), alpmDescFileEntry.getVersion(), alpmDescFileEntry.getArchitecture()));
        components.add(cpd);
    }

    @Override
    public List<String> getSuffixes() {
        return SUFFIXES;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private String buildPurl(String name, String version, String architecture) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        return PurlUtils.buildPurl(PackageInfo.of(PackageManager.ALPM, name).namespace(LinuxDistributionInfo.ARCH.getPurlNamespace()).version(version).arch(architecture).build());
    }
}

