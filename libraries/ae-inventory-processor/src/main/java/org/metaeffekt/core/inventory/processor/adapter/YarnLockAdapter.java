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
import org.apache.commons.lang3.tuple.Pair;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Extracts an inventory for production npm modules based on a yarn.lock file.
 */
public class YarnLockAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(YarnLockAdapter.class);

    public Inventory extractInventory(File yarnLock, String relativePath) {
        try {
            final List<String> lines = FileUtils.readLines(yarnLock, FileUtils.ENCODING_UTF_8);

            Map<String, NpmPackageLockAdapter.NpmModule> webModuleMap = new HashMap<>();

            String currentModuleId = null;

            for (String line : lines) {
                if (line.startsWith("#")) continue;
                if (StringUtils.isBlank(line)) continue;
                if (line.startsWith("  dependencies:")) continue;
                // FIXME: how to capture optional dependencies
                if (line.startsWith("  optionalDependencies:")) continue;
                if (line.startsWith("    ")) {
                    // dependency
                    final Pair<String, String> keyValuePair = extractKeyValuePair(line);
                    if (keyValuePair == null) continue;

                    // process attribute
                    // FIXME: currently we do not evaluate the dependencies; must be
                    //  consolidated with the dependencies from package.json (dev/prod)
                    continue;
                }
                if (line.startsWith("  ")) {
                    // attribute
                    final Pair<String, String> keyValuePair = extractKeyValuePair(line);

                    if (keyValuePair == null) continue;

                    // process attribute
                    NpmPackageLockAdapter.NpmModule npmModule = webModuleMap.get(currentModuleId);
                    if ("version".equalsIgnoreCase(keyValuePair.getKey())) {
                        npmModule.setVersion(keyValuePair.getRight());
                    }
                    if ("resolved".equalsIgnoreCase(keyValuePair.getKey())) {
                        npmModule.setUrl(keyValuePair.getRight());
                    }
                    continue;
                }
                // module
                line = line.trim();

                line = trimColon(line);

                final Pair<String, String> nameVersionPair = extractNameVersionPair(line);
                if (nameVersionPair == null) continue;
                NpmPackageLockAdapter.NpmModule webModule =
                        new NpmPackageLockAdapter.NpmModule(nameVersionPair.getKey(), relativePath);

                final String webModuleId = line;
                webModuleMap.put(webModuleId, webModule);

                // subsequent parse events are centric to this module
                currentModuleId = webModuleId;
            }

            return createInventory(webModuleMap, relativePath);

        } catch (Exception e) {
            LOG.warn("Cannot read / parse [{}].", yarnLock.getAbsoluteFile());
        }

        return null;
    }

    private Inventory createInventory(Map<String, NpmPackageLockAdapter.NpmModule> webModuleMap, String path) {
        Inventory inventory = new Inventory();

        NpmPackageLockAdapter.populateInventory(inventory, path, webModuleMap.values());

        return inventory;
    }

    private String trimColon(String line) {
        if (line.endsWith(":")) {
            line = line.substring(0, line.length() - 1);
        }
        return line;
    }

    private Pair<String, String> extractKeyValuePair(String line) {
        line = line.trim();
        int separatorIndex = line.indexOf(" ");

        if (separatorIndex == -1) {
            LOG.error("Cannot parse " + line);
            return null;
        }

        final String key = line.substring(0, separatorIndex);
        final String value = line.substring(separatorIndex + 1);
        return Pair.of(trimQuotes(key), trimQuotes(value));
    }

    private Pair<String, String> extractNameVersionPair(String line) {
        line = line.trim();

        int commaIndex = line.lastIndexOf(",");
        if (commaIndex != -1) {
            line = line.substring(commaIndex + 1).trim();
        }

        line = trimQuotes(line);

        int separatorIndex = line.lastIndexOf("@");
        if (separatorIndex == -1) {
            LOG.error("Cannot parse " + line);
            return null;
        }

        final String key = line.substring(0, separatorIndex);

        if (StringUtils.isEmpty(key)) {
            LOG.error("Cannot parse " + line);
        }

        final String value = line.substring(separatorIndex + 1);

        return Pair.of(trimQuotes(key), trimQuotes(value));
    }

    private String trimQuotes(String key) {
        if (key.startsWith("\"") && key.endsWith("\"")) {
            key = key.substring(1, key.length() - 1);
        }
        return key;
    }

}
