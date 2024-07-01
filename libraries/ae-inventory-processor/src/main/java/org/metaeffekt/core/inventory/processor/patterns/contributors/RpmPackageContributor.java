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
                    LOG.warn("Error reading entry:", entry.getError());
                    break;
                }
                if (entry.getValue() != null) {
                    List<IndexEntry> indexEntries = RPMDBUtils.headerImport(entry.getValue());
                    PackageInfo packageInfo = RPMDBUtils.getNEVRA(indexEntries);
                    List<String> paths = packageInfo.getDirNames();
                    StringJoiner sj = new StringJoiner(",");
                    if (paths != null && !paths.isEmpty()) {
                        for (String path : paths) {
                            sj.add(path);
                        }
                    } else {
                        sj.add("**/*");
                    }

                    ComponentPatternData cpd = new ComponentPatternData();
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageInfo.getName());
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, packageInfo.getVersion());
                    cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageInfo.getName() + "-" + packageInfo.getVersion());
                    cpd.set(Artifact.Attribute.CHECKSUM, packageInfo.getSigMD5());
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, virtualRoot.relativize(relativeAnchorFile).toString());
                    cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                    cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, sj.toString());
                    cpd.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, packageInfo.getLicense());
                    cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
                    cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, RPM_TYPE);
                    cpd.set(Constants.KEY_NO_MATCHING_FILE, Constants.MARKER_CROSS);
                    cpd.set(Artifact.Attribute.PURL, buildPurl(packageInfo.getVendor(), packageInfo.getName(), packageInfo.getVersion(), packageInfo.getArch(), packageInfo.getEpoch(), packageInfo.getRelease()));

                    components.add(cpd);
                }
            }
            return components;
        } catch (Exception e) {
            LOG.warn("Error reading RPM packages file", e);
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

    private String buildPurl(String vendor, String name, String version, String arch, Integer epoch, String distro) {
        StringBuilder sb = new StringBuilder();
        sb.append("pkg:rpm/");
        sb.append(vendor).append("/");
        sb.append(name).append("@");
        sb.append(version);
        if (arch != null && !arch.isEmpty()) {
            sb.append("?arch=").append(arch);
        }
        if (epoch != null) {
            sb.append("&epoch=").append(epoch);
        }
        if (distro != null && !distro.isEmpty()) {
            sb.append("&distro=").append(distro);
        }
        return sb.toString();
    }
}
