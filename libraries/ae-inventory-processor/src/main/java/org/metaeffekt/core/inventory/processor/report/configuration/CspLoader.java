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
package org.metaeffekt.core.inventory.processor.report.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
public class CspLoader {
    private File file;
    private List<File> files = new ArrayList<>();
    private String inlineOverwriteJson;

    private List<String> activeIds = new ArrayList<>();

    // validation
    private boolean failOnMissingPolicyFile = true;
    private boolean failOnVersionIncompatibility = true;
    private boolean failOnMissingVersion = false;

    public void addFile(File... files) {
        this.files.addAll(Arrays.asList(files));
    }

    public List<File> getFiles() {
        final List<File> policyFiles = new ArrayList<>(files);
        if (file != null) policyFiles.add(file);
        return Collections.unmodifiableList(policyFiles);
    }

    public CentralSecurityPolicyConfiguration loadConfiguration() {
        try {
            return this.loadConfigurationInternal();
        } catch (Exception e) {
            log.error("└── Failed to load Security Policy");
            log.error("    ├── {}", e.getMessage());
            log.error("    └── file={}, files={}, inlineOverwriteJson={}, activeIds={}", file, files, inlineOverwriteJson, activeIds);
            throw new RuntimeException("Central security policy loader failed to create Central security policy instance from parameters.", e);
        }
    }

    protected CentralSecurityPolicyConfiguration loadConfigurationInternal() throws IOException {
        final List<File> policyFiles = this.getFiles();

        if (policyFiles.isEmpty() && StringUtils.isEmpty(inlineOverwriteJson)) {
            if (failOnMissingPolicyFile) {
                throw new IllegalStateException("No valid security policy files were provided and property failOnMissingPolicyFile is set to true.");
            }

            log.info("No policyFiles or inlineOverwriteJson were provided, using default Central Security Policy instance");
            return new CentralSecurityPolicyConfiguration();
        }

        if (activeIds.isEmpty()) {
            log.info("Security Policy Loader initialized, no active Ids provided (using defaults/inline only)");
        } else {
            log.info("Security Policy Loader initialized for active Ids: {}", activeIds);
        }

        final List<CspLoaderEntry> allEntries = new ArrayList<>();
        for (File policyFile : policyFiles) {
            log.info("├── Loading: file://{}", canonicalOrAbsolute(policyFile));
            final List<CspLoaderEntry> entries = CspLoaderEntry.fromFile(this, policyFile);
            log.info("│   └── File provides {} configuration{}: {}", entries.size(), entries.size() == 1 ? "" : "s", entries.stream().map(CspLoaderEntry::getId).collect(Collectors.toList()));
            allEntries.addAll(entries);
        }

        final Map<String, CspLoaderEntry> idToEntryMap = new HashMap<>();
        for (CspLoaderEntry entry : allEntries) {
            idToEntryMap.put(entry.getId(), entry);
        }

        log.info("└── Resolving active configurations");
        final List<CspLoaderEntry> activeEntries = new ArrayList<>();

        // the ones that are active by default must be loaded first
        allEntries.stream().filter(CspLoaderEntry::isActiveByDefault).forEach(entry -> {
            log.info("    ├── [{}] (active by default)", entry.getId());
            activeEntries.add(entry);
        });

        // search the user-specified ids in the known events
        for (String activeId : activeIds) {
            final CspLoaderEntry foundEntry = idToEntryMap.get(activeId);
            if (foundEntry == null) {
                throw new IllegalStateException("Configured Configuration Id [" + activeId + "] does not exist");
            }
            if (!activeEntries.contains(foundEntry)) {
                log.info("    ├── [{}]", foundEntry.getId());
                activeEntries.add(foundEntry);
            }
        }

        final Set<CspLoaderEntry> checkedExtends = new HashSet<>();
        final Queue<CspLoaderEntry> checkExtends = new LinkedList<>(activeEntries);
        while (!checkExtends.isEmpty()) {
            final CspLoaderEntry current = checkExtends.poll();
            if (!checkedExtends.add(current)) continue;

            for (String extendedId : current.getExtendsEntries()) {
                final CspLoaderEntry extendsEntry = idToEntryMap.get(extendedId);
                if (extendsEntry == null) {
                    throw new IllegalStateException("Configuration [" + current.getId() + "] extends entry does not exist [" + extendedId + "]");
                }
                if (!activeEntries.contains(extendsEntry)) {
                    log.info("    ├── [{}] (dependency of {})", extendsEntry.getId(), current.getId());
                    activeEntries.add(0, extendsEntry);
                    checkExtends.add(extendsEntry);
                }
            }
        }

        final JSONObject mergedConfig = new JSONObject();
        for (CspLoaderEntry entry : activeEntries) {
            log.debug("    Merging [{}]: {}", entry.getId(), entry.getConfiguration());
            mergeJson(mergedConfig, entry.getConfiguration());
        }

        final boolean hasOverwrite = inlineOverwriteJson != null && !inlineOverwriteJson.trim().isEmpty();
        if (hasOverwrite) {
            final JSONObject json = new JSONObject(inlineOverwriteJson);
            log.info("    ├── Inline overwrite JSON with {} keys", json.keySet().size());
            mergeJson(mergedConfig, json);
        }

        log.debug("    ├── {}", mergedConfig);
        final int mergedConfigurations = activeEntries.size() + (hasOverwrite ? 1 : 0);
        log.info("    └── Merged {} configuration{}", mergedConfigurations, mergedConfigurations == 1 ? "" : "s");

        return CentralSecurityPolicyConfiguration.fromJson(mergedConfig, "Central security policy failed to parse effective security policy configuration");
    }

    private void mergeJson(JSONObject target, JSONObject source) {
        for (String key : source.keySet()) {
            target.put(key, source.get(key));
        }
    }

    @Data
    private static class CspLoaderEntry {
        private final File originFile;
        private String id;
        private boolean activeByDefault = false;
        private final Set<String> extendsEntries = new HashSet<>();
        private JSONObject configuration;

        private static final Set<String> ALLOWED_KEYS = new HashSet<>(Arrays.asList("id", "extends", "activeByDefault", "configuration"));

        public static List<CspLoaderEntry> fromFile(CspLoader loader, File file) throws IOException {
            if (file == null || !file.exists() || !file.isFile()) {
                throw new FileNotFoundException("CSP file must exist but was not found: file://" + canonicalOrAbsolute(file));
            }

            final List<CspLoaderEntry> entries = new ArrayList<>();
            // in the past there was a case where there were multiple empty lines at the start of a file.
            // it was therefore completely ignored by the if-else later in this method.
            // we now strip the content and throw an exception if we cannot determine the type of the file.
            final String content = StringUtils.strip(FileUtils.readFileToString(file, StandardCharsets.UTF_8));

            if (content.startsWith("[")) {
                entries.addAll(fromJsonArray(loader, file, new JSONArray(content)));

            } else if (content.startsWith("{")) {
                entries.addAll(fromJsonObject(loader, file, new JSONObject(content)));

            } else {
                throw new IllegalArgumentException("CSP file content must be a JSON Object or JSON Array: file://" + file.getAbsoluteFile() +
                        "\nBut was: " + content);
            }

            return entries;
        }

        private static List<CspLoaderEntry> fromJsonObject(CspLoader loader, File file, JSONObject json) {
            final List<CspLoaderEntry> entries = new ArrayList<>();

            if (json.has("configurations")) {
                final String policyId = json.optString("id", file.getName());
                final String policyName = json.optString("name", null);
                final String policyDescription = json.optString("description", null);

                if (policyName != null) log.info("│   ├── {} ({})", policyName, policyId);
                else log.info("│   ├── {}", policyId);
                if (policyDescription != null) log.info("│   ├── {}", policyDescription);

                final String version = json.optString("version");
                if (StringUtils.isEmpty(version) && loader.failOnMissingVersion) {
                    throw new IllegalStateException("'failOnMissingVersion' is active and policy file did not contain a version field. Latest version is [" + CentralSecurityPolicyConfiguration.LATEST_VERSION + "] in file://" + canonicalOrAbsolute(file));
                } else if (StringUtils.isNotEmpty(version) && !CentralSecurityPolicyConfiguration.LATEST_VERSION.equals(version) && loader.failOnVersionIncompatibility) {
                    throw new IllegalStateException("'failOnVersionIncompatibility' is active and policy file version [" + version + "] does not match latest version [" + CentralSecurityPolicyConfiguration.LATEST_VERSION + "] in file://" + canonicalOrAbsolute(file));
                }

                final JSONArray jsonArray = json.optJSONArray("configurations");
                if (jsonArray == null) {
                    throw new IllegalStateException("Key 'configurations' is in an invalid format, expected JSON Array in file://" + canonicalOrAbsolute(file));
                }
                entries.addAll(fromJsonArray(loader, file, jsonArray));

            } else {
                final CspLoaderEntry entry = new CspLoaderEntry(file);
                entry.id = file.getName() + UUID.randomUUID();
                entry.configuration = json;
                entry.activeByDefault = true;
                entries.add(entry);
            }

            return entries;
        }

        private static List<CspLoaderEntry> fromJsonArray(CspLoader loader, File file, JSONArray jsonArray) {
            final List<CspLoaderEntry> entries = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject jsonEntry = jsonArray.optJSONObject(i);
                if (jsonEntry == null) {
                    throw new IllegalStateException("Expected JSON Object in file://" + canonicalOrAbsolute(file) + " but got: " + jsonArray.get(i));
                }

                final Set<String> keys = jsonEntry.keySet();
                for (String key : keys) {
                    if (!ALLOWED_KEYS.contains(key)) {
                        throw new IllegalStateException("Unknown key in configuration entry [" + key + "] in file://" + canonicalOrAbsolute(file));
                    }
                }

                if (!jsonEntry.has("id")) {
                    throw new IllegalStateException("Missing required key 'id' in configuration entry in file://" + canonicalOrAbsolute(file));
                }
                if (!jsonEntry.has("configuration")) {
                    throw new IllegalStateException("Missing required key 'configuration' in configuration entry [" + jsonEntry.get("id") + "] in file://" + canonicalOrAbsolute(file));
                }

                final CspLoaderEntry entry = new CspLoaderEntry(file);
                entry.id = jsonEntry.getString("id");

                if (jsonEntry.has("extends")) {
                    final Object extendsObj = jsonEntry.get("extends");
                    if (!(extendsObj instanceof JSONArray)) {
                        throw new IllegalStateException("'extends' must be a JSON array in configuration entry [" + entry.id + "] in file://" + canonicalOrAbsolute(file));
                    }
                    final JSONArray extendsArray = (JSONArray) extendsObj;
                    for (int j = 0; j < extendsArray.length(); j++) {
                        final Object val = extendsArray.get(j);
                        if (!(val instanceof String)) {
                            throw new IllegalStateException("All elements of 'extends' must be strings in configuration entry [" + entry.id + "] in file://" + canonicalOrAbsolute(file));
                        }
                        entry.extendsEntries.add((String) val);
                    }
                }

                if (jsonEntry.has("activeByDefault")) {
                    final Object activeObj = jsonEntry.get("activeByDefault");
                    if (!(activeObj instanceof Boolean)) {
                        throw new IllegalStateException("'activeByDefault' must be a boolean in configuration entry [" + entry.id + "] in file://" + canonicalOrAbsolute(file));
                    }
                    entry.activeByDefault = (Boolean) activeObj;
                }

                final Object config = jsonEntry.get("configuration");
                if (config instanceof String) {
                    final File nestedFile = new File(file.getParentFile(), (String) config);
                    try {
                        entry.configuration = new JSONObject(FileUtils.readFileToString(nestedFile, StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to load sub-configuration from://" + canonicalOrAbsolute(nestedFile), e);
                    } catch (JSONException e) {
                        throw new IllegalStateException("Invalid JSON in sub-configuration file://" + canonicalOrAbsolute(nestedFile), e);
                    }
                } else if (config instanceof JSONObject) {
                    entry.configuration = (JSONObject) config;
                } else {
                    throw new IllegalStateException("'configuration' must be a string path or JSON object in configuration entry [" + entry.id + "] in file://" + canonicalOrAbsolute(file));
                }

                entries.add(entry);
            }

            return entries;
        }
    }

    private static String canonicalOrAbsolute(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
