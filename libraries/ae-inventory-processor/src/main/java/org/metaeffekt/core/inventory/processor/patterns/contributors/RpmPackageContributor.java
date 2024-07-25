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
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.Database;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.Entry;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb.BerkeleyDB;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb.IndexEntry;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb.PackageInfo;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb.RPMDBUtils;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.ndb.NDB;
import org.metaeffekt.core.inventory.processor.patterns.contributors.util.sqlite3.SQLite3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RpmPackageContributor extends ComponentPatternContributor {
    private static final Logger LOG = LoggerFactory.getLogger(RpmPackageContributor.class);
    private static final String RPM_TYPE = "rpm";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("/packages");
        add("/rpmdb.sqlite");
        add("/packages.db");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("/Packages") || pathInContext.endsWith("/rpmdb.sqlite") || pathInContext.endsWith("/Packages.db");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File packagesFile = new File(baseDir, relativeAnchorPath);

        if (!packagesFile.exists()) {
            LOG.warn("RPM packages file does not exist: [{}]", packagesFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try {
            BlockingQueue<Entry> entries = getEntries(packagesFile);
            Entry entry;
            List<ComponentPatternData> components = new ArrayList<>();
            Path virtualRoot = new File(virtualRootPath).toPath();
            Path relativeAnchorFile = new File(relativeAnchorPath).toPath();
            while (true) {
                try {
                    entry = entries.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Thread interrupted while waiting for entries", e);
                    break;
                }
                if (entry.getValue() == null && entry.getError() == null) {
                    // sentinel value encountered, processing is complete
                    break;
                }
                if (entry.getError() != null) {
                    LOG.warn("Could not read entry:", entry.getError());
                    break;
                }
                if (entry.getValue() != null) {
                    List<IndexEntry> indexEntries = RPMDBUtils.headerImport(entry.getValue());
                    PackageInfo packageInfo = RPMDBUtils.getNEVRA(indexEntries);
                    StringJoiner sj = new StringJoiner(",");
                    String distro = getDistro(baseDir.getAbsolutePath(), virtualRootPath);
                    try {
                        List<String> installedFileNames = packageInfo.installedFileNames();
                        if (installedFileNames != null && !installedFileNames.isEmpty()) {
                            for (String installedFileName : installedFileNames) {

                                // NOTE: we must use the relative path from the perspective of the version anchor.
                                installedFileName = installedFileName.startsWith("/") ? installedFileName.substring(1) : installedFileName;

                                File file = new File(baseDir, virtualRootPath + "/" + installedFileName);
                                if (file.exists() && file.isFile()) {
                                    sj.add(installedFileName);
                                }
                            }
                        }
                        if (sj.length() == 0) {
                            throw new Exception("No files found for rpm-package: " + packageInfo.getName());
                        }
                    } catch (Exception e) { // FIXME: what kind of exceptions happen here; also observed NPE
                        LOG.warn("Could not include patterns for rpm-package: [{}]", packageInfo.getName());

                        // NOTE: never add **/*; only add files which may contribute to the package from known locations
                        // FIXME: check names of folder (distribution-specific)
                        sj.add("usr/share/doc/" + packageInfo.getName() + "/**/*");
                        sj.add("usr/share/licenses/" + packageInfo.getName() + "/**/*");
                        sj.add("usr/share/man/**/" + packageInfo.getName() + "*");
                    }

                    ComponentPatternData cpd = new ComponentPatternData();
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageInfo.getName());
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, packageInfo.getVersion());
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageInfo.getName() + "-" + packageInfo.getVersion());
                    cpd.set(Artifact.Attribute.CHECKSUM, packageInfo.getSigMD5());
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, virtualRoot.relativize(relativeAnchorFile).toString());
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                    cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, sj.toString());

                    cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar,**/node_modules/**/*");

                    cpd.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, packageInfo.getLicense());
                    cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
                    cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, RPM_TYPE);
                    cpd.set(Artifact.Attribute.PURL, buildPurl(packageInfo.getName(), packageInfo.getVersion() + "-" + packageInfo.getRelease(), packageInfo.getArch(), packageInfo.getEpoch(), packageInfo.getSourceRpm(), distro));

                    components.add(cpd);
                }
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Could not read RPM packages file", e);
            return Collections.emptyList();
        }
    }

    private static BlockingQueue<Entry> getEntries(File packagesFile) {
        Database db;
        if (packagesFile.toPath().endsWith("Packages")) {
            db = new BerkeleyDB(null, null);
        } else if (packagesFile.toPath().endsWith("rpmdb.sqlite")) {
            db = new SQLite3(null);
        } else if (packagesFile.toPath().endsWith("Packages.db")) {
            db = new NDB(null, null);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + packagesFile.getAbsolutePath());
        }
        db = db.open(packagesFile.getAbsolutePath());
        return db.read();
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private String buildPurl(String name, String version, String arch, Integer epoch, String upstream, String distro) {
        StringBuilder sb = new StringBuilder();
        sb.append("pkg:rpm/");
        String newVendor = distro.replaceAll("[^a-z]", "");
        sb.append(newVendor).append("/");
        sb.append(name).append("@");
        sb.append(version);
        if (arch != null && !arch.isEmpty()) {
            sb.append("?arch=").append(arch);
        }
        if (epoch != null) {
            sb.append("&epoch=").append(epoch);
        }
        if (upstream != null && !upstream.isEmpty()) {
            sb.append("&upstream=").append(upstream);
        }
        if (!distro.isEmpty()) {
            sb.append("&distro=").append(distro);
        }
        return sb.toString();
    }

    private String parseRedHatRelease(File file) {
        Pattern pattern = Pattern.compile("(.*?)\\srelease\\s(\\d\\.\\d+)");
        try (Stream<String> lines = Files.lines(file.toPath())) {
            for (String line : lines.collect(Collectors.toList())) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if (matcher.groupCount() >= 2) {
                        String[] words = matcher.group(1).split("\\s+");
                        StringBuilder initials = new StringBuilder();
                        for (String word : words) {
                            initials.append(Character.toLowerCase(word.charAt(0)));
                        }
                        return initials + "-" + matcher.group(2);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not read redhat-release file", e);
            return null;
        }
        return null;
    }

    private String getDistro(String baseDir, String virtualRootPath) {
        List<Path> paths = new ArrayList<>(Arrays.asList(
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/os-release"),
                Paths.get(baseDir + "/" + virtualRootPath + "/usr/lib/os-release"),
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/system-release-cpe"),
                Paths.get(baseDir + "/" + virtualRootPath + "/etc/redhat-release")
        ));
        for (Path path : paths) {
            if (path.endsWith("os-release")) {
                File file = path.toFile();
                if (file.exists()) {
                    String distro = parseOsRelease(file);
                    if (distro != null && !distro.isEmpty()) {
                        return distro;
                    }
                }
            }
            if (path.endsWith("system-release-cpe")) {
                File file = path.toFile();
                if (file.exists()) {
                    return parseSystemReleaseCpe(file);
                }
            }
            if (path.endsWith("redhat-release")) {
                File file = path.toFile();
                if (file.exists()) {
                    return parseRedHatRelease(file);
                }
            }
        }
        return "";
    }

    private String parseOsRelease(File file) {
        StringBuilder distro = new StringBuilder();
        try (Stream<String> lines = Files.lines(file.toPath())) {
            for (String line : lines.collect(Collectors.toList())) {
                if (line.startsWith("ID=")) {
                    distro.append(line.substring(3).replaceAll("\"", ""));
                }
                if (line.startsWith("VERSION_ID=")) {
                    distro.append("-").append(line.substring(11).replaceAll("\"", ""));
                }
            }
            return distro.toString();
        } catch (Exception e) {
            LOG.warn("Could not read os-release file", e);
            return null;
        }
    }

    private String parseSystemReleaseCpe(File file) {
        Pattern pattern = Pattern.compile("cpe:/o:(.*?):.*?:(.*?):.*?$");
        try (Stream<String> lines = Files.lines(file.toPath())) {
            for (String line : lines.collect(Collectors.toList())) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not read system-release-cpe file", e);
            return null;
        }
        return null;
    }
}
