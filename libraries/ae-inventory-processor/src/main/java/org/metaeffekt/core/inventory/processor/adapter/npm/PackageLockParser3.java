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
package org.metaeffekt.core.inventory.processor.adapter.npm;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.NpmModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PackageLockParser3 extends PackageLockParser {

    @Getter
    private final JSONObject object;

    public PackageLockParser3(File file, JSONObject object) {
        super(file);
        this.object = object;
    }

    public void parseModules(WebModule webModule) {
        // detect format variant; the one with packages has an additional upper level.
        JSONObject allPackages = object;
        JSONObject packages = object.optJSONObject("packages");
        JSONObject specificPackage = null;

        final Map<String, NpmModule> pathModuleMap = new HashMap<>();
        final Map<String, NpmModule> qualifierModuleMap = new HashMap<>();

        if (packages != null) {
            // change level
            allPackages = packages;

            if (StringUtils.isNotBlank(webModule.getName())) {
                specificPackage = packages.optJSONObject("");
                if (!webModule.getName().equals(specificPackage.optString("name"))) {
                    specificPackage = null;
                }
                if (specificPackage == null) {
                    specificPackage = packages.optJSONObject(webModule.getName());
                }
                if (specificPackage == null) {
                    log.warn("Matching package in [{}] with name [{}] not found.", getFile().getAbsolutePath(), webModule.getName());
                } else {
                    final NpmModule npmModule = new NpmModule(webModule.getName(), webModule.getName());
                    parseModuleContent(npmModule, specificPackage);

                    final String qualifier = npmModule.deriveQualifier();
                    pathModuleMap.put(webModule.getName(), npmModule);
                    qualifierModuleMap.put(qualifier, npmModule);
                }
            }
        }

        // build map with all modules covered by the lock file
        final String prefix = "node_modules/";

        for (String path : allPackages.keySet()) {
            String name = path;

            int index = name.lastIndexOf(prefix);
            if (index != -1) {
                name = name.substring(index + prefix.length());
            }

            final JSONObject jsonObject = allPackages.getJSONObject(path);

            final NpmModule npmModule = new NpmModule(name, path);
            parseModuleContent(npmModule, jsonObject);

            if (StringUtils.isNotBlank(npmModule.getName())) {
                final String qualifier = npmModule.deriveQualifier();
                final NpmModule qualifiedModule = qualifierModuleMap.get(qualifier);
                if (qualifiedModule == null) {
                    pathModuleMap.put(path, npmModule);
                    qualifierModuleMap.put(qualifier, npmModule);
                } else {
                    pathModuleMap.put(path, qualifiedModule);
                }
            }
        }

        setPathModuleMap(pathModuleMap);
    }

    private void parseModuleContent(NpmModule npmModule, JSONObject specificPackage) {
        npmModule.setUrl(specificPackage.optString("resolved"));
        npmModule.setHash(specificPackage.optString("integrity"));
        npmModule.setVersion(specificPackage.optString("version"));

        npmModule.setDevDependency(specificPackage.optBoolean("dev"));
        npmModule.setPeerDependency(specificPackage.optBoolean("peer"));
        npmModule.setOptionalDependency(specificPackage.optBoolean("optional"));

        npmModule.setRuntimeDependencies(collectModuleMap(specificPackage, "dependencies"));
        npmModule.setDevDependencies(collectModuleMap(specificPackage, "devDependencies"));
        npmModule.setPeerDependencies(collectModuleMap(specificPackage, "peerDependencies"));
        npmModule.setOptionalDependencies(collectModuleMap(specificPackage, "optionalDependencies"));
    }

    protected Map<String, String> collectModuleMap(JSONObject specificPackage, String dependencies) {
        final Map<String, String> nameVersionMap = new HashMap<>();
        if (dependencies != null) {
            JSONObject jsonObject = specificPackage.optJSONObject(dependencies);
            if (jsonObject != null) {
                Map<String, Object> map = jsonObject.toMap();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    nameVersionMap.put(entry.getKey(), (String) entry.getValue());
                }
            }
        }
        return nameVersionMap;
    }

}
