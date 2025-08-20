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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.InventoryMergeUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.adapter.NpmPackageLockAdapter;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static org.metaeffekt.core.util.FileUtils.asRelativePath;

/**
 * Separate WebModuleComponentPatternContributor for NPM.
 */
@Slf4j
public class NpmWebModuleComponentPatternContributor extends AbstractWebModuleComponentPatternContributor {

    // FIXME-KKL: replace suffixes by regexp patterns
    private static final List<String> SUFFIXES = unmodifiableList(new ArrayList<String>() {{
        // FIXME-KKL: tests whether a top-level package json is detected
        add("/package.json");

        // lock files are secondary
        add("/package-lock.json");
        add(".package-lock.json");
        add("/yarn.lock");
        add(".yarn.lock");
    }});

    /**
     * Definition files may contain information for the component pattern. These files are collected and parsed in
     * the given order.
     */
    private static final List<String> DEFINITION_FILES = unmodifiableList(new ArrayList<String>() {{
        add("package.json");
        add("package-lock.json");
        add("yarn.lock");
        add(".package-lock.json");
        add(".yarn.lock");
    }});

    @Override
    protected Artifact createArtifact(WebModule webModule) {
        final Artifact artifact = new Artifact();

        artifact.setVersion(webModule.getVersion());
        artifact.set("Module Specified License", webModule.getLicense());
        artifact.setComponent(webModule.getName());

        if (!StringUtils.isEmpty(webModule.getName())) {
            // derive id; append version if available
            if ("unspecific".equalsIgnoreCase(artifact.getVersion()) || StringUtils.isEmpty(artifact.getVersion())) {
                artifact.setId(webModule.getName());
            } else {
                artifact.setId(webModule.getName() + "-" + webModule.getVersion());
            }
        }
        return artifact;
    }

    @Override
    protected Inventory createSubcomponentInventory(String relativeAnchorPath, WebModule webModule) throws IOException {
        final List<Inventory> inventoriesFromLockFiles = new ArrayList<>();
        for (File lockFile : webModule.getLockFiles()) {
            Inventory lockInventory = new NpmPackageLockAdapter().createInventoryFromPackageLock(lockFile, webModule);
            if (lockInventory != null) {
                inventoriesFromLockFiles.add(lockInventory);
            }
        }

        if (!inventoriesFromLockFiles.isEmpty()) {
            final Inventory inventory = new Inventory();

            // condense inventory dependency details
            for (Inventory inventoryFromLockFile : inventoriesFromLockFiles) {
                condenseInventory(inventoryFromLockFile);
            }

            new InventoryMergeUtils().mergeInventories(inventoriesFromLockFiles, inventory);

            return inventory;
        }

        return null;
    }

    @Override
    protected ComponentPatternData createComponentPatternData(File anchorFile, File anchorParentDir, String anchorChecksum, Artifact artifact, File contextBaseDir, Inventory inventoryFromLockFile) {

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, artifact.get("Module Specified License"));

        final String anchorParentDirName = anchorParentDir.getName();

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART_PATH, anchorParentDirName + "/" + anchorFile.getName());

        // set includes
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, anchorParentDirName + "/**/*");

        // set excludes
        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                "**/.yarn-integrity," +
                "**/node_modules/**/*," +
                "**/bower_components/**/*," +
                "**/bower.json," +
                "**/.bower.json," +
                "**/composer.json," +
                "**/composer.lock");

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
        componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
        componentPatternData.set(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN, "**/apps/**/*.json");

        if (inventoryFromLockFile != null) {
            final Inventory expansionInventory = inventoryFromLockFile;
            componentPatternData.setExpansionInventorySupplier(() -> expansionInventory);
        }

        return componentPatternData;
    }

    public static String buildPurl(String packageManager, String name, String version) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        String normalizedName = name.toLowerCase();
        if (packageManager.equals("npm") && name.startsWith("@")) {
            normalizedName = name;
        }

        if (version == null || version.isEmpty() || "unspecific".equalsIgnoreCase(version)) {
            return String.format("pkg:%s/%s", packageManager.toLowerCase(), normalizedName);
        }

        if (version.startsWith("v")) {
            version = version.substring(1);
        }

        return String.format("pkg:%s/%s@%s", packageManager.toLowerCase(), normalizedName, version);
    }

    @Override
    protected void processAnchorFile(File anchorFile, WebModule webModule) {
        try {
            final File anchorParentDir = anchorFile.getParentFile();

            // collect lock files for further analysis
            final File packageLockFile = new File(anchorParentDir, "package-lock.json");
            if (packageLockFile.exists()) {
                webModule.getLockFiles().add(packageLockFile);
            }
            final File yarnLockFile = new File(anchorParentDir, "yarn.lock");
            if (yarnLockFile.exists()) {
                webModule.getLockFiles().add(yarnLockFile);
            }

            // manage anchor
            webModule.setAnchor(anchorFile);
            webModule.setAnchorChecksum(FileUtils.computeChecksum(anchorFile));

            if (!anchorFile.getName().equals("yarn.lock")) {
                final String json = FileUtils.readFileToString(anchorFile, "UTF-8");
                final JSONObject obj = new JSONObject(json);

                webModule.setName(getString(obj, "name", webModule.getName()));
                webModule.setLicense(getString(obj, "license", webModule.getLicense()));
                webModule.setVersion(getVersion(obj));

                if (anchorFile.getName().endsWith("package.json")) {
                    // see https://docs.npmjs.com/cli/v11/configuring-npm/package-json
                    mapDependencies(webModule, obj, "dependencies", d -> d.setRuntimeDependency(true));
                    mapDependencies(webModule, obj, "devDependencies", d -> d.setDevDependency(true));
                    mapDependencies(webModule, obj, "peerDependencies", d -> d.setPeerDependency(true));
                    mapDependencies(webModule, obj, "optionalDependencies", d -> d.setOptionalDependency(true));

                    // TODO: peerDependencyMeta are not evaluated; can be nested
                    // TODO: overwrites not processed yet; we can leave this to the lock to resolve

                }
                // NOTE: nothing to do in the else branch; we leave the direct dependencies empty/unspecified for lock files
            }
        } catch (Exception e) {
            log.warn("Cannot parse web module information: [{}]", anchorFile);
        }
    }

    @Override
    public List<String> getSuffixes() {
        return SUFFIXES;
    }

    @Override
    protected List<String> getDefinitionFiles() {
        return DEFINITION_FILES;
    }

    @Override
    protected void processDefinitionFile(File definitionFile, WebModule webModule) throws IOException {
        if (!definitionFile.getName().equals("yarn.lock")) {
            super.processDefinitionFile(definitionFile, webModule);
        }
    }
}
