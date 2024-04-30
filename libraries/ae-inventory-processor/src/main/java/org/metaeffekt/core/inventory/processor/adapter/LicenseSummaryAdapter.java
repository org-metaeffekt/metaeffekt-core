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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LicenseSummaryAdapter {

    /**
     * Parses a license summary file in the structure
     *
     * @param licenseSummaryFile The file to parse. Currently, it is expected that the file in UTF-8 encoded.
     * @param relPath Relative path to version anchor.
     *
     * @return An inventory with artifacts parsed from the file.
     *
     * @throws IOException TODO: Missing description
     */
    public Inventory createInventoryFromLicenseSummary(File licenseSummaryFile, String relPath) throws IOException {
        final String content = FileUtils.readFileToString(licenseSummaryFile, FileUtils.ENCODING_UTF_8);
        final JSONObject jsonObject = new JSONObject(content);

        final JSONObject data = jsonObject.getJSONObject("data");
        final JSONArray head = data.getJSONArray("head");
        final JSONArray body = data.getJSONArray("body");

        final List<Object> keyList = head.toList();
        final Inventory inventory = new Inventory();

        for (int i = 0; i < body.length(); i++) {
            final JSONArray row = body.getJSONArray(i);

            final Artifact artifact = new Artifact();
            artifact.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_WEB_MODULE);

            for (int j = 0; j < row.length(); j++) {
                final String value = row.getString(j);

                final Object originalKey = keyList.get(j);
                final String targetKey = mapKey(originalKey);

                artifact.set(targetKey, value);
            }

            if (artifact.isValid()) {
                inventory.getArtifacts().add(artifact);
            }

            modulate(artifact);

            artifact.set(Constants.KEY_PATH_IN_ASSET, relPath);
        }
        return inventory;
    }

    private static void modulate(Artifact artifact) {

        String originalName = artifact.getId();

        // derive component
        String component = originalName;
        int slashIndex = component.indexOf("/");
        if (slashIndex != -1) {
            component = component.substring(0, slashIndex);
        }
        artifact.setComponent(component);

        // include version in id
        if (StringUtils.hasText(artifact.getVersion())) {
            if (!originalName.contains(artifact.getVersion())) {
                artifact.setId(originalName + "-" + artifact.getVersion());
            }
        }

        // eliminate "unknown" in Vendor, Vendor URL and URL
        final String valueUnknown = "unknown";
        eliminateValue(artifact, "Vendor", valueUnknown);
        eliminateValue(artifact, "Vendor URL", valueUnknown);
        eliminateValue(artifact, "URL", valueUnknown);
    }

    private static void eliminateValue(Artifact artifact, String key, String eliminateValue) {
        final String value = artifact.get(key);
        if (eliminateValue.equalsIgnoreCase(value)) {
            artifact.set(key, null);
        }
    }

    private String mapKey(Object originalKey) {
        switch (String.valueOf(originalKey)) {
            case "Name": return "Id";
            case "Version": return "Version";
            case "License": return "Package Specified Licenses";
            case "URL": return "URL";
            case "VendorUrl": return "Vendor URL";
            case "VendorName": return "Vendor";

        }
        throw new IllegalStateException("Attribute " + originalKey + " not yet mapped.");
    }

}
