/*
 * Copyright 2009-2022 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class WebModuleComponentPatternContributor extends ComponentPatternContributor {

    @Override
    public boolean applies(File contextBaseDir, String file, Artifact artifact) {
        return isWebModule(file);
    }

    @Override
    public void contribute(File contextBaseDir, String anchorFilePath, Artifact artifact, ComponentPatternData componentPatternData) {
        final File anchorFile = new File(contextBaseDir, anchorFilePath);
        final File anchorParentDir = anchorFile.getParentFile();
        final File parentDir = anchorParentDir.getParentFile();

        // attempt reading web modules metadata
        try {
            WebModule webModule = new WebModule();
            parseDetails(anchorFile, webModule);
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
        } catch (IOException e) {
        }

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, artifact.getComponent());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, artifact.getVersion());
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, artifact.getId());

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, anchorParentDir.getName() + "/**/node_modules/**/*" + "," + anchorParentDir.getName() + "/**/bower_components/**/*");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, anchorParentDir.getName() + "/**/*");

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_NODEJS_MODULE);
    }

    private Artifact parseWebModule(File baseDir, String file, final Map<String, WebModule> pathModuleMap) throws IOException {
        WebModule webModule = getOrInitWebModule(file, pathModuleMap);
        webModule.folder = file;

        Artifact artifact = new Artifact();
        parseModuleDetails(artifact, new File(baseDir, file).getAbsolutePath(), webModule);

        return artifact;
    }

    public class WebModule implements Comparable<WebModule> {
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
    }

    public void createInventory(File scanDir) throws IOException {
        final Map<String, WebModule> pathModuleMap = new HashMap<>();

        // FIXME:
        // - ues derived inventories instead (all files unfiltered, with proper project path)
        // - do for all inventories in a project context

        scanWebComponents(scanDir, pathModuleMap);

        Inventory webComponentInventory = new Inventory();

        Set<String> uniqueAnchors = pathModuleMap.values().stream().map(wm -> wm.anchor != null ? wm.anchorChecksum : null).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> noAnchors = pathModuleMap.values().stream().map(wm -> wm.anchor == null ? wm.path : null).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> uniqueComponents = pathModuleMap.values().stream().map(wm -> wm.name + "-" + wm.version).collect(Collectors.toSet());

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

            System.out.println(cpd.createCompareStringRepresentation());
            webComponentInventory.getComponentPatternData().add(cpd);
        }

        // NOTE: we drop modules, which we do not have sufficient information for using only modules with anchors
        // System.out.println("No anchors: ");
        // noAnchors.forEach(System.out::println);

        System.out.println("Unique components: " + uniqueComponents.size());
        System.out.println("Unique anchors: " + uniqueAnchors.size());
        pathModuleMap.values().stream().sorted().forEach(System.out::println);

        System.out.println("Web modules without version: ");
        pathModuleMap.values().stream().sorted().filter(p -> p.version == null).forEach(System.out::println);

        // derive artifacts of type web component for component patterns
        for (ComponentPatternData cpd : webComponentInventory.getComponentPatternData()) {
            String artifactId = cpd.get(ComponentPatternData.Attribute.COMPONENT_PART);
            String component = cpd.get(ComponentPatternData.Attribute.COMPONENT_NAME);
            String version = cpd.get(ComponentPatternData.Attribute.COMPONENT_VERSION);
            Artifact queryArtifact = new Artifact();
            queryArtifact.setId(artifactId);
            queryArtifact.setVersion(version);
            queryArtifact.setComponent(component);
            // FIXME-Core: use type as attribute constant; renanme nodejs to webmodule (as there are different types)
            queryArtifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_NODEJS_MODULE);
            Artifact artifact = webComponentInventory.findArtifact(queryArtifact);
            if (artifact == null) {
                artifact = queryArtifact;
                webComponentInventory.getArtifacts().add(artifact);
            }
        }

        new InventoryWriter().writeInventory(webComponentInventory, new File("target/web.xls"));
    }

    protected void scanWebComponents(File inventoryFile, Map<String, WebModule> pathModuleMap) throws IOException {
        final Inventory inventory = new InventoryReader().readInventory(inventoryFile);
        for (Artifact artifact : inventory.getArtifacts()) {
            for (String project : artifact.getProjects()) {
                parseWebModule("/node_modules/", artifact, project, pathModuleMap);
                parseWebModule("/bower_components/", artifact, project, pathModuleMap);
            }
        }
    }

    protected void parseWebModule(String moduleMarker, Artifact artifact, String artifactPath, Map<String, WebModule> pathModuleMap) throws IOException {
        if (!isWebModule(artifactPath)) {
            return;
        }

        if (artifactPath.contains(moduleMarker)) {
            String folder = artifactPath.substring(artifactPath.lastIndexOf(moduleMarker) + moduleMarker.length());

            String path = artifactPath.substring(0, artifactPath.lastIndexOf(moduleMarker) + moduleMarker.length());
            path = path + folder;

            System.out.println("Parsing " + folder);

            WebModule webModule = getOrInitWebModule(path, pathModuleMap);
            webModule.folder = folder;

            parseModuleDetails(artifact, artifactPath, webModule);
        }
    }

    protected void parseModuleDetails(Artifact artifact, String project, WebModule webModule) throws IOException {
        String filePath = webModule.folder;

        // do not mix with files in a deeper context
//        if (!project.endsWith(filePath)) return;

        if ("composer.json".equals(artifact.getId())) {
            parseDetails(artifact, project, webModule);
        }

        if ("package.json".equals(artifact.getId())) {
            parseDetails(artifact, project, webModule);
        }
        if (".bower.json".equals(artifact.getId())) {
            parseDetails(artifact, project, webModule);
        }
        if ("bower.json".equals(artifact.getId())) {
            parseDetails(artifact, project, webModule);
        }
        if ("package-lock.json".equals(artifact.getId())) {
            parseDetails(artifact, project, webModule);
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

    protected void parseDetails(File file, WebModule webModule) throws IOException {
        final File packageJson = file;
        if (packageJson.exists()) {
            final String json = FileUtils.readFileToString(packageJson, "UTF-8");
            try {
                final JSONObject obj = new JSONObject(json);
                if (StringUtils.isEmpty(webModule.version)) {
                    webModule.version = getString(obj, "version", webModule.version);
                    webModule.anchor = packageJson;
                }
                if (StringUtils.isEmpty(webModule.version)) {
                    webModule.version = getString(obj, "_version", webModule.version);
                    webModule.anchor = packageJson;
                }
                if (StringUtils.isEmpty(webModule.version)) {
                    webModule.version = getString(obj, "_release", webModule.version);
                    webModule.anchor = packageJson;
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
                System.err.println("Cannot parse :" + packageJson);
            }
        }
    }

    protected void parseDetails(Artifact artifact, String project, WebModule webModule) throws IOException {
        final File baseDir = new File("/Volumes/S-DIT-007");
        final File projectDir = new File(baseDir, project);
        final File packageJson = new File(projectDir, artifact.getId());
        if (packageJson.exists()) {
            final String json = FileUtils.readFileToString(packageJson, "UTF-8");
            JSONObject obj = new JSONObject(json);
            try {
                if (webModule.version == null) {
                    webModule.version = getString(obj, "version", webModule.version);
                    webModule.anchor = packageJson;
                }
                if (webModule.version == null) {
                    webModule.version = getString(obj, "_release", webModule.version);
                    webModule.anchor = packageJson;
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
                System.err.println("Cannot parse :" + packageJson);
            }
        }
    }

    private String getString(JSONObject obj, String key, String defaultValue) {
        return obj.has(key) ? obj.getString(key) : defaultValue;
    }


}
