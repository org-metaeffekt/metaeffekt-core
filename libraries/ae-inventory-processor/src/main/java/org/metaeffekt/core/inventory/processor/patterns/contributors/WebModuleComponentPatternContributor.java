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
import org.metaeffekt.core.inventory.processor.adapter.YarnLockAdapter;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WebModuleComponentPatternContributor extends ComponentPatternContributor {
    private static final Logger LOG = LoggerFactory.getLogger(WebModuleComponentPatternContributor.class);

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        // TODO: perhaps canonicalize these as the specs in "applies" and the suffixes are not the same
        add(".bower.json");
        add("/bower.json");
        add("/package-lock.json");
        add(".package-lock.json");
        add("/package.json");
        add("/composer.json");
        add(".composer.json");
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

            // FIXME: use both package.json and package-lock.json to produce a consolidated outcome
            if (webModule.packageLockJsonFile != null) {
                inventoryFromPackageLock = new NpmPackageLockAdapter().
                        createInventoryFromPackageLock(webModule.packageLockJsonFile, relativeAnchorPath, webModule.name);
            } else {
                artifact.set(Artifact.Attribute.PURL, buildPurl(webModule.name, webModule.version));
            }
        } catch (IOException e) {
            // it was an attempts
            LOG.warn("Unable to parse web module parts: " + e.getMessage(), e);
            return Collections.emptyList();
        }

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());
        componentPatternData.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, artifact.get("Module Specified License"));

        final String anchorParentDirName = anchorParentDir.getName();

        // set includes
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, anchorParentDirName + "/**/*");

        // set excludes
        if ("node_modules".equalsIgnoreCase(anchorParentDirName)) {
            // we are already in the node_modules directory; in this case omit the parent dir;
            // this may happen when we have identified a .package-lock.json file in the node_modules folder;
            // we have to make sure we do not include the complete node_modules folder with all modules
            componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                ".yarn-integrity," +
                "**/node_modules/**/*," +
                "**/bower_components/**/*");
        } else {
            componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                anchorParentDirName + "/.yarn-integrity," +
                anchorParentDirName + "/**/node_modules/**/*," +
                anchorParentDirName + "/**/bower_components/**/*");
        }

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);
        if (anchorFile.getName().endsWith(Constants.BOWER_JSON) || anchorFile.getName().endsWith(Constants.DOT_BOWER_JSON)) {
            componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "bower-module");
        } else {
            componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "npm-module");
        }
        componentPatternData.set(Artifact.Attribute.PURL, buildPurl(artifact.getComponent(), artifact.getVersion()));
        componentPatternData.set(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN, "**/apps/**/*.json, **/apps/**/**/*.json");

        // check whether alternatively a yarn.lock file is available
        File yarnLock = new File(anchorParentDir, "yarn.lock");

        Inventory inventoryFromYarnLock = null;
        if (anchorFile.exists()) {
            inventoryFromYarnLock = new YarnLockAdapter().extractInventory(yarnLock, relativeAnchorPath);
        }

        if (inventoryFromPackageLock != null) {
            final Inventory expansionInventory = inventoryFromPackageLock;
            componentPatternData.setExpansionInventorySupplier(() -> expansionInventory);

            // FIXME: consolidate with yarnLock
        } else {
            if (inventoryFromYarnLock != null) {
                final Inventory expansionInventory = inventoryFromYarnLock;
                // FIXME: consolidate with package.json (dev/prod)
                componentPatternData.setExpansionInventorySupplier(() -> expansionInventory);
            }
        }


        return Collections.singletonList(componentPatternData);
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
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

        String url;

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
                    ", url='" + url + '\'' +
                    '}';
        }

        public boolean hasData() {
            if (StringUtils.isBlank(name)) return false;
            if (StringUtils.isBlank(version)) return false;
            if (anchor == null) return false;

            return true;
        }

        File packageLockJsonFile;
        File packageJsonFile;
        File yarnLockFile;
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
            case ".package-lock.json":
            case "composer.json":
            case ".composer.json":
            case "package.json":
            case ".bower.json":
            case "bower.json":
                parseDetails(artifact, project, webModule, baseDir);
        }
    }

    boolean isWebModule(String artifactPath) {
        if (artifactPath.endsWith(Constants.PACKAGE_JSON)) {
            return true;
        }
        if (artifactPath.endsWith(Constants.BOWER_JSON)) {
            return true;
        }
        if (artifactPath.endsWith(Constants.PACKAGE_LOCK_JSON)) {
            return true;
        }
        if (artifactPath.endsWith(Constants.DOT_PACKAGE_LOCK_JSON)) {
            return true;
        }
        if (artifactPath.endsWith(Constants.COMPOSER_JSON)) {
            return true;
        }
        if (artifactPath.endsWith(Constants.DOT_BOWER_JSON)) {
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

            // in case of a package-lock.json / package.json or pairs; keep the references
            if (packageJsonFile.getName().endsWith("package-lock.json")) {
                webModule.packageLockJsonFile = packageJsonFile;
                // check for adjacent package.json
                final File file = new File(packageJsonFile.getParentFile(), "package.json");
                if (file.exists()) {
                    webModule.packageJsonFile = file;
                }
            } else {
                final File file = new File(packageJsonFile.getParentFile(), "package-lock.json");
                if (file.exists()) {
                     webModule.packageLockJsonFile = file;
                }
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

    public static String buildPurl(String name, String version) {
        return String.format("pkg:npm/%s@%s", name.toLowerCase(), version);
    }
}
