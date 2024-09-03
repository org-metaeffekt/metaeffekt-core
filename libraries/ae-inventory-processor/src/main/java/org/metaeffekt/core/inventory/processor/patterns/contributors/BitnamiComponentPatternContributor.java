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

import com.github.packageurl.PackageURL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class BitnamiComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(BitnamiComponentPatternContributor.class);
    private static final String BITNAMI_PACKAGE_TYPE = "bitnami-module";
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>() {{
        add(".spdx");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith(".spdx");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        File spdxFile = new File(baseDir, relativeAnchorPath);

        String relativePathForAnchor = FileUtils.asRelativePath(spdxFile.getParentFile().getParentFile(), spdxFile);
        String bitnamiPackagesDir = spdxFile.getParentFile().getParentFile().getPath();
        String packagesDir = spdxFile.getParentFile().getPath();

        if (!spdxFile.exists()) {
            LOG.warn("SPDX file does not exist: [{}]", spdxFile.getAbsolutePath());
            return Collections.emptyList();
        }

        try {
            String spdxContent = new String(Files.readAllBytes(spdxFile.toPath()));
            JSONObject rootNode = new JSONObject(spdxContent);
            return parseSpdxFile(rootNode, bitnamiPackagesDir, packagesDir, relativePathForAnchor, anchorChecksum);
        } catch (IOException e) {
            LOG.error("Error reading SPDX file", e);
            return Collections.emptyList();
        }
    }

    private List<ComponentPatternData> parseSpdxFile(JSONObject rootNode, String bitnamiPackagesDir, String packagesDir, String relativeAnchorPath, String anchorChecksum) {
        List<ComponentPatternData> components = new ArrayList<>();

        JSONArray packagesNode = rootNode.getJSONArray("packages");
        List<String> bitnamiPackageTypes = new ArrayList<>();
        for (int i = 0; i < packagesNode.length(); i++) {
            JSONObject packageNode = packagesNode.getJSONObject(i);

            List<String> packageNameCandidates = getPackageNameCandidates(packageNode);
            List<String> versionCandidates = getVersionCandidates(packageNode);

            // collect all packages in the spdx
            if (!packageNameCandidates.isEmpty() && !versionCandidates.isEmpty()) {
                String packageName = packageNameCandidates.get(0);
                String version = versionCandidates.get(0);

                String packageFolder = FileUtils.asRelativePath(bitnamiPackagesDir, packagesDir);
                ComponentPatternData cpd = new ComponentPatternData();
                cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, packageName.toLowerCase());
                cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
                cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, packageName.toLowerCase() + "-" + version);
                cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
                cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                Set<String> includePatterns = new HashSet<>();
                for (String packageNameCandidate : packageNameCandidates) {
                    bitnamiPackageTypes.add(packageNameCandidate.toLowerCase());
                    for (String versionCandidate : versionCandidates) {
                        includePatterns.add("licenses/" + packageNameCandidate.toLowerCase() + "-" + versionCandidate + ".*");
                    }
                }
                cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, packageFolder + "/**/*," + "scripts/" + packageFolder + "/**/*," + String.join(",", includePatterns));
                cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");
                cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
                cpd.set(Constants.KEY_SPECIFIED_PACKAGE_LICENSE, getDeclaredLicense(packageNode));
                cpd.set(Constants.KEY_SPECIFIED_PACKAGE_CONCLUDED_LICENSE, getConcludedLicense(packageNode));
                cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, BITNAMI_PACKAGE_TYPE);
                cpd.set(Constants.KEY_NO_FILE_MATCH_REQUIRED, Constants.MARKER_CROSS);
                cpd.set(Artifact.Attribute.PURL, getPurl(packageNode));
                components.add(cpd);
            }
        }
        List<String> bitnamiPackageTypeWithoutDuplicatesAndNull = bitnamiPackageTypes.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        File bitnamiCompnentsJson = new File(bitnamiPackagesDir, ".bitnami_components.json");
        try {
            String bitnamiComponentsJsonContent = new String(Files.readAllBytes(bitnamiCompnentsJson.toPath()));
            JSONObject bitnamiComponentsJson = new JSONObject(bitnamiComponentsJsonContent);
            for (String type : bitnamiPackageTypeWithoutDuplicatesAndNull) {
                File moduleFolder = new File(bitnamiPackagesDir, type);
                if (moduleFolder.exists() && moduleFolder.isDirectory()) {
                    JSONObject packageNode = bitnamiComponentsJson.getJSONObject(type);
                    String topLevelVersion = packageNode.optString("version");
                    if (!type.isEmpty() && topLevelVersion != null) {
                        ComponentPatternData cpd = new ComponentPatternData();
                        cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, "bitnami-" + type);
                        cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, topLevelVersion);
                        cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, "bitnami-" + type + "-" + topLevelVersion);
                        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeAnchorPath);
                        cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
                        cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, type + "/**/*," + "common/**/*,scripts/**/*,licenses/**/*,.bitnami_components.json");
                        cpd.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/*.jar, **/node_modules/**/*");
                        cpd.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);
                        cpd.set(Constants.KEY_COMPONENT_SOURCE_TYPE, BITNAMI_PACKAGE_TYPE);
                        cpd.set(Constants.KEY_NO_FILE_MATCH_REQUIRED, Constants.MARKER_CROSS);
                        components.add(cpd);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not read bitnami-components.json", e);
        }

        return components;
    }

    private String getPurl(JSONObject packageNode) {
        JSONArray externalRefs = packageNode.optJSONArray("externalRefs");
        if (externalRefs != null) {
            for (int i = 0; i < externalRefs.length(); i++) {
                JSONObject externalRef = externalRefs.getJSONObject(i);
                if (("PACKAGE_MANAGER".equals(externalRef.getString("referenceCategory")) || "PACKAGE-MANAGER".equals(externalRef.getString("referenceCategory")))
                        && "purl".equals(externalRef.getString("referenceType"))) {
                    return externalRef.getString("referenceLocator");
                }
            }
        }
        return null;
    }

    private String getDeclaredLicense(JSONObject packageNode) {
        return packageNode.optString("licenseDeclared");
    }

    private String getConcludedLicense(JSONObject packageNode) {
        return packageNode.optString("licenseConcluded");
    }

    private List<String> getPackageNameCandidates(JSONObject currentPackageNode) {
        List<String> packageNameCandidates = new ArrayList<>();
        String currentPackageNodePurl = getPurl(currentPackageNode);
        try {
            if (currentPackageNodePurl != null) {
                PackageURL currentPackagePurl = new PackageURL(currentPackageNodePurl);
                packageNameCandidates.add(currentPackagePurl.getName());
            }
        } catch (Exception e) {
            LOG.warn("Could not parse purl [{}].", currentPackageNodePurl);
        }

        String packageName = currentPackageNode.optString("name");
        if (!packageName.isEmpty()) {
            packageNameCandidates.add(packageName);
        }

        return packageNameCandidates;
    }

    // TODO: add fileExtensions ".jar" e.g. for Java packages for the id

    private List<String> getVersionCandidates(JSONObject currentPackageNode) {
        List<String> versionCandidates = new ArrayList<>();
        String currentPackageNodePurl = getPurl(currentPackageNode);
        try {
            if (currentPackageNodePurl != null) {
                PackageURL currentPackagePurl = new PackageURL(currentPackageNodePurl);
                versionCandidates.add(currentPackagePurl.getVersion());
            }
        } catch (Exception e) {
            LOG.warn("Could not parse purl [{}].", currentPackageNodePurl);
        }

        String versionInfo = currentPackageNode.optString("versionInfo");
        if (!versionInfo.isEmpty()) {
            versionCandidates.add(versionInfo);
        }

        return versionCandidates;
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }
}

