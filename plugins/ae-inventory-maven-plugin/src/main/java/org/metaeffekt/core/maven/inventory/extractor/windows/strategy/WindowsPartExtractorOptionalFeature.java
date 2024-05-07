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
package org.metaeffekt.core.maven.inventory.extractor.windows.strategy;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryInfo;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowsPartExtractorOptionalFeature extends WindowsPartExtractorBase {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPartExtractorOptionalFeature.class);

    public void parse(Inventory inventory, JSONArray optionalFeatureJson) {
        for (int i = 0; i < optionalFeatureJson.length(); i++) {
            final JSONObject featureJson = optionalFeatureJson.getJSONObject(i);

            final String featureName = getJsonFieldValue(featureJson, "Name");

            final String effectiveFeatureName;
            if (inventory.findInventoryInfo(featureName) != null) {
                LOG.warn("Duplicate [{}] feature name found: {}", WindowsExtractorAnalysisFile.Class_Win32_OptionalFeature.getTypeName(), featureName);
                effectiveFeatureName = featureName + " (" + getJsonFieldValue(featureJson, "Caption") + ")";
            } else {
                effectiveFeatureName = featureName;
            }

            final InventoryInfo optionalFeatureInventoryInfo = inventory.findOrCreateInventoryInfo(effectiveFeatureName);
            mapBaseJsonInformationToInventory(featureJson, optionalFeatureInventoryInfo);
            optionalFeatureInventoryInfo.set("Windows Source", WindowsExtractorAnalysisFile.Class_Win32_OptionalFeature.getTypeName());

            optionalFeatureInventoryInfo.set("Name", effectiveFeatureName);
            mapJsonFieldToInventory(featureJson, optionalFeatureInventoryInfo, "Caption", "Caption");

            final Integer installStateInt = super.getJsonFieldValueInt(featureJson, "InstallState");
            if (installStateInt != null) {
                final OptionalFeatureInstallState installState = OptionalFeatureInstallState.fromCode(installStateInt);
                if (installState != null) {
                    optionalFeatureInventoryInfo.set("InstallState", String.valueOf(installState.getCode()));
                    optionalFeatureInventoryInfo.set("InstallState Name", installState.getDescribingName());
                } else {
                    optionalFeatureInventoryInfo.set("InstallState", String.valueOf(installStateInt));
                    optionalFeatureInventoryInfo.set("InstallState Name", "unknown");
                }
            }
        }
    }

    public enum OptionalFeatureInstallState {
        BAD_CONFIGURATION(-6, "bad configuration"),
        INVALID_ARGUMENT(-2, "invalid argument"),
        UNKNOWN_PACKAGE(-1, "unknown package"),
        ENABLED(1, "enabled"),
        DISABLED(2, "disabled"),
        ABSENT(3, "absent"),
        UNINSTALLED(4, "uninstalled"),
        HARDWARE_NOT_PRESENT(5, "hardware not present"),
        PARENT_FEATURE_DISABLED(6, "parent feature disabled"),
        DISABLED_THROUGH_GROUP_POLICY(7, "disabled through group policy"),
        ENABLED_BUT_PAYLOAD_REMOVED(8, "enabled, but payload removed");

        private final int code;
        private final String describingName;

        OptionalFeatureInstallState(int code, String describingName) {
            this.code = code;
            this.describingName = describingName;
        }

        public int getCode() {
            return code;
        }

        public String getDescribingName() {
            return describingName;
        }

        public static OptionalFeatureInstallState fromCode(int code) {
            for (OptionalFeatureInstallState state : values()) {
                if (state.getCode() == code) {
                    return state;
                }
            }
            return null;
        }

        public static OptionalFeatureInstallState fromDescribingName(String describingName) {
            for (OptionalFeatureInstallState state : values()) {
                if (state.getDescribingName().equals(describingName)) {
                    return state;
                }
            }
            return null;
        }
    }
}
