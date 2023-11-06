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
package org.metaeffekt.core.maven.inventory.extractor.windows.strategy;

import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WindowsPartExtractorSoftwareElement extends WindowsPartExtractorBase {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPartExtractorSoftwareElement.class);

    private final static JSONObject NO_MATCHING_FEATURE = new JSONObject();

    public void parse(Inventory inventory, JSONArray softwareElementJson, JSONArray softwareFeatureJson, JSONArray elementsFeaturesMappingJson) {
        // software feature --(1 : n)--> software element via mapping table

        final Map<JSONObject, List<JSONObject>> softwareFeatureToSoftwareElementMapping = mapSoftwareElementToSoftwareFeatureMapping(softwareElementJson, softwareFeatureJson, elementsFeaturesMappingJson);

        checkMissingFeatures(softwareFeatureJson, softwareFeatureToSoftwareElementMapping);
        checkMissingElements(softwareElementJson, softwareFeatureToSoftwareElementMapping);

        for (Map.Entry<JSONObject, List<JSONObject>> featureElements : softwareFeatureToSoftwareElementMapping.entrySet()) {
            final JSONObject softwareFeature = featureElements.getKey();
            final List<JSONObject> softwareElements = featureElements.getValue();

            if (softwareFeature == NO_MATCHING_FEATURE) {
                continue;
            }

            final Artifact featureArtifact = new Artifact();
            inventory.getArtifacts().add(featureArtifact);
            mapBaseJsonInformationToInventory(softwareFeature, featureArtifact);
            featureArtifact.set("Windows Source", WindowsExtractorAnalysisFile.Class_Win32_SoftwareFeature.getTypeName());

            final String productName = getJsonFieldValue(softwareFeature, "ProductName");
            final String name = getJsonFieldValue(softwareFeature, "Name");
            if (productName != null && name != null) {
                featureArtifact.set(Artifact.Attribute.ID, productName + " " + name);
                featureArtifact.set(Artifact.Attribute.COMPONENT, productName);
            } else if (productName != null) {
                featureArtifact.set(Artifact.Attribute.ID, productName);
                featureArtifact.set(Artifact.Attribute.COMPONENT, productName);
            } else if (name != null) {
                featureArtifact.set(Artifact.Attribute.ID, name);
                featureArtifact.set(Artifact.Attribute.COMPONENT, name);
            } else {
                final String identifyingNumber = getJsonFieldValue(softwareFeature, "IdentifyingNumber");
                featureArtifact.set(Artifact.Attribute.ID, identifyingNumber);
                featureArtifact.set(Artifact.Attribute.COMPONENT, identifyingNumber);
            }

            if (featureArtifact.getId() == null) {
                LOG.warn("No ID found for software feature: {}", softwareFeature);
                featureArtifact.setId(UUID.randomUUID().toString());
            }

            mapJsonFieldToInventory(softwareFeature, featureArtifact, Artifact.Attribute.VERSION, "Version");

            mapJsonFieldToInventory(softwareFeature, featureArtifact, "ProductName", "ProductName");
            mapJsonFieldToInventory(softwareFeature, featureArtifact, "IdentifyingNumber", "IdentifyingNumber");

            mapJsonFieldToInventory(softwareFeature, featureArtifact, "Description", "Description");
            mapJsonFieldToInventory(softwareFeature, featureArtifact, "Caption", "Caption");
            mapJsonFieldToInventory(softwareFeature, featureArtifact, "Organisation", "Vendor");

            mapJsonFieldToInventory(softwareFeature, featureArtifact, "InstallDate", "InstallDate");
            mapJsonFieldToInventory(softwareFeature, featureArtifact, "InstallState", "InstallState");

            if (!softwareElements.isEmpty()) {
                featureArtifact.set("SoftwareElements", new JSONArray(softwareElements).toString());

                // collect all "Path" values from the software elements into a comma-separated list
                softwareElements.stream()
                        .map(e -> getJsonFieldValue(e, "Path"))
                        .filter(Objects::nonNull)
                        .reduce((a, b) -> a + ", " + b)
                        .ifPresent(paths -> featureArtifact.append("System Paths", paths, ", "));
            }
        }
    }

    private void checkMissingFeatures(JSONArray softwareFeatureJson, Map<JSONObject, List<JSONObject>> mapping) {
        final Set<String> featuresFound = new HashSet<>();
        for (Map.Entry<JSONObject, List<JSONObject>> softwareEntry : mapping.entrySet()) {
            featuresFound.add(getJsonFieldValue(softwareEntry.getKey(), "IdentifyingNumber") + getJsonFieldValue(softwareEntry.getKey(), "Name"));
        }

        for (int i = 0; i < softwareFeatureJson.length(); i++) {
            final JSONObject softwareFeature = softwareFeatureJson.getJSONObject(i);
            final String featureName = getJsonFieldValue(softwareFeature, "IdentifyingNumber") + getJsonFieldValue(softwareFeature, "Name");

            if (!featuresFound.contains(featureName)) {
                LOG.warn("Missing feature: {}", featureName);
            }
        }
    }

    private void checkMissingElements(JSONArray softwareElementJson, Map<JSONObject, List<JSONObject>> mapping) {
        for (int i = 0; i < softwareElementJson.length(); i++) {
            final JSONObject softwareElement = softwareElementJson.getJSONObject(i);
            boolean found = false;
            for (Map.Entry<JSONObject, List<JSONObject>> softwareEntry : mapping.entrySet()) {
                if (softwareEntry.getValue().stream().anyMatch(e -> e.equals(softwareElement))) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                final String elementName = getJsonFieldValue(softwareElement, "SoftwareElementID") + getJsonFieldValue(softwareElement, "Name");
                LOG.warn("Missing element: {}", elementName);
            }
        }
    }

    /**
     * This method is responsible for creating a mapping between Software Elements and Software Features.<br>
     * It iterates through a JSON array of software elements, features, and mappings, and maps software elements to
     * respective software features as per the mapping data.
     * <p>The mapping is created as follows:<br>
     * <code>SoftwareFeature:IdentifyingNumber --(1 : n)--> SoftwareElement:IdentificationCode via the SoftwareFeatureSoftwareElement table</code>
     * <p>JSON objects in the elements-features mapping array are formatted as shown in the following example:
     * <pre>
     * {
     *    "Computer": "DESKTOP-TEHU2AC",
     *    "GroupComponent": "Win32_SoftwareFeature.IdentifyingNumber=\"{ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}\",Name=\"VC_Redist\",ProductName=\"Microsoft Visual C++ 2005 Redistributable (x64)\",Version=\"8.0.61000\"",
     *    "PartComponent": "Win32_SoftwareElement.Name=\"uplevel.837BF1EB_D770_94EB_FF1F_C8B3B9A1E18E\",SoftwareElementID=\"{837BF1EB-D770-94EB-A01F-C8B3B9A1E18E}\",SoftwareElementState=2,TargetOperatingSystem=19,Version=\"8.0.61000\""
     * }
     * </pre>
     *
     * @param softwareElementJson         JSONArray containing the softwareElement objects
     * @param softwareFeatureJson         JSONArray containing the softwareFeature objects
     * @param elementsFeaturesMappingJson JSONArray containing the mapping between software elements and features
     * @return A Map where each Key is a JSONObject representing a Software Feature,
     * and the Value is a List of JSONObjects, each representing a Software Element associated with the Feature.
     */
    private Map<JSONObject, List<JSONObject>> mapSoftwareElementToSoftwareFeatureMapping(JSONArray softwareElementJson, JSONArray softwareFeatureJson, JSONArray elementsFeaturesMappingJson) {
        final Map<JSONObject, List<JSONObject>> mapping = new LinkedHashMap<>();

        for (int i = 0; i < elementsFeaturesMappingJson.length(); i++) {
            final JSONObject mappingJson = elementsFeaturesMappingJson.getJSONObject(i);

            final Map<String, String> groupComponentProperties = extractPropertiesFromMappingString(mappingJson.optString("GroupComponent"));
            final Map<String, String> partComponentProperties = extractPropertiesFromMappingString(mappingJson.optString("PartComponent"));

            final String groupComponentIdentifyingNumber = ObjectUtils.firstNonNull(groupComponentProperties.get("Win32_SoftwareFeature.IdentifyingNumber"), groupComponentProperties.get("IdentifyingNumber"));
            final String groupComponentName = ObjectUtils.firstNonNull(groupComponentProperties.get("Name"), groupComponentProperties.get("Win32_SoftwareFeature.Name"));
            final String groupComponentProductName = ObjectUtils.firstNonNull(groupComponentProperties.get("ProductName"), groupComponentProperties.get("Win32_SoftwareFeature.ProductName"));
            final String groupComponentVersion = ObjectUtils.firstNonNull(groupComponentProperties.get("Version"), groupComponentProperties.get("Win32_SoftwareFeature.Version"));

            final String partComponentSoftwareElementId = ObjectUtils.firstNonNull(partComponentProperties.get("SoftwareElementID"), partComponentProperties.get("Win32_SoftwareElement.SoftwareElementID"));
            final String partComponentName = ObjectUtils.firstNonNull(partComponentProperties.get("Win32_SoftwareElement.Name"), partComponentProperties.get("Name"));
            final String partComponentVersion = ObjectUtils.firstNonNull(partComponentProperties.get("Version"), partComponentProperties.get("Win32_SoftwareElement.Version"));

            final JSONObject groupComponentSoftwareFeature = super.findFirstJsonObjectInArray(softwareFeatureJson, new HashMap<String, Object>() {{
                put("IdentifyingNumber", groupComponentIdentifyingNumber);
                put("Name", groupComponentName);
                put("ProductName", groupComponentProductName);
                put("Version", groupComponentVersion);
            }});
            final JSONObject partComponentSoftwareElement = super.findFirstJsonObjectInArray(softwareElementJson, new HashMap<String, Object>() {{
                put("SoftwareElementID", partComponentSoftwareElementId);
                put("Name", partComponentName);
                put("Version", partComponentVersion);
            }});

            if (groupComponentSoftwareFeature != null && partComponentSoftwareElement != null) {
                mapping.computeIfAbsent(groupComponentSoftwareFeature, k -> new ArrayList<>(1)).add(partComponentSoftwareElement);
            } else if (groupComponentSoftwareFeature != null) {
                LOG.warn("No matching software element found for mapping: {}", mappingJson);
                mapping.computeIfAbsent(groupComponentSoftwareFeature, k -> new ArrayList<>());
            } else if (partComponentSoftwareElement != null) {
                LOG.warn("No matching software feature found for mapping: {}", mappingJson);
                mapping.computeIfAbsent(NO_MATCHING_FEATURE, k -> new ArrayList<>(1)).add(partComponentSoftwareElement);
            } else {
                LOG.warn("No matching software feature or software element found for mapping: {}", mappingJson);
            }
        }

        // put all missing software features with empty list of software elements
        for (int i = 0; i < softwareFeatureJson.length(); i++) {
            final JSONObject softwareFeature = softwareFeatureJson.getJSONObject(i);
            if (!mapping.containsKey(softwareFeature)) {
                mapping.put(softwareFeature, new ArrayList<>());
            }
        }

        // put all missing software elements with empty software feature object (NO_MATCHING_FEATURE)
        for (int i = 0; i < softwareElementJson.length(); i++) {
            final JSONObject softwareElement = softwareElementJson.getJSONObject(i);
            if (mapping.values().stream().noneMatch(l -> l.contains(softwareElement))) {
                mapping.computeIfAbsent(NO_MATCHING_FEATURE, k -> new ArrayList<>(1)).add(softwareElement);
            }
        }

        return mapping;
    }

    /**
     * Extracts properties from a mapping string from the SoftwareFeatureSoftwareElement table.<br><br>
     * <code>Win32_SoftwareFeature.IdentifyingNumber="{ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}",Name="VC_Redist",ProductName="Microsoft Visual C++ 2005 Redistributable (x64)",Version="8.0.61000"</code><br><br>
     * will be extracted to a map with the following entries:<br><br>
     * <pre>
     *     IdentifyingNumber = {ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}
     *     Name              = VC_Redist
     *     ProductName       = Microsoft Visual C++ 2005 Redistributable (x64)
     *     Version           = 8.0.61000
     * </pre>
     *
     * @param mappingString the mapping string to extract properties from
     * @return a map of properties extracted from the mapping string
     */
    protected Map<String, String> extractPropertiesFromMappingString(String mappingString) {
        final Map<String, String> properties = new HashMap<>();

        if (mappingString == null) {
            return properties;
        }

        final StringBuilder key = new StringBuilder();
        final StringBuilder value = new StringBuilder();

        boolean isKey = true;
        boolean inQuotes = false;

        final char[] charArray = mappingString.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            final char ch = charArray[i];
            switch (ch) {
                case '\\':
                    if (i + 1 < charArray.length) {
                        (isKey ? key : value).append(charArray[i + 1]);
                        i++;
                    }
                    break;
                case '\"':
                    inQuotes = !inQuotes;
                    break;
                case ',':
                    if (!inQuotes) {
                        properties.put(key.toString().trim(), value.toString().trim());
                        key.setLength(0);
                        value.setLength(0);
                        isKey = true;
                    } else {
                        (isKey ? key : value).append(ch);
                    }
                    break;
                case '=':
                    if (!inQuotes) {
                        isKey = false;
                    } else {
                        (isKey ? key : value).append(ch);
                    }
                    break;
                default:
                    (isKey ? key : value).append(ch);
                    break;
            }
        }

        if (key.length() > 0) {
            properties.put(key.toString().trim(), value.toString().trim());
        }

        return properties;
    }
}
