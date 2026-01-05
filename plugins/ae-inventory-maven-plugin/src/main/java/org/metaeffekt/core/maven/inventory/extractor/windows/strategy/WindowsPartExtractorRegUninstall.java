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
package org.metaeffekt.core.maven.inventory.extractor.windows.strategy;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.maven.inventory.extractor.windows.WindowsExtractorAnalysisFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class WindowsPartExtractorRegUninstall extends WindowsPartExtractorBase {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPartExtractorRegUninstall.class);

    public void parse(Inventory inventory, JSONArray regUninstallJson) {
        for (int i = 0; i < regUninstallJson.length(); i++) {
            parse(inventory, regUninstallJson.getJSONObject(i));
        }
    }

    public void parse(Inventory inventory, JSONObject regUninstallJson) {
        final JSONObject properties = regUninstallJson.optJSONObject("Properties", null);
        if (properties == null || properties.isEmpty()) {
            LOG.warn("Empty registry uninstall entry found: {}", regUninstallJson);
            return;
        }

        final String key = regUninstallJson.optString("Key", null);
        if (key == null) {
            LOG.warn("Registry uninstall entry does not have a key: {}", regUninstallJson);
            return;
        }


        final String productName = getJsonFieldValue(properties, "DisplayName");

        final Artifact productArtifact = findArtifactOrElseAppendNew(inventory,
                artifact -> {
                    if (productName != null) {
                        return StringUtils.equals(artifact.getId(), productName);
                    } else {
                        return false;
                    }
                });

        mapBaseJsonInformationToInventory(properties, productArtifact);
        productArtifact.set("Windows Source", WindowsExtractorAnalysisFile.RegistrySubtree_WindowsUninstall.getTypeName());

        mapJsonFieldToInventory(properties, productArtifact, Artifact.Attribute.ID, "DisplayName", "PSChildName");
        mapJsonFieldToInventory(properties, productArtifact, Artifact.Attribute.VERSION, "DisplayVersion");
        mapJsonFieldToInventory(properties, productArtifact, Constants.KEY_ORGANIZATION, "Publisher");

        if (productArtifact.get(Artifact.Attribute.ID) == null) {
            LOG.warn("Failed to extract artifact Id for registry uninstall entry: {}", regUninstallJson);
            inventory.getArtifacts().remove(productArtifact);
        }

        final String comments = getJsonFieldValue(properties, "Comments");
        if (StringUtils.isNotBlank(comments)) {
            productArtifact.append("Description", comments, ", ");
        }


        // first non-null URL
        final String urlInfoAbout = getJsonFieldValue(properties, "URLInfoAbout");
        final String urlUpdateInfo = getJsonFieldValue(properties, "URLUpdateInfo");
        if (StringUtils.isNotBlank(urlInfoAbout)) {
            productArtifact.set(Artifact.Attribute.URL, urlInfoAbout);
        } else if (StringUtils.isNotBlank(urlUpdateInfo)) {
            productArtifact.set(Artifact.Attribute.URL, urlUpdateInfo);
        }
        productArtifact.set("URLInfoAbout", urlInfoAbout);
        productArtifact.set("URLUpdateInfo", urlUpdateInfo);


        // all paths into "System Paths"
        final String displayIcon = extractPath(getJsonFieldValue(properties, "DisplayIcon"));
        final String installLocation = extractPath(getJsonFieldValue(properties, "InstallLocation"));
        final String installSource = extractPath(getJsonFieldValue(properties, "InstallSource"));
        final String uninstallString = extractPath(getJsonFieldValue(properties, "UninstallString"));

        final Set<String> systemPaths = new HashSet<>();

        systemPaths.add(displayIcon);
        systemPaths.add(installLocation);
        systemPaths.add(installSource);
        systemPaths.add(uninstallString);

        systemPaths.removeIf(StringUtils::isBlank);

        if (!systemPaths.isEmpty()) {
            productArtifact.append("System Paths", String.join(", ", systemPaths), ", ");
        }

        mapJsonFieldToInventory(properties, productArtifact, "InstallSource", "InstallSource");
        mapJsonFieldToInventory(properties, productArtifact, "InstallLocation", "InstallLocation");
        mapJsonFieldToInventory(properties, productArtifact, "DisplayIcon", "DisplayIcon");
        mapJsonFieldToInventory(properties, productArtifact, "UninstallString", "UninstallString");


        // "Id" - "Version" = "Component"
        final String id = productArtifact.get(Artifact.Attribute.ID);
        final String version = productArtifact.get(Artifact.Attribute.VERSION);
        if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(version)) {
            productArtifact.set(Artifact.Attribute.COMPONENT, constructComponent(id, version));
        }


        // store key path
        productArtifact.set("Registry Key", key);
    }

    private String constructComponent(String id, String version) {
        final boolean hasId = StringUtils.isNotBlank(id);
        final boolean hasVersion = StringUtils.isNotBlank(version);

        if (!hasVersion && hasId) {
            return id;
        } else if (hasVersion && !hasId) {
            return version;
        } else if (hasVersion) { // && hasId
            // 1. replace the last occurrence of the version in the id with nothing (make sure to quote the version for regex)
            // 2. remove trailing [vV _.:;,-], but the v only if it is a character that comes before the version
            final Pattern versionPattern = Pattern.compile(Pattern.quote(version) + "$");
            final String component = versionPattern.matcher(id).replaceAll("");
            return component.replaceAll("([ _.:;,-]+[vV][ _.:;,-]*|[ _.:;,-]+)$", "");
        } else {
            return null;
        }
    }

    private String extractPath(String string) {
        if (StringUtils.isBlank(string)) return null;

        final String potentialPath;
        if (string.startsWith("\"")) {
            potentialPath = StringUtils.substringBetween(string, "\"", "\"");
        } else {
            potentialPath = string;
        }

        // valid:
        //   C:\\Program Files (x86)\\Sierra Wireless Inc\\Driver Package\\unDriverPackageSetup.exe
        //   d:\\447a9ba3035683d904a6e0fa56\\
        // invalid:
        //   MsiExec.exe /X{DA5E371C-6333-3D8A-93A4-6FD5B20BCC6E}

        if (StringUtils.isBlank(potentialPath)) return null;

        final Pattern pathPattern = Pattern.compile("^[a-zA-Z]:\\\\.*");
        if (pathPattern.matcher(potentialPath).matches()) {
            return potentialPath;
        } else {
            return null;
        }
    }
}
