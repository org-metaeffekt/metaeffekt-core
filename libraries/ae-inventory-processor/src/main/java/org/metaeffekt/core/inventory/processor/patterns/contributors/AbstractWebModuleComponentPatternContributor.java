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
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.filepatterns.FileMetaData;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Abstract base class for WebModuleComponentPatternContributors. The class provides general parts common to
 * all subclasses to unify the rather complex implementation of such contributors.
 */
@Slf4j
public abstract class AbstractWebModuleComponentPatternContributor extends ComponentPatternContributor {

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

        // chose the primary anchor to use
        final File representativeAnchorFile = selectRepresentativeAnchorFile(definitionFiles);

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
            final WebModule webModule = createWebModule(anchorFile, relativeAnchorPath, anchorChecksum, definitionFiles);

            // create inventory with subcomponents
            final Inventory inventoryFromLockFile = createSubcomponentInventory(relativeAnchorPath, webModule);

            if (webModule.isIncomplete() && (inventoryFromLockFile == null || inventoryFromLockFile.getArtifacts().isEmpty())) {
                return Collections.emptyList();
            }

            // construct component pattern
            final ComponentPatternData componentPatternData = createComponentPatternData(
                    anchorFile, anchorParentDir, anchorChecksum, webModule, contextBaseDir, inventoryFromLockFile);

            return Collections.singletonList(componentPatternData);
        } catch (IOException e) {
        log.warn("Unable to parse web module parts: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Collects an ordered list of definition files. The method uses the ordering as provided by getDefinitionFiles().
     *
     * @param parentDir The folder to inspect.
     *
     * @return An ordered list of files representing the definition files.
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

    /**
     * Select the representative anchor from the available definition files.
     *
     * @param definitionFiles The priority-ordered list of definition files.
     *
     * @return The selected representative definition file, which will be used as anchor.
     */
    protected File selectRepresentativeAnchorFile(List<File> definitionFiles) {
        if (!definitionFiles.isEmpty()) {
            return definitionFiles.get(0);
        }
        return null;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    /**
     * Definition files are all possible anchors and supplemental files. This method supplied all relevant definition
     * files.
     *
     * @return A priority ordered list of files paths that are expected.
     */
    protected abstract List<String> getDefinitionFiles();

    /**
     * Creates an artifact from the given webModule.
     *
     * @param webModule The web module to create the artifact for.
     *
     * @return The implementation-specific, derived artifact.
     */
    protected abstract Artifact createArtifact(WebModule webModule);

    /**
     * Produces a subcomponent inventory.
     *
     * @param relativeAnchorPath Relative anchor path.
     * @param webModule The web module.
     *
     * @return An inventory containing all artifacts identified on subcomponent level.
     *
     * @throws IOException The implementation may involve further parsing of files and may throw IOException.
     */
    protected abstract Inventory createSubcomponentInventory(String relativeAnchorPath, WebModule webModule) throws IOException;

    /**
     * Implementation-specific creation of a ComponentPatternData representing the component.
     *
     * @param anchorFile Anchor file.
     * @param anchorParentDir Parent dir of the anchor.
     * @param anchorChecksum Checksum of the anchor.
     * @param webModule The webmodule on which the ComponentPatternData is based.
     * @param contextBaseDir The context base directory.
     * @param inventoryFromLockFile The subcomponent inventory in case available.
     *
     * @return The specific ComponentPatternData instance representing the collection details.
     */
    protected abstract ComponentPatternData createComponentPatternData(File anchorFile, File anchorParentDir,
           String anchorChecksum, WebModule webModule, File contextBaseDir, Inventory inventoryFromLockFile);


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

    protected WebModule createWebModule(File anchorFile, String relativeAnchorPath, String anchorChecksum, List<File> definitionFiles) throws IOException {
        final WebModule webModule = new WebModule();

        // check if there is a package.json or composer.json or bower.json

        // replace by parse details on definition files; this should include the anchor
        processAnchorFile(anchorFile, webModule);

        // parse all definition to gather/merge all available data
        for (File definitionFile : definitionFiles) {
            if (!definitionFile.equals(anchorFile)) {
                processDefinitionFile(definitionFile, webModule);
            }
        }

        if (webModule.getName() == null || webModule.getVersion() == null) {
            webModule.setIncomplete(true);

            // derive name and version in case not set
            FileMetaData fileMetaData = getFileComponentPatternProcessor().deriveFileMetaData(relativeAnchorPath);
            if (fileMetaData != null) {
                if (webModule.getName() == null) {
                    webModule.setName(fileMetaData.getName());
                }
                if (webModule.getVersion() == null) {
                    webModule.setVersion(fileMetaData.getVersion());
                }
            }
            if (webModule.getName() == null) {
               if (anchorFile.getParentFile() != null) {
                    webModule.setName(anchorFile.getParentFile().getName());
                } else {
                    webModule.setName(anchorFile.getName());
                }
            }
        }

        webModule.setPath(relativeAnchorPath);

        return webModule;
    }

    /**
     * Parse the representative anchor.
     *
     * @param anchorFile The anchor file.
     *
     * @param webModule The web module to contribute to.
     *
     * @throws IOException May be thrown on file access.
     */
    protected abstract void processAnchorFile(File anchorFile, WebModule webModule) throws IOException;

    /**
     * Processes the definition file to contribute further details to the web module. This method will not be invoked
     * for the anchor file.
     *
     * @param definitionFile The definition file.
     * @param webModule The web module to contribute to.
     *
     * @throws IOException May be thrown on file access.
     */
    protected void processDefinitionFile(File definitionFile, WebModule webModule) throws IOException {
        if (definitionFile.exists()) {
            try {
                final String json = FileUtils.readFileToString(definitionFile, "UTF-8");
                JSONObject obj = new JSONObject(json);
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

    protected void mapDependencies(WebModule webModule, JSONObject obj, String dependencyType, Consumer<WebModuleDependency> consumer) {
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

    protected void condenseInventory(Inventory inventory) {
        // select primary assets
        final Set<String> primaryAssetIds = inventory.getAssetMetaData().stream()
                .filter(AssetMetaData::isPrimary)
                .map(a -> a.get(AssetMetaData.Attribute.ASSET_ID))
                .collect(Collectors.toSet());

        if (primaryAssetIds.isEmpty()) {
            throw new IllegalStateException("No primary asset identified.");
        }

        // eliminate all but primary asset columns in artifacts
        final Set<String> givenAssetIds = InventoryUtils.collectAssetIdsFromArtifacts(inventory);
        givenAssetIds.removeAll(primaryAssetIds);

        givenAssetIds.forEach(aid -> InventoryUtils.removeArtifactAttribute(aid, inventory));

        // degrade primary assets as normal assets
        inventory.getAssetMetaData().forEach(amd -> amd.set(Constants.KEY_PRIMARY, null));

        // do not include root module (already subject to contributor)
        final Set<Artifact> removableArtifacts = new HashSet<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            boolean remove = true;
            for (String aid : primaryAssetIds) {
                if (StringUtils.isNotBlank(artifact.get(aid))) {
                    remove = false;
                    break;
                }
            }
            if (remove) {
                removableArtifacts.add(artifact);
            }
        }
        inventory.getArtifacts().removeAll(removableArtifacts);
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

}
