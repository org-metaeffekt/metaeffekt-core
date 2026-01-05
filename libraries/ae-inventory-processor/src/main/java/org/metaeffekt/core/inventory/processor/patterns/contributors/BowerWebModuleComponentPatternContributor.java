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
import org.metaeffekt.core.inventory.processor.adapter.BowerLockAdapter;
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
 * Separate WebModuleComponentPatternContributor for Bower.
 */
@Slf4j
public class BowerWebModuleComponentPatternContributor extends AbstractWebModuleComponentPatternContributor {

    // FIXME-KKL: replace suffixes by regexp patterns
    private static final List<String> SUFFIXES = Collections.unmodifiableList(new ArrayList<String>() {{
        // FIXME-KKL: tests whether a top-level package json is detected
        add("/.bower.json");

        // lock files are secondary
        add("/bower.json");
    }});

    /**
     * Definition files may contain information for the component pattern. These files are collected and parsed in
     * the given order.
     */
    private static final List<String> DEFINITION_FILES = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".bower.json");
        add("bower.json");
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
        Inventory inventoryFromLockFile = null;
        for (File lockFile : webModule.getLockFiles()) {
            if (lockFile.toPath().endsWith(".bower.json")) {
                inventoryFromLockFile = new BowerLockAdapter().extractInventory(lockFile, relativeAnchorPath);
            } else {
                log.warn("Lock file not processed: " + lockFile.getAbsolutePath());
            }
        }
        return inventoryFromLockFile;
    }

    @Override
    protected ComponentPatternData createComponentPatternData(File anchorFile, File anchorParentDir, String anchorChecksum, WebModule webModule, File contextBaseDir, Inventory inventoryFromLockFile) {

        Artifact artifact = createArtifact(webModule);

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, artifact.get("Module Specified License"));

        // create purl
        componentPatternData.set(Artifact.Attribute.PURL, buildPurl("bower", webModule.getName(), webModule.getVersion()));

        final String anchorParentDirName = anchorParentDir.getName();

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART_PATH, anchorParentDirName + "/" + anchorFile.getName());

        // set includes
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, anchorParentDirName + "/**/*");

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
            anchorParentDirName + "/.yarn-integrity," +
            anchorParentDirName + "/**/node_modules/**/*," +
            anchorParentDirName + "/**/bower_components/**/*," +

            anchorParentDirName + "/**/.package-lock.json," +
            anchorParentDirName + "/**/package.json," +
            anchorParentDirName + "/**/yarn.lock," +
            anchorParentDirName + "/**/composer.json," +
            anchorParentDirName + "/**/composer.lock"
        );

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
        componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "bower-module");
        componentPatternData.set(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN, "**/apps/**/*.json");

        if (inventoryFromLockFile != null) {
            final Inventory expansionInventory = inventoryFromLockFile;
            componentPatternData.setExpansionInventorySupplier(() -> expansionInventory);
        }

        return componentPatternData;
    }

    protected void parseModuleDetails(Artifact artifact, String project, WebModule webModule, File baseDir) throws IOException {
        switch(artifact.getId()) {
            case ".bower.json":
            case "bower.json":
                parseDetails(artifact, project, webModule, baseDir);
        }
    }

    protected void parseDetails(Artifact artifact, String project, WebModule webModule, File baseDir) throws IOException {
        final File projectDir = new File(baseDir, project);
        final File packageJsonFile = new File(projectDir, artifact.getId());
        if (packageJsonFile.exists()) {
            final String json = FileUtils.readFileToString(packageJsonFile, "UTF-8");
            JSONObject obj = new JSONObject(json);
            try {
                if (webModule.getVersion() == null) {
                    webModule.setVersion(getString(obj, "version", webModule.getVersion()));
                    webModule.setAnchor(packageJsonFile);
                }
                if (webModule.getVersion() == null) {
                    webModule.setVersion(getString(obj, "_release", webModule.getVersion()));
                    webModule.setAnchor(packageJsonFile);
                }
                webModule.setName(getString(obj, "name", webModule.getName()));

                if (webModule.getAnchor() != null) {
                    webModule.setAnchorChecksum(FileUtils.computeChecksum(webModule.getAnchor()));
                }

                // eliminate trailing v on version
                final String version = webModule.getVersion();
                if (version != null && version.matches("v[0-9].*")) {
                    webModule.setVersion(version.substring(1));
                }

                webModule.setLicense(getString(obj, "license", webModule.getLicense()));
            } catch (Exception e) {
                log.warn("Cannot parse web module information: [{}]", packageJsonFile);
            }
        }
    }

    @Override
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

            if (anchorFile.getName().endsWith("bower.json")) {
                if (obj.has("dependencies")) {
                    JSONObject dependencies = obj.getJSONObject("dependencies");
                    for (String key : dependencies.keySet()) {
                        WebModuleDependency dependency = new WebModuleDependency();
                        dependency.setName(key);
                        dependency.setResolvedVersion(dependencies.getString(key));
                        dependency.setDevDependency(false);
                        webModule.getDirectDependencies().add(dependency);
                    }
                }
                if (obj.has("devDependencies")) {
                    JSONObject devDependencies = obj.getJSONObject("devDependencies");
                    for (String key : devDependencies.keySet()) {
                        WebModuleDependency dependency = new WebModuleDependency();
                        dependency.setName(key);
                        dependency.setResolvedVersion(devDependencies.getString(key));
                        dependency.setDevDependency(true);
                        webModule.getDirectDependencies().add(dependency);
                    }
                }

                final File file = new File(anchorFile.getParentFile(), ".bower.json");
                if (file.exists()) {
                    webModule.getLockFiles().add(file);
                }
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
}
