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

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;

@Slf4j
public class PackageLockParser1 extends PackageLockParser {

    /**
     * This can be activated on test level to verify the invariants during computation.
     */
    public static boolean CHECK_INVARIANTS = false;

    @Getter
    private final JSONObject object;

    public PackageLockParser1(File file, JSONObject object) {
        super(file);
        this.object = object;
    }

    @Data
    public static class ResolvedModuleData {
        private final String path;
        private final String name;
        private final JSONObject jsonObject;
    }

    @Override
    public void parseModules(WebModule webModule) {
        final Map<String, ResolvedModule> pathModuleMap = new HashMap<>();

        final Stack<ResolvedModuleData> resolvedModuleDataStack = new Stack<>();

        final ResolvedModule rootResolvedModule = new ResolvedModule(webModule.getName(), "");
        parseModuleContent(rootResolvedModule, object, resolvedModuleDataStack);
        pathModuleMap.put(rootResolvedModule.getPath(), rootResolvedModule);
        pathModuleMap.put(rootResolvedModule.getPath(), rootResolvedModule);

        // qualify root module
        setRootModule(rootResolvedModule);

        while (!resolvedModuleDataStack.isEmpty()) {
            final ResolvedModuleData moduleData = resolvedModuleDataStack.pop();

            final ResolvedModule resolvedModule = new ResolvedModule(moduleData.getName(), moduleData.getPath());
            parseModuleContent(resolvedModule, moduleData.getJsonObject(), resolvedModuleDataStack);

            pathModuleMap.put(moduleData.getPath(), resolvedModule);
        }

        setPathModuleMap(pathModuleMap);

        if (CHECK_INVARIANTS) {
            verifyPathModuleMapInvariants(webModule);
        }
    }

    private void parseModuleContent(ResolvedModule dependentModule, JSONObject specificPackage, Stack<ResolvedModuleData> stack) {
         dependentModule.setSourceArchiveUrl(specificPackage.optString("resolved"));
         dependentModule.setHash(specificPackage.optString("integrity"));
         dependentModule.setVersion(specificPackage.optString("version"));

        dependentModule.setDevDependency(specificPackage.optBoolean("dev"));
        dependentModule.setPeerDependency(specificPackage.optBoolean("peer"));
        dependentModule.setOptionalDependency(specificPackage.optBoolean("optional"));

        // requires uses a name to version range map
        final Map<String, UnresolvedModule> required = collectNameVersionRangeMap(specificPackage, "requires");

        final Map<String, UnresolvedModule> development = collectResolvedModuleMap(specificPackage, "dependencies", stack, o -> o.optBoolean("dev"), dependentModule);
        final Map<String, UnresolvedModule> runtime = collectResolvedModuleMap(specificPackage, "dependencies", stack, o -> !o.optBoolean("dev"), dependentModule);

        // combine required and runtime
        runtime.putAll(required);

        dependentModule.setRuntimeDependencies(runtime);
        dependentModule.setDevDependencies(development);
    }

    protected Map<String, UnresolvedModule> collectResolvedModuleMap(JSONObject specificPackage, String attribute, Stack<ResolvedModuleData> stack, Predicate<JSONObject> filter, ResolvedModule dependentModule) {
        final Map<String, UnresolvedModule> pathVersionMap = new HashMap<>();
        final JSONObject jsonObject = specificPackage.optJSONObject(attribute);
        if (jsonObject != null) {
            for (String name : jsonObject.keySet()) {
                final JSONObject dependency = jsonObject.getJSONObject(name);
                final String version = dependency.optString("version");
                if (version != null) {
                    if (filter == null || filter.test(dependency)) {
                        String fullPath = buildFullPath(dependentModule, name);
                        pathVersionMap.put(fullPath, new UnresolvedModule(name, fullPath, version));
                        final ResolvedModuleData moduleData = new ResolvedModuleData(fullPath, name, dependency);
                        // only push those that pass the filter (location) and have not been put on the stack yet
                        if (!stack.contains(moduleData)) {
                            stack.push(moduleData);
                        }
                    }
                }
            }
        }
        return pathVersionMap;
    }

    private static String buildFullPath(ResolvedModule dependentModule, String path) {
        final String dependentModulePath = dependentModule.getPath();
        if (StringUtils.isNotBlank(dependentModulePath)) {
            return dependentModule.getPath() + "/npm_modules/" + path;
        } else {
            return path;
        }
    }

    @Override
    public ResolvedModule resolveNpmModule(ResolvedModule dependentModule, String path, String versionRange) {
        ResolvedModule resolvedModule = resolveNpmModule(path);
        if (resolvedModule != null) return resolvedModule;

        final String dependentModulePath = dependentModule.getPath();
        if (StringUtils.isNotBlank(dependentModulePath)) {

            String queryPath = dependentModulePath + "/npm_modules/" + path;
            resolvedModule = resolveNpmModule(queryPath);
            if (resolvedModule != null) return resolvedModule;

            queryPath = dependentModule.getName() + "/npm_modules/" + path;
            resolvedModule = resolveNpmModule(queryPath);
            if (resolvedModule != null) return resolvedModule;

            // search one level up
            int slashIndex = dependentModulePath.lastIndexOf("/npm_modules/");
            if (slashIndex != -1) {
                String parentModulePath = dependentModulePath.substring(0, slashIndex);
                queryPath = parentModulePath + "/npm_modules/" + path;
                resolvedModule = resolveNpmModule(queryPath);
                if (resolvedModule != null) return resolvedModule;
            }
        }

        return resolvedModule;
    }

}
