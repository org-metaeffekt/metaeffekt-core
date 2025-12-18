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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.filepatterns.FileMetaData;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.cargo.CargoLock;
import org.metaeffekt.core.inventory.processor.patterns.contributors.cargo.CargoMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;
import static org.metaeffekt.core.util.FileUtils.asRelativePath;
import static org.metaeffekt.core.util.FileUtils.findSingleFile;

public class CargoContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(CargoContributor.class);

    public static final String TYPE_CARGO_CRATE = "cargo-crate";
    public static final String TYPE_CARGO_APP = "cargo-application";

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add("cargo.toml");
        add("cargo.lock");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return suffixes.stream().map(s -> pathInContext.endsWith(s)).findFirst().isPresent();
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile();
        final String anchorRelPath = asRelativePath(contextBaseDir, anchorFile);

        try {
            final List<ComponentPatternData> list = new ArrayList<>();

            if (relativeAnchorPath.endsWith("toml")) {
                final File cargoTomlFile = new File(baseDir, relativeAnchorPath);
                final File cargoLockFile = findSingleFile(cargoTomlFile.getParentFile(), "Cargo.lock", "cargo.lock");

                // handle basic toml file content (CPD)
                final CargoMetadata cargoMetadata = new CargoMetadata(cargoTomlFile);

                final CargoMetadata.Package cargoMetadataPackage = cargoMetadata.getPackage();
                String name = cargoMetadataPackage.getName();
                String version = cargoMetadataPackage.getVersion();

                FileMetaData fileMetaData = getFileComponentPatternProcessor().deriveFileMetaData(relativeAnchorPath);

                boolean incomplete = false;
                if (name == null || version == null) {
                    incomplete = true;
                    if (fileMetaData != null) {
                        if (name == null) {
                            name = fileMetaData.getName();
                        }
                        if (version == null) {
                            version = fileMetaData.getVersion();
                        }
                    }

                    if (name == null) {
                        File parent = anchorFile.getParentFile();
                        if (parent != null) {
                            name = parent.getName();
                        } else {
                            name = anchorFile.getName();
                        }
                    }
                }

                final ComponentPatternData componentPatternData = new ComponentPatternData();

                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorRelPath);
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, name);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);

                if (fileMetaData != null) {
                    componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, fileMetaData.getQualifier());
                } else {
                    if (version != null) {
                        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, name + "-" + version);
                    } else {
                        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, name);
                    }
                }

                // NOTE: we used to be more selective; yet er consider (until excluded) all to be part of the module
                // componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*.rs, **/*_rs.so, **/cargo.lock, **/*");
                componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
                componentPatternData.set(ComponentPatternData.Attribute.TYPE, ARTIFACT_TYPE_MODULE);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_SOURCE_TYPE, TYPE_CARGO_APP);

                componentPatternData.set(Artifact.Attribute.PURL, buildPurl(name, version, null));

                // manage cargoLockFile file
                if (cargoLockFile != null && cargoLockFile.exists()) {
                    final CargoLock cargoLock = new CargoLock(cargoLockFile);
                    final String relativeLockFilePath = asRelativePath(baseDir, cargoLockFile);

                    // only add dependencies as included in the lock file; ignore toml dependencies
                    Inventory inventory = cargoLockSupplier(cargoLock, relativeLockFilePath);
                    if (inventory != null && inventory.getArtifacts().size() > 0) {
                        incomplete = false;
                    }
                    componentPatternData.setExpansionInventorySupplier(() -> inventory);
                } else {
                    // FIXME: add toml file dependencies as seeds
                    LOG.warn("Incomplete contributor implementation. No Cargo.lock file available for [{}].", cargoTomlFile.getAbsolutePath());
                }

                if (!incomplete) {
                    list.add(componentPatternData);
                }
            } else {
                final File cargoLockFile = new File(baseDir, relativeAnchorPath);
                final File cargoTomlFile = findSingleFile(cargoLockFile.getParentFile(), "Cargo.toml", "cargo.toml");
                if (cargoTomlFile != null && cargoTomlFile.exists()) {
                    // skip this evaluation; toml file is detected separately
                } else {
                    // FIXME: no toml file; handle lock file individually
                    LOG.warn("Incomplete contributor implementation. No Cargo.toml file available for [{}].", cargoLockFile.getAbsolutePath());
                }
            }

            return list;
        } catch (IOException e) {
            LOG.warn("Failure processing composer.lock file: [{}]", e.getMessage());
            return Collections.emptyList();
        }

    }

    private static Inventory cargoLockSupplier(CargoLock cargoLock, String relLockFilePath) {
        final Inventory inventory = new Inventory();

        final String lockFileVersion = cargoLock.getVersion();
        LOG.debug("Parsing Cargo Lock file [{}] with version [{}].", relLockFilePath, lockFileVersion);

        // keep track of package name to artifact mapping
        final Map<String, Artifact> nameArtifactMap = new HashMap<>();

        final List<CargoLock.Package> packages = cargoLock.getPackages();

        // 1st pass; create artifacts from packages
        for (CargoLock.Package pkg : packages) {
            final String packageName = pkg.getName();
            final String packageVersion = pkg.getVersion();
            final String packageSource = pkg.getSource();
            final String packageSha256Hash = pkg.getChecksum();

            final Artifact artifact = new Artifact();

            artifact.set(Artifact.Attribute.ID, packageName + "-" + packageVersion);
            artifact.set(Artifact.Attribute.COMPONENT, packageName);
            artifact.set(Artifact.Attribute.VERSION, packageVersion);
            artifact.set(KEY_CARGO_SOURCE_ID, packageSource);

            // FIXME: move differentiation of representation to core
            artifact.set("Source Artifact - " + KEY_HASH_SHA256, packageSha256Hash);

            artifact.set(Artifact.Attribute.TYPE, ARTIFACT_TYPE_MODULE);
            artifact.set(Artifact.Attribute.COMPONENT_SOURCE_TYPE, TYPE_CARGO_CRATE);
            artifact.set(Artifact.Attribute.PATH_IN_ASSET, relLockFilePath + "[" + packageName + "]");

            artifact.set(Artifact.Attribute.PURL, buildPurl(packageName, packageVersion, packageSha256Hash));

            inventory.getArtifacts().add(artifact);

            // memorize mapping
            nameArtifactMap.put(packageName, artifact);
        }

        // 2nd pass; mark relationships
        for (CargoLock.Package pkg : packages) {
            final String packageName = pkg.getName();
            final List<String> dependencies = pkg.getDependencies();

            if (dependencies != null && !dependencies.isEmpty()) {
                for (String dependencyName : dependencies) {
                    final Artifact dependentArtifact = nameArtifactMap.get(packageName);
                    final Artifact dependencyArtifact = nameArtifactMap.get(dependencyName);

                    if (dependentArtifact != null && dependencyArtifact != null) {
                        String assetId = "AID-" + dependentArtifact.getId();
                        final String dependentSha256Hash = dependentArtifact.get(Artifact.Attribute.HASH_SHA256);
                        if (dependentSha256Hash != null) {
                            assetId += "-" + dependentSha256Hash;
                        }

                        // we use the containment relationship here due to rusts' static-linking nature
                        dependencyArtifact.set(assetId, Constants.MARKER_CONTAINS);
                    }
                }
            }
        }

        return inventory;
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    public static String buildPurl(String name, String version, String sha256Hash) {
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(version)) {
            if (StringUtils.isNotBlank(sha256Hash)) {
                return String.format("pkg:cargo/%s/%s&checksum=sha256:%s", name, version, sha256Hash);
            }
            return String.format("pkg:cargo/%s/%s", name, version);
        }
        return null;
    }

}
