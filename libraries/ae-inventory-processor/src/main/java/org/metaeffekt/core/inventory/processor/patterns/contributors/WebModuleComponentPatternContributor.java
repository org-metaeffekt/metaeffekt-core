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
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.NpmPackageLockAdapter;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class WebModuleComponentPatternContributor extends ComponentPatternContributor {
    private static final Logger LOG = LoggerFactory.getLogger(WebModuleComponentPatternContributor.class);

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        // TODO: perhaps canonicalize these ad the specs in "applies" and the suffixes are not the same
        add(".bower.json");
        add("/bower.json");
        add("/package-lock.json");
        add("/.package-lock.json");
        add("/package.json");
        add("/composer.json");
    }});

        @Override
    public boolean applies(String pathInContext) {
        return isWebModule(pathInContext);
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File anchorParentDir = anchorFile.getParentFile();
        final File parentDir = anchorParentDir.getParentFile();
        final File contextBaseDir = anchorParentDir.getParentFile();

        final Artifact artifact = new Artifact();

        Inventory inventoryFromPackageLock = null;

        // attempt reading web modules metadata
        try {
            WebModule webModule = new WebModule();
            parseDetails(anchorFile, webModule);

            if (!webModule.hasData()) {
                return Collections.emptyList();
            }

            artifact.setVersion(webModule.version);
            artifact.set("Module Specified License", webModule.license);
            artifact.setComponent(webModule.name);

            if (!StringUtils.isEmpty(webModule.name)) {
                if (webModule.name.contains(parentDir.getName())) {
                    // e.g. @babel
                    artifact.setComponent(parentDir.getName());
                }
                // e.g. @babel/parser
                if ("unspecific".equalsIgnoreCase(artifact.getVersion()) || StringUtils.isEmpty(artifact.getVersion())) {
                    artifact.setId(webModule.name);
                } else {
                    artifact.setId(webModule.name + "-" + webModule.version);
                }
            }

            if (webModule.packageLockJsonFile != null) {
                inventoryFromPackageLock = new NpmPackageLockAdapter().createInventoryFromPackageLock(webModule.packageLockJsonFile, relativeAnchorPath);
            }

        } catch (IOException e) {
            // it was an attempts
            LOG.warn("Unable to parse web module parts: " + e.getMessage(), e);
        }

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, anchorParentDir.getName() + "/.yarn-integrity," + anchorParentDir.getName() + "/**/bower_components/**/*");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, anchorParentDir.getName() + "/**/*");

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_NODEJS_MODULE);

        if (inventoryFromPackageLock != null) {
            final Inventory expansionInventory = inventoryFromPackageLock;
            componentPatternData.setExpansionInventorySupplier(() -> expansionInventory);
        }

        return Collections.singletonList(componentPatternData);
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    private Artifact parseWebModule(File baseDir, String file, final Map<String, WebModule> pathModuleMap) throws IOException {
        WebModule webModule = getOrInitWebModule(file, pathModuleMap);
        webModule.folder = file;

        Artifact artifact = new Artifact();
        parseModuleDetails(artifact, new File(baseDir, file).getAbsolutePath(), webModule, baseDir);

        return artifact;
    }

    public static class WebModule implements Comparable<WebModule> {
        String folder;
        String name;
        String version;
        String license;

        String path;
        File anchor;
        String anchorChecksum;

        @Override
        public int compareTo(WebModule o) {
            return path.compareToIgnoreCase(o.path);
        }

        @Override
        public String toString() {
            return "WebModule{" +
                    "folder='" + folder + '\'' +
                    ", name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    ", license='" + license + '\'' +
                    ", path='" + path + '\'' +
                    ", anchor='" + anchor + '\'' +
                    '}';
        }

        public boolean hasData() {
            if (StringUtils.isBlank(name)) return false;
            if (StringUtils.isBlank(version)) return false;
            if (anchor == null) return false;

            return true;
        }

        File packageLockJsonFile;
    }

    public void createInventory(File scanDir) throws IOException {
        final Map<String, WebModule> pathModuleMap = new HashMap<>();

        // FIXME:
        // - ues derived inventories instead (all files unfiltered, with proper project path)
        // - do for all inventories in a project context

        scanWebComponents(scanDir, pathModuleMap, scanDir);

        Inventory webComponentInventory = new Inventory();

        Set<String> uniqueAnchors = pathModuleMap.values().stream().map(wm -> wm.anchor != null ? wm.anchorChecksum : null).filter(Objects::nonNull).collect(Collectors.toSet());

        for (String anchorChecksum : uniqueAnchors) {
            WebModule webModule = null;
            for (WebModule candidate : pathModuleMap.values()) {
                if (anchorChecksum.equals(candidate.anchorChecksum)) {
                    webModule = candidate;
                }
            }

            ComponentPatternData cpd = new ComponentPatternData();
            cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, webModule.name);
            cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, webModule.name + "-" + webModule.version);
            cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, webModule.version);

            cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/" + webModule.folder + "/**/*");
            String versionAnchorPath = webModule.anchor.getAbsolutePath();
            versionAnchorPath = versionAnchorPath.substring(versionAnchorPath.indexOf(webModule.folder));
            cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, versionAnchorPath);
            cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, webModule.anchorChecksum);

            cpd.set("TMP-LICENSE", webModule.license);
            cpd.set("TMP-PATH", webModule.path);

            webComponentInventory.getComponentPatternData().add(cpd);
        }

        // NOTE: we drop modules, which we do not have sufficient information for using only modules with anchors

        // derive artifacts of type web component for component patterns
        for (ComponentPatternData cpd : webComponentInventory.getComponentPatternData()) {
            String artifactId = cpd.get(ComponentPatternData.Attribute.COMPONENT_PART);
            String component = cpd.get(ComponentPatternData.Attribute.COMPONENT_NAME);
            String version = cpd.get(ComponentPatternData.Attribute.COMPONENT_VERSION);
            Artifact queryArtifact = new Artifact();
            queryArtifact.setId(artifactId);
            queryArtifact.setVersion(version);
            queryArtifact.setComponent(component);
            // FIXME-Core: use type as attribute constant; rename nodejs to webmodule (as there are different types)
            queryArtifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_NODEJS_MODULE);
            Artifact artifact = webComponentInventory.findArtifact(queryArtifact);
            if (artifact == null) {
                artifact = queryArtifact;
                webComponentInventory.getArtifacts().add(artifact);
            }
        }

        new InventoryWriter().writeInventory(webComponentInventory, new File("target/web.xls"));
    }

    protected void scanWebComponents(File inventoryFile, Map<String, WebModule> pathModuleMap, File baseDir) throws IOException {
        final Inventory inventory = new InventoryReader().readInventory(inventoryFile);
        for (Artifact artifact : inventory.getArtifacts()) {
            for (String project : artifact.getProjects()) {
                parseWebModule(baseDir, artifact, project, pathModuleMap, "/node_modules/");
                parseWebModule(baseDir, artifact, project, pathModuleMap, "/bower_components/");
            }
        }
    }

    protected void parseWebModule(File baseDir, Artifact artifact, String artifactPath, Map<String, WebModule> pathModuleMap, String moduleMarker) throws IOException {
        if (!isWebModule(artifactPath)) {
            return;
        }

        if (artifactPath.contains(moduleMarker)) {
            String folder = artifactPath.substring(artifactPath.lastIndexOf(moduleMarker) + moduleMarker.length());

            String path = artifactPath.substring(0, artifactPath.lastIndexOf(moduleMarker) + moduleMarker.length());
            path = path + folder;

            WebModule webModule = getOrInitWebModule(path, pathModuleMap);
            webModule.folder = folder;

            parseModuleDetails(artifact, artifactPath, webModule, baseDir);
        }
    }

    protected void parseModuleDetails(Artifact artifact, String project, WebModule webModule, File baseDir) throws IOException {
        switch(artifact.getId()) {
            case "package-lock.json":
            case "composer.json":
            case "package.json":
            case ".bower.json":
            case "bower.json":
                parseDetails(artifact, project, webModule, baseDir);
        }
    }

    boolean isWebModule(String artifactPath) {
        if (artifactPath.endsWith("package.json")) {
            return true;
        }
        if (artifactPath.endsWith("bower.json")) {
            return true;
        }
        if (artifactPath.endsWith("package-lock.json")) {
            return true;
        }
        if (artifactPath.endsWith("composer.json")) {
            return true;
        }
        if (artifactPath.endsWith(".bower.json")) {
            return true;
        }
        return false;
    }

    protected WebModule getOrInitWebModule(String path, Map<String, WebModule> pathModuleMap) {
        WebModule webModule = pathModuleMap.get(path);
        if (webModule == null) {
            webModule = new WebModule();
            webModule.path = path;
            pathModuleMap.put(path, webModule);
        }
        return webModule;
    }

    protected void parseDetails(File packageJsonFile, WebModule webModule) throws IOException {
        if (packageJsonFile.exists()) {
            final String json = FileUtils.readFileToString(packageJsonFile, "UTF-8");
            try {
                final JSONObject obj = new JSONObject(json);
                if (StringUtils.isEmpty(webModule.version)) {
                    webModule.version = getString(obj, "version", webModule.version);
                    webModule.anchor = packageJsonFile;
                }
                if (StringUtils.isEmpty(webModule.version)) {
                    webModule.version = getString(obj, "_version", webModule.version);
                    webModule.anchor = packageJsonFile;
                }
                if (StringUtils.isEmpty(webModule.version)) {
                    webModule.version = getString(obj, "_release", webModule.version);
                    webModule.anchor = packageJsonFile;
                }

                webModule.name = getString(obj, "name", webModule.name);

                if (webModule.anchor != null) {
                    webModule.anchorChecksum = FileUtils.computeChecksum(webModule.anchor);
                }

                if (webModule.version != null && webModule.version.matches("v[0-9].*")) {
                    webModule.version = webModule.version.substring(1);
                }

                webModule.license = getString(obj, "license", webModule.license);
            } catch (Exception e) {
                LOG.warn("Cannot parse web module information: [{}]", packageJsonFile);
            }

            // in case of a package-lock.json file; keep the reference
            if (packageJsonFile.getName().endsWith("package-lock.json")) {
                webModule.packageLockJsonFile = packageJsonFile;
            }

        }
    }

    protected void parseDetails(Artifact artifact, String project, WebModule webModule, File baseDir) throws IOException {
        final File projectDir = new File(baseDir, project);
        final File packageJsonFile = new File(projectDir, artifact.getId());
        if (packageJsonFile.exists()) {
            final String json = FileUtils.readFileToString(packageJsonFile, "UTF-8");
            JSONObject obj = new JSONObject(json);
            try {
                if (webModule.version == null) {
                    webModule.version = getString(obj, "version", webModule.version);
                    webModule.anchor = packageJsonFile;
                }
                if (webModule.version == null) {
                    webModule.version = getString(obj, "_release", webModule.version);
                    webModule.anchor = packageJsonFile;
                }
                webModule.name = getString(obj, "name", webModule.name);

                if (webModule.anchor != null) {
                    webModule.anchorChecksum = FileUtils.computeChecksum(webModule.anchor);
                }

                if (webModule.version != null && webModule.version.matches("v[0-9].*")) {
                    webModule.version = webModule.version.substring(1);
                }

                webModule.license = getString(obj, "license", webModule.license);
            } catch (Exception e) {
                LOG.warn("Cannot parse web module information: [{}]", packageJsonFile);
            }
        }
    }

    private String getString(JSONObject obj, String key, String defaultValue) {
        return obj.has(key) ? obj.getString(key) : defaultValue;
    }

}
