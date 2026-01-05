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
package org.metaeffekt.core.inventory.processor.adapter.npm;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
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

        final Map<String, ResolvedModule> pathModuleMap = new HashMap<>();
        final Map<String, ResolvedModule> qualifierModuleMap = new HashMap<>();

        if (packages != null) {
            // change level
            allPackages = packages;

            if (StringUtils.isNotBlank(webModule.getName())) {

                // check whether the package is resolvable by its name, first
                specificPackage = packages.optJSONObject(webModule.getName());

                // check resolve via anonymous package
                if (specificPackage == null) {
                    JSONObject anonymousModule = packages.optJSONObject("");
                    String anonymousModuleName = anonymousModule.optString("name");
                    if (webModule.getName().equals(anonymousModuleName)) {
                        specificPackage = anonymousModule;
                    }
                }

                if (specificPackage == null) {
                    log.warn("Matching package in [{}] with name [{}] not found.", getFile().getAbsolutePath(), webModule.getName());
                } else {
                    final ResolvedModule rootModule = new ResolvedModule(webModule.getName(), "");
                    parseModuleContent(rootModule, specificPackage);

                    final String qualifier = rootModule.deriveQualifier();
                    pathModuleMap.put(webModule.getName(), rootModule);
                    qualifierModuleMap.put(qualifier, rootModule);

                    setRootModule(rootModule);
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

            final ResolvedModule resolvedModule = new ResolvedModule(name, path);
            parseModuleContent(resolvedModule, jsonObject);

            if (StringUtils.isNotBlank(resolvedModule.getName())) {
                final String qualifier = resolvedModule.deriveQualifier();
                final ResolvedModule qualifiedModule = qualifierModuleMap.get(qualifier);
                if (qualifiedModule == null) {
                    pathModuleMap.put(path, resolvedModule);
                    qualifierModuleMap.put(qualifier, resolvedModule);
                } else {
                    pathModuleMap.put(path, qualifiedModule);
                }
            }
        }

        setPathModuleMap(pathModuleMap);

        if (getRootModule() == null) throw new IllegalStateException("Root module not identified for [" +  getFile().getAbsolutePath() + "].");

    }

    private void parseModuleContent(ResolvedModule resolvedModule, JSONObject specificPackage) {
        resolvedModule.setSourceArchiveUrl(specificPackage.optString("resolved"));
        resolvedModule.setHash(specificPackage.optString("integrity"));
        resolvedModule.setVersion(specificPackage.optString("version"));

        resolvedModule.setDevDependency(specificPackage.optBoolean("dev"));
        resolvedModule.setPeerDependency(specificPackage.optBoolean("peer"));
        resolvedModule.setOptionalDependency(specificPackage.optBoolean("optional"));

        resolvedModule.setRuntimeDependencies(collectNameVersionRangeMap(specificPackage, "dependencies"));
        resolvedModule.setDevDependencies(collectNameVersionRangeMap(specificPackage, "devDependencies"));
        resolvedModule.setPeerDependencies(collectNameVersionRangeMap(specificPackage, "peerDependencies"));
        resolvedModule.setOptionalDependencies(collectNameVersionRangeMap(specificPackage, "optionalDependencies"));
    }

}
