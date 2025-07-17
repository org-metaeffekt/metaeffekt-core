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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Abstract base class for WebModuleComponentPatternContributors. The class provides the general parts common to
 * all subclasses to unify the rather complex implementation on such contributors.
 */
@Slf4j
public abstract class AbstractWebModuleComponentPatternContributor extends ComponentPatternContributor {

    /**
     * Definition files are all possible anchors and supplemental files.
     *
     * @return A priority ordered list of files paths that are expected.
     */
    protected abstract List<String> getDefinitionFiles();

    // FIXME-KKL: revise
    protected abstract Artifact createArtifact(WebModule webModule);

    // FIXME-KKL: revise
    protected abstract Inventory createSubcomponentInventory(String relativeAnchorPath, WebModule webModule) throws IOException;

    // FIXME-KKL: revise
    protected abstract ComponentPatternData createComponentPatternData(File anchorFile, File anchorParentDir, String anchorChecksum, Artifact artifact, File contextBaseDir, Inventory inventoryFromLockFile);

    @Override
    public boolean applies(String pathInContext) {
        // FIXME-KKL: replace suffixes by regexp patterns
        return getSuffixes().stream().map(pathInContext::endsWith).findFirst().isPresent();
    }

    @Override
    public final List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File anchorParentDir = anchorFile.getParentFile();

        // collect all relevant files
        final List<File> definitionFiles = collectDefinitionFiles(anchorParentDir);
        final Map<String, File> definitionFileMap = buildDefinitionFileMap(anchorParentDir);

        // chose the primary anchor to use
        final File representativeAnchorFile = selectRepresentativeAnchorFile(definitionFileMap, definitionFiles);

        // we skip further evaluation in favor of the representative anchor file
        if (!anchorFile.getName().equals(representativeAnchorFile.getName())) {
            return Collections.emptyList();
        }

        // TODO: parse all; merge results
        // TODO: use qualifier to path and type to mark as already processed

        final File contextBaseDir = anchorParentDir.getParentFile();

        // check whether the selected anchor file exists
        if (!anchorFile.exists()) {
            return Collections.emptyList();
        }

        try {
            final WebModule webModule = createWebModule(anchorFile, definitionFiles);

            // in case the webModule ends up with too few data, we skip
            if (!webModule.hasData()) {
                return Collections.emptyList();
            }

            final Artifact artifact = createArtifact(webModule);

            // create inventory with subcomponents
            final Inventory inventoryFromLockFile = createSubcomponentInventory(relativeAnchorPath, webModule);

            // construct component pattern
            final ComponentPatternData componentPatternData = createComponentPatternData(
                    anchorFile, anchorParentDir, anchorChecksum, artifact, contextBaseDir, inventoryFromLockFile);

            return Collections.singletonList(componentPatternData);
        } catch (IOException e) {
            log.warn("Unable to parse web module parts: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Builds a map of file paths/names to files. This implementation makes use of the order of getDefinitionFiles().
     *
     * @param parentDir The directory, which to inspect.
     *
     * @return A map of file paths/names to files.
     */
    protected Map<String, File> buildDefinitionFileMap(File parentDir) {
        final Map<String, File> definitionFileMap = new HashMap<>();
        for (String name : getDefinitionFiles()) {
            final File definitionFile = new File(parentDir, name);
            if (definitionFile.exists()) {
                // store with original name; preserve subfolders context
                definitionFileMap.put(name, definitionFile);

                // store with filename only; do not preserve subfolders context; no not overwrite if exists already
                String definitionFileName = definitionFile.getName();
                if (definitionFileMap.containsKey(definitionFileName)) {
                    definitionFileMap.put(definitionFileName, definitionFile);
                }
            }
        }
        return definitionFileMap;
    }

    /**
     * Collects an ordered list of definition files. The methods uses the ordering as provided by getDefinitionFiles().
     *
     * @param parentDir The folder to inspect.
     *
     * @return A ordered list of files representing the definition files.
     */
    protected List<File> collectDefinitionFiles(File parentDir) {
        final List<File> definitionFiles = new ArrayList<>();
        for (String name : getDefinitionFiles()) {
            final File definitionFile = new File(parentDir, name);
            if (definitionFile.exists()) {
                definitionFiles.add(definitionFile);
            }
        }
        return definitionFiles;
    }

    protected File selectRepresentativeAnchorFile(Map<String, File> definitionFileMap, List<File> definitionFiles) {
        for (File definitionFile : definitionFiles) {
            return definitionFile;
        }
        return null;
    }


    @Override
    public int getExecutionPhase() {
        return 1;
    }

    protected WebModule getOrInitWebModule(String path, Map<String, WebModule> pathModuleMap) {
        WebModule webModule = pathModuleMap.get(path);
        if (webModule == null) {
            webModule = new WebModule();
            webModule.setPath(path);
            pathModuleMap.put(path, webModule);
        }
        return webModule;
    }

    protected String getVersion(JSONObject obj) {
        if (obj.has("version")) {
            return obj.getString("version");
        }
        if (obj.has("_version")) {
            return obj.getString("_version");
        }
        if (obj.has("_release")) {
            return obj.getString("_release");
        }
        return null;
    }

    protected WebModule createWebModule(File anchorFile, List<File> definitionFiles) throws IOException {
        WebModule webModule = new WebModule();

        // check if there is a package.json or composer.json or bower.json

        // replace by parse details on definition files; this should include the anchor
        processAnchorFile(anchorFile, webModule);

        // parse all definition to gather/merge all available data
        for (File definitionFile : definitionFiles) {
            if (!definitionFile.equals(anchorFile)) {
                processDefinitionFile(definitionFile, webModule);
            }
        }

        return webModule;
    }

    /**
     * Parse the representative anchor.
     *
     * @param anchorFile The anchor file.
     *
     * @param webModule The web module to contribute to.
     */
    protected abstract void processAnchorFile(File anchorFile, WebModule webModule) throws IOException;

    /**
     * Processes the definition file to contribute further details to the web module. This method will not be invoked
     * for the anchor file.
     *
     * @param definitionFile The definition file.
     * @param webModule The web module to contribute to.
     */
    protected void processDefinitionFile(File definitionFile, WebModule webModule) throws IOException {
        if (definitionFile.exists()) {
            final String json = FileUtils.readFileToString(definitionFile, "UTF-8");
            JSONObject obj = new JSONObject(json);
            try {
                if (StringUtils.isBlank(webModule.getVersion())) {
                    webModule.setVersion(getString(obj, "version", webModule.getVersion()));
                }
                if (StringUtils.isBlank(webModule.getName())) {
                    webModule.setName(getString(obj, "name", webModule.getName()));
                }
                if (StringUtils.isBlank(webModule.getLicense())) {
                    webModule.setLicense(getString(obj, "license", webModule.getLicense()));
                }
            } catch (Exception e) {
                log.warn("Cannot parse web module information: [{}]", definitionFile);
            }
        }
    }

    protected String getString(JSONObject obj, String key, String defaultValue) {
        return obj.has(key) ? obj.getString(key) : defaultValue;
    }

    /**
     * FIXME-KKL
     *
     * @param webModule
     * @param obj
     * @param dependencyType
     * @param consumer
     */
    protected static void mapDependencies(WebModule webModule, JSONObject obj, String dependencyType, Consumer<WebModuleDependency> consumer) {
        if (obj.has(dependencyType)) {
            final JSONObject dependencies = obj.getJSONObject(dependencyType);
            for (String dependencyName : dependencies.keySet()) {
                final WebModuleDependency dependency = new WebModuleDependency();
                dependency.setName(dependencyName);
                dependency.setVersionRange(dependencies.getString(dependencyName));
                consumer.accept(dependency);
                webModule.getDirectDependencies().add(dependency);
            }
        }
    }




}
