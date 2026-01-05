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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.ComposerLockAdapter;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.metaeffekt.core.util.FileUtils.asRelativePath;

/**
 * Separate WebModuleComponentPatternContributor for Composer.
 */
@Slf4j
public class ComposerWebModuleComponentPatternContributor extends AbstractWebModuleComponentPatternContributor {

    // FIXME-KKL: replace suffixes by regexp patterns
    private static final List<String> SUFFIXES = Collections.unmodifiableList(new ArrayList<String>() {{
        // FIXME-KKL: tests whether a top-level package json is detected
        add("/composer.json");

        // lock files are secondary
        add("/composer.lock");
        add(".composer.lock");
    }});

    /**
     * Definition files may contain information for the component pattern. These files are collected and parsed in
     * the given order.
     */
    private static final List<String> DEFINITION_FILES = Collections.unmodifiableList(new ArrayList<String>() {{
        add("composer.json");
        add("composer.lock");
        add(".composer.lock");
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
    protected ComponentPatternData createComponentPatternData(File anchorFile, File anchorParentDir, String anchorChecksum, WebModule webModule, File contextBaseDir, Inventory inventoryFromLockFile) {
        final Artifact artifact = createArtifact(webModule);

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, artifact.get("Module Specified License"));

        // create purl
        componentPatternData.set(Artifact.Attribute.PURL, buildPurl("composer", webModule.getName(), webModule.getVersion()));

        final String anchorParentDirName = anchorParentDir.getName();

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART_PATH, anchorParentDirName + "/" + anchorFile.getName());

        // set includes
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, anchorParentDirName + "/**/*");

        // set excludes
        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
            "**/node_modules/**/*," +
            "**/bower_components/**/*," +
            "**/.package-lock.json," +
            "**/bower.json," +
            "**/.bower.json," +
            "**/package.json," +
            "**/package-lock.json," +
            "**/yarn-integrity," +
            "**/yarn.lock"
        );

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
        componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "php-module");
        componentPatternData.set(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN, "**/apps/**/*.json");

        if (inventoryFromLockFile != null) {
            final Inventory expansionInventory = inventoryFromLockFile;
            componentPatternData.setExpansionInventorySupplier(() -> expansionInventory);
        }
        return componentPatternData;
    }

    @Override
    protected Inventory createSubcomponentInventory(String relativeAnchorPath, WebModule webModule) throws IOException {
        Inventory inventoryFromLockFile = null;

        for (File lockFile : webModule.getLockFiles()) {
            if (lockFile.toPath().endsWith("composer.lock")) {
                inventoryFromLockFile = new ComposerLockAdapter().extractInventory(lockFile, webModule);
                condenseInventory(inventoryFromLockFile);
            } else {
                log.warn("Lock file not processed: " + lockFile.getAbsolutePath());
            }
        }

        return inventoryFromLockFile;
    }

    @Override
    public List<String> getSuffixes() {
        return SUFFIXES;
    }

    protected void processAnchorFile(File anchorFile, WebModule webModule) {
        try {
            final String json = FileUtils.readFileToString(anchorFile, "UTF-8");
            final JSONObject obj = new JSONObject(json);

            if (anchorFile.getName().endsWith("composer.lock")) {
                webModule.setName(anchorFile.getParentFile().getName());
                // FIXME-KKL: detected components without name are considered abstract; do not invent data
                webModule.setVersion("unspecific");
            } else {
                webModule.setName(getString(obj, "name", webModule.getName()));
                webModule.setLicense(getString(obj, "license", webModule.getLicense()));
                webModule.setVersion(getVersion(obj));
                webModule.setAnchor(anchorFile);
                if (webModule.getVersion() != null) {
                    webModule.setAnchorChecksum(FileUtils.computeChecksum(webModule.getAnchor()));
                }

                final String version = webModule.getVersion();
                if (version != null && webModule.getVersion().matches("v[0-9].*")) {
                    webModule.setVersion(webModule.getVersion().substring(1));
                }
            }

            if (anchorFile.getName().endsWith("composer.json")) {
                JSONObject dependencies = obj.optJSONObject("require");
                if (dependencies != null) {
                    for (String key : dependencies.keySet()) {
                        WebModuleDependency dependency = new WebModuleDependency();
                        dependency.setName(key);
                        String version = dependencies.getString(key);
                        dependency.setVersionRange(version);
                        dependency.setRuntimeDependency(true);
                        webModule.getDirectDependencies().add(dependency);
                    }
                }
                dependencies = obj.optJSONObject("require-dev");
                if (dependencies != null) {
                    for (String key : dependencies.keySet()) {
                        WebModuleDependency dependency = new WebModuleDependency();
                        dependency.setName(key);
                        String version = dependencies.getString(key);
                        dependency.setVersionRange(version);
                        dependency.setDevDependency(true);
                        webModule.getDirectDependencies().add(dependency);
                    }
                }

                // check for adjacent composer.lock
                final File file = new File(anchorFile.getParentFile(), "composer.lock");
                if (file.exists()) {
                    webModule.getLockFiles().add(file);
                }
            }
        } catch (Exception e) {
            log.warn("Cannot parse web module information: [{}]", anchorFile);
        }
    }

    @Override
    protected List<String> getDefinitionFiles() {
        return DEFINITION_FILES;
    }
}
