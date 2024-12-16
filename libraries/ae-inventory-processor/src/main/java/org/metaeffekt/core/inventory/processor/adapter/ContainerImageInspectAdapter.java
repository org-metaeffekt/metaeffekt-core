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
package org.metaeffekt.core.inventory.processor.adapter;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.reader.inspect.image.ImageInspectReader;
import org.metaeffekt.reader.inspect.image.model.ImageInspectData;
import org.metaeffekt.reader.inspect.image.model.ImageInspectElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ContainerImageInspectAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerImageInspectAdapter.class);

    public static final String KEY_IMAGE_ID = "Image Id";

    public AssetMetaData deriveAssetMetadata(File containerInspectionFile) {
        try {
            final ImageInspectData imageInspectElements = ImageInspectReader.dataFromJson(containerInspectionFile);
            if (imageInspectElements.size() == 1) {
                final AssetMetaData assetMetaData = new AssetMetaData();
                assetMetaData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_CONTAINER);

                final ImageInspectElement element = imageInspectElements.get(0);

                extractCreatedTimestamp(element, assetMetaData);

                assetMetaData.set("Size", String.valueOf(element.getSize()));
                assetMetaData.set(KEY_IMAGE_ID, String.valueOf(element.getId()));
                assetMetaData.set("Author", String.valueOf(element.getAuthor()));
                assetMetaData.set(Constants.KEY_ARCHITECTURE, String.valueOf(element.getArchitecture()));
                assetMetaData.set("Os", String.valueOf(element.getOs()));

                assetMetaData.set("Comment", element.getComment());

                extractMetaEntries(element, assetMetaData);
                extractPropData(element, assetMetaData);
                extractConfigData(element, assetMetaData);
                extractRepoDigest(element, assetMetaData);

                // by default containers are always primary assets
                assetMetaData.set("Primary", Constants.MARKER_CROSS);

                // compute id from image id
                assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, "CID-" + getPlainImageId(assetMetaData));

                return assetMetaData;
            } else {
                LOG.warn("Container image inspect file at [{}] shows unexpected content.", containerInspectionFile);
            }
        } catch (IllegalArgumentException e) {
            // we must expect that other .json files are found. Therefore, only log problems in parsing to debug.
            LOG.debug("Cannot parse container inspection file [{}]. Assuming not a inspect file?", containerInspectionFile);
        } catch (Exception e) {
            LOG.error("Cannot parse container inspection file [{}].", containerInspectionFile, e);
        }

        return null;
    }

    private static String getPlainImageId(AssetMetaData assetMetaData) {
        final String hash = assetMetaData.get(KEY_IMAGE_ID);
        if (hash != null && hash.startsWith("sha256:")) {
            return hash.substring(7);
        }
        return hash;
    }

    private void extractRepoDigest(ImageInspectElement element, AssetMetaData assetMetaData) {
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

    private void extractConfigData(ImageInspectElement element, AssetMetaData assetMetaData) {
        if (element.getConfig() != null && element.getConfig().getLabels() != null) {
            for (Map.Entry<String, String> entry : element.getConfig().getLabels().entrySet()) {
                assetMetaData.set("config_" + entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        // handle unified config entries specifically
        modulateValue(assetMetaData, "config_version", AssetMetaData.Attribute.VERSION.getKey());
        modulateValue(assetMetaData, "config_org.opencontainers.image.version", AssetMetaData.Attribute.VERSION.getKey());

        modulateValue(assetMetaData, "config_io.k8s.description", AssetMetaData.Attribute.NAME.getKey());
        modulateValue(assetMetaData, "config_name", AssetMetaData.Attribute.NAME.getKey());

        modulateValue(assetMetaData, "config_architecture", "Architecture");
        modulateValue(assetMetaData, "config_url", "URL");
        modulateValue(assetMetaData, "config_maintainer", "Organization URL");
        modulateValue(assetMetaData, "config_org.opencontainers.image.licenses", "Container Specified Licenses");
    }

    private static void modulateValue(AssetMetaData assetMetaData, String sourceKey, String targetKey) {
        final String value = assetMetaData.get(sourceKey);
        if (!StringUtils.isBlank(value)) {
            assetMetaData.set(targetKey, value);
        }
    }

    private void extractPropData(ImageInspectElement element, AssetMetaData assetMetaData) {
        for (Map.Entry<String, Object> entry : element.getAdditionalProperties().entrySet()) {
            assetMetaData.set("prop_" + entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private void extractMetaEntries(ImageInspectElement element, AssetMetaData assetMetaData) {
        for (Map.Entry<String, String> entry : element.getMetadata().entrySet()) {
            assetMetaData.set("meta_" + entry.getKey(), entry.getValue());
        }
    }

    private void extractCreatedTimestamp(ImageInspectElement element, AssetMetaData assetMetaData) {
        String created = element.getCreated();
        if (created != null) {
            int index = created.indexOf(".");
            if (index != -1) {
                created = created.substring(0, index);
            }
            assetMetaData.set("Created", created);
        }
    }

}
