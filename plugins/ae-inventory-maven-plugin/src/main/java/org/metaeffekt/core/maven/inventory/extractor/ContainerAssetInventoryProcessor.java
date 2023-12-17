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
package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.reader.inspect.image.ImageInspectReader;
import org.metaeffekt.reader.inspect.image.model.ImageInspectData;
import org.metaeffekt.reader.inspect.image.model.ImageInspectElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.model.Constants.KEY_ISSUE;

public class ContainerAssetInventoryProcessor extends BaseInventoryProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerAssetInventoryProcessor.class);

    public static final String ASSET_MARKER = "x";

    private File analysisDir;
    private File containerInspectionFile;

    private String repo;
    private String tag;
    private String qualifier;

    public ContainerAssetInventoryProcessor() {}

    public ContainerAssetInventoryProcessor basedOn(File containerInspectionFile) {
        this.containerInspectionFile = containerInspectionFile;
        return this;
    }

    public ContainerAssetInventoryProcessor fromRepo(String repo) {
        this.repo = repo;
        return this;
    }

    public ContainerAssetInventoryProcessor withTag(String tag) {
        this.tag = tag;
        return this;
    }

    public ContainerAssetInventoryProcessor fromAnalysisDir(File analysisDir) {
        this.analysisDir = analysisDir;
        return this;
    }

    public ContainerAssetInventoryProcessor qualifiedBy(String qualifier) {
        this.qualifier = qualifier;
        return this;
    }

    public ContainerAssetInventoryProcessor augmenting(File inventoryFile) {
        super.augmenting(inventoryFile);
        return this;
    }

    public void process() throws IOException {
        // load the inventory
        final Inventory inventory = new InventoryReader().readInventory(getInventoryFile());

        final AssetMetaData assetMetadata = createAssetMetadata(inventory);
        final String assetId = assetMetadata.get(AssetMetaData.Attribute.ASSET_ID);

        // apply asset metadata to all entries in the inventory
        for (Artifact artifact : inventory.getArtifacts()) {
            artifact.set(assetId, ASSET_MARKER);

            // FIXME: we eliminate issue on artifact level; this is an asset level attribute
            //   Needs consolidation of new asset feature.
            artifact.set(KEY_ISSUE, null);
        }

        // write the inventory
        new InventoryWriter().writeInventory(inventory, getInventoryFile());
    }

    private AssetMetaData createAssetMetadata(Inventory inventory) {
        AssetMetaData assetMetaData = new AssetMetaData();
        assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, "CID-" + repo + "-" + tag);
        assetMetaData.set("Repository", repo);
        assetMetaData.set("Version", tag);
        inventory.getAssetMetaData().add(assetMetaData);

        attachContainerInspectionDetails(assetMetaData);
        attachIssueInformation(assetMetaData);

        return assetMetaData;
    }

    private void attachIssueInformation(AssetMetaData assetMetaData) {
        try {
            DebianInventoryExtractor extractor = new DebianInventoryExtractor();
            final String issue = extractor.extractIssue(analysisDir);
            assetMetaData.set(KEY_ISSUE, issue);

        } catch (Exception e) {
            LOG.error("Cannot inspect issue of container image {}:{}.", repo, tag, e);
        }
    }

    private void attachContainerInspectionDetails(AssetMetaData assetMetaData) {
        try {
            final ImageInspectData imageInspectElements = ImageInspectReader.dataFromJson(containerInspectionFile);
            if (imageInspectElements.size() == 1) {
                ImageInspectElement element = imageInspectElements.get(0);
                String created = element.getCreated();

                int index = created.indexOf(".");
                if (index != -1) {
                    created = created.substring(0, index);
                }

                assetMetaData.set("Created", created);

                assetMetaData.set("Size", String.valueOf(element.getSize()));
                assetMetaData.set("Image Id", String.valueOf(element.getId()));
                assetMetaData.set("Author", String.valueOf(element.getAuthor()));
                assetMetaData.set("Architecture", String.valueOf(element.getArchitecture()));
                assetMetaData.set("Os", String.valueOf(element.getOs()));
                assetMetaData.set("Primary", ASSET_MARKER);
                assetMetaData.set("Qualifier", qualifier);

                final List<String> repoDigests = element.getRepoDigests();
                if (repoDigests != null) {
                    for (String repoDigest : repoDigests) {
                        int atIndex = repoDigest.lastIndexOf("@");
                        if (atIndex > 0) {
                            final String repo = repoDigest.substring(0, atIndex);
                            assetMetaData.set("Repository", repo);
                            final String digest = repoDigest.substring(atIndex + 1);
                            assetMetaData.set("Digest", digest);

                            // FIXME: currently we pick the first non-localhost
                            if (!repo.contains("localhost")) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Cannot parse container inspection file {} for {}:{}.", containerInspectionFile, repo, tag, e);
        }
    }

}
