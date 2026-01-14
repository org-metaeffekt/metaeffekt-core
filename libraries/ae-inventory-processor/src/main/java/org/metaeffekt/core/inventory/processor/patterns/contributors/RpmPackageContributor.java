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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.linux.LinuxDistributionUtil;
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
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;

public class RpmPackageContributor extends ComponentPatternContributor {
    private static final Logger LOG = LoggerFactory.getLogger(RpmPackageContributor.class);
    private static final String RPM_TYPE = "rpm";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("/packages");
        add("/rpmdb.sqlite");
        add("/packages.db");
    }});

    private static final List<String> PATH_FRAGMENTS = new ArrayList<String>() {{
        add("var/lib/");
        add("usr/lib/");
    }};

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("/Packages") || pathInContext.endsWith("/rpmdb.sqlite") || pathInContext.endsWith("/Packages.db");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        File packagesFile = new File(baseDir, relativeAnchorPath);

        if (!packagesFile.exists()) {
            LOG.warn("RPM packages file does not exist: [{}]", packagesFile.getAbsolutePath());
            return Collections.emptyList();
        }

        String virtualRootPath = modulateVirtualRootPath(baseDir, relativeAnchorPath, PATH_FRAGMENTS);

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
                    ComponentPatternData cpd = new ComponentPatternData();
                    Artifact artifact = new Artifact();
                    List<IndexEntry> indexEntries = RPMDBUtils.headerImport(entry.getValue());
                    PackageInfo packageInfo = RPMDBUtils.getNEVRA(indexEntries);
                    StringJoiner includePatternJoiner = new StringJoiner(", ");

                    artifact.setId(packageInfo.getName() + "-" + packageInfo.getVersion());

                    final File distroBaseDir = new File(baseDir, virtualRootPath);
                    final LinuxDistributionUtil.LinuxDistro distro = LinuxDistributionUtil.parseDistro(distroBaseDir);

                    try {
                        final List<String> installedFileNames = packageInfo.installedFileNames();
                        if (installedFileNames != null && !installedFileNames.isEmpty()) {
                            for (String installedFileName : installedFileNames) {

                                // NOTE: we must use the relative path from the perspective of the version anchor.
                                installedFileName = installedFileName.startsWith("/") ? installedFileName.substring(1) : installedFileName;

                                final File file = new File(baseDir, virtualRootPath + "/" + installedFileName);
                                if (file.exists() && file.isFile() && !FileUtils.isSymlink(file)) {
                                    includePatternJoiner.add(installedFileName);
                                }
                            }
                        }
                        if (includePatternJoiner.length() == 0) {
                            throw new IllegalStateException("No files found for rpm-package: " + packageInfo.getName());
                        }
                    } catch (Exception e) { // FIXME: what kind of exceptions happen here; also observed NPE
                        LOG.warn("Could not derive include patterns for rpm-package: [{}]", packageInfo.getName());

                        // include even, when there is no file match
                        cpd.set(Constants.KEY_NO_FILE_MATCH_REQUIRED, Constants.MARKER_CROSS);
                    }

                    // NOTE: never add **/*; only add files which may contribute to the package from known locations
                    // NOTE: as of now (03.09.2024) we have no complications
                    includePatternJoiner.add("usr/share/doc/" + packageInfo.getName() + "/**/*");
                    includePatternJoiner.add("usr/share/licenses/" + packageInfo.getName() + "/**/*");
                    includePatternJoiner.add("usr/share/man/**/" + packageInfo.getName() + "*");

                    cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageInfo.getName());
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, packageInfo.getVersion());
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageInfo.getName() + "-" + packageInfo.getVersion());
                    cpd.set(Artifact.Attribute.CHECKSUM, packageInfo.getSigMD5());
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, virtualRoot.relativize(relativeAnchorFile).toString());
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                    cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, includePatternJoiner.toString());

                    cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar,**/node_modules/**/*");

                    cpd.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, packageInfo.getLicense());
                    cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
                    cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, RPM_TYPE);
                    cpd.set(Artifact.Attribute.PURL, buildPurl(packageInfo.getName(),
                            ((StringUtils.isNotBlank(packageInfo.getRelease())) ?
                                packageInfo.getVersion() + "-" + packageInfo.getRelease() :
                                packageInfo.getVersion()),
                            packageInfo.getArch(), packageInfo.getEpoch(),
                            packageInfo.getSourceRpm(), distro));

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

    private String buildPurl(String name, String version, String arch, Integer epoch, String upstream, LinuxDistributionUtil.LinuxDistro distro) {
        if (distro == null || distro.id == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("pkg:rpm/");

        // FIXME: this is a hack; we should use the distro id as is or have a dedicated mapping con
        if ("rhel".equals(distro.id)) {
            sb.append("redhat/");
        } else {
            sb.append(distro.id).append("/");
        }
        sb.append(name).append("@");
        sb.append(version);
        if (StringUtils.isNotBlank(arch)) {
            sb.append("?arch=").append(arch);
        }
        if (epoch != null) {
            sb.append("&epoch=").append(epoch);
        }
        if (StringUtils.isNotBlank(upstream)) {
            sb.append("&upstream=").append(upstream);
        }
        if (StringUtils.isNotBlank(distro.versionId)) {
            sb.append("&distro=").append(distro.id).append("-").append(distro.versionId);
        }
        return sb.toString();
    }

}
