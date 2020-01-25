/**
 * Copyright 2009-2020 the original author or authors.
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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.extractor.InventoryExtractor;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts an inventory from node package information.
 */
@Mojo( name = "extract-node-inventory", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class NodeModuleInventoryExtractionMojo extends AbstractInventoryExtractionMojo {

    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_VERSION = "version";
    public static final String ATTRIBUTE_RESOLVED = "resolved";
    public static final String ATTRIBUTE_DEPENDENCIES = "dependencies";
    public static final String ATTRIBUTE_REPOSITORY = "repository";
    public static final String ATTRIBUTE_PUBLISHER = "publisher";
    public static final String ATTRIBUTE_LICENSES = "licenses";
    public static final String ATTRIBUTE_PATH = "path";
    public static final String ATTRIBUTE_LICENSE_FILE = "licenseFile";
    public static final String ATTRIBUTE_COPYRIGHT = "copyright";

    public static final String INVENTORY_ATTRIBUTE_LICENSE_FILE = "License File";
    public static final String INVENTORY_ATTRIBUTE_EXTRACTED_COPYRIGHT_LICENSE_CHECKER = "Extracted Copyright (license-checker)";
    public static final String INVENTORY_ATTRIBUTE_DERIVED_LICENSE_LICENSE_CHECKER = "Derived License (license-checker)";
    public static final String INVENTORY_ATTRIBUTE_PUBLISHER = "Publisher";

    public static final String SEPARATOR_AT = "@";

    public static final String TYPE_NODEJS_MODULE = "nodejs-module";

    @Parameter(defaultValue="${project.build.directory}/analysis/dependency-tree_prod.json", required = true )
    private File dependencyTreeJson;

    @Parameter(defaultValue="${project.build.directory}/analysis/license-checker_prod.json", required = false )
    private File licenseCheckerJson;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Inventory inventory = new Inventory();
            extractModuleArtifacts(dependencyTreeJson, inventory, artifactInventoryId);
            if (licenseCheckerJson != null && licenseCheckerJson.exists()) {
                enrichPackageArtifacts(licenseCheckerJson, inventory);
            }
            targetInventoryFile.getParentFile().mkdirs();
            new InventoryWriter().writeInventory(inventory, targetInventoryFile);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public void extractModuleArtifacts(File file, Inventory inventory, String sourceId) throws IOException {
        String dependencyTree = FileUtils.readFileToString(file, FileUtils.ENCODING_UTF_8);
        JSONObject rootModule = new JSONObject(dependencyTree);
        String name = rootModule.getString(ATTRIBUTE_NAME);
        String version = rootModule.getString(ATTRIBUTE_VERSION);
        String sourceUrl = rootModule.optString(ATTRIBUTE_RESOLVED);

        addModuleArtifact(createPackageArtifact(name, version, sourceUrl, sourceId), inventory);

        JSONObject dependencies = rootModule.optJSONObject(ATTRIBUTE_DEPENDENCIES);
        if (dependencies != null) {
            for (String key : dependencies.keySet()) {
                extractModuleArtifacts(key, dependencies.getJSONObject(key), inventory, sourceId);
            }
        }
    }

    protected void extractModuleArtifacts(String name, JSONObject packageObject, Inventory inventory, String sourceId) {
        String version = packageObject.getString(ATTRIBUTE_VERSION);
        String sourceUrl = packageObject.optString(ATTRIBUTE_RESOLVED);

        addModuleArtifact(createPackageArtifact(name, version, sourceUrl, sourceId), inventory);

        JSONObject dependencies = packageObject.optJSONObject(ATTRIBUTE_DEPENDENCIES);
        if (dependencies != null) {
            for (String key : dependencies.keySet()) {
                extractModuleArtifacts(key, dependencies.getJSONObject(key), inventory, sourceId);
            }
        }
    }

    protected void addModuleArtifact(Artifact artifact, Inventory inventory) {
        if (inventory.findArtifact(artifact.getId()) == null) {
            inventory.getArtifacts().add(artifact);
        }
    }

    protected void enrichPackageArtifacts(File licenseCheckerFile, Inventory inventory) throws IOException {
        String licenseCheckerOutput = FileUtils.readFileToString(licenseCheckerFile, FileUtils.ENCODING_UTF_8);
        JSONObject licenseCheckerResult = new JSONObject(licenseCheckerOutput);
        if (licenseCheckerResult != null) {
            for (String key : licenseCheckerResult.keySet()) {
                JSONObject packageObject = licenseCheckerResult.getJSONObject(key);
                Artifact artifact = inventory.findArtifact(key);
                if (artifact != null) {
                    artifact.setUrl(packageObject.optString(ATTRIBUTE_REPOSITORY));
                    artifact.set(INVENTORY_ATTRIBUTE_PUBLISHER, packageObject.optString(ATTRIBUTE_PUBLISHER));

                    try {
                        artifact.set(INVENTORY_ATTRIBUTE_DERIVED_LICENSE_LICENSE_CHECKER, packageObject.optString(ATTRIBUTE_LICENSES));
                    } catch (Exception e) {
                        try {
                            JSONArray array = packageObject.getJSONArray(ATTRIBUTE_LICENSES);
                            List<String> licenses = new ArrayList<>();
                            for (int i = 0; i < array.length(); i++) {
                                licenses.add(array.getString(i));
                            }
                            artifact.set(INVENTORY_ATTRIBUTE_DERIVED_LICENSE_LICENSE_CHECKER, licenses.stream().collect(Collectors.joining(", ")));
                        } catch (Exception ex) {
                            // ignore
                        }
                    }

                    artifact.addProject(packageObject.optString(ATTRIBUTE_PATH));
                    artifact.set(INVENTORY_ATTRIBUTE_LICENSE_FILE, packageObject.optString(ATTRIBUTE_LICENSE_FILE));
                    artifact.set(INVENTORY_ATTRIBUTE_EXTRACTED_COPYRIGHT_LICENSE_CHECKER, packageObject.optString(ATTRIBUTE_COPYRIGHT));
                }

            }
        }
    }

    protected Artifact createPackageArtifact(String name, String version, String sourceUrl, String sourceId) {
        Artifact artifact = new Artifact();
        String id = name + SEPARATOR_AT + version;
        artifact.setId(id);
        artifact.setVersion(version);
        artifact.setComponent(name);
        artifact.setUrl(sourceUrl);
        artifact.set(InventoryExtractor.KEY_ATTRIBUTE_TYPE, TYPE_NODEJS_MODULE);
        artifact.set(InventoryExtractor.KEY_ATTRIBUTE_SOURCE_PROJECT, sourceId);
        return artifact;
    }

}
