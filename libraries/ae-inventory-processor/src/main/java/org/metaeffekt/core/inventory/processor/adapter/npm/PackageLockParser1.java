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

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.ModuleData;
import org.metaeffekt.core.inventory.processor.adapter.NpmModule;
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
        final Map<String, NpmModule> pathModuleMap = new HashMap<>();

        final Stack<ResolvedModuleData> resolvedModuleDataStack = new Stack<>();

        final NpmModule rootNpmModule = new NpmModule(webModule.getName(), "");
        parseModuleContent(rootNpmModule, object, resolvedModuleDataStack);
        pathModuleMap.put(rootNpmModule.getPath(), rootNpmModule);
        pathModuleMap.put(rootNpmModule.getPath(), rootNpmModule);

        // qualify root module
        setRootModule(rootNpmModule);

        while (!resolvedModuleDataStack.isEmpty()) {
            final ResolvedModuleData moduleData = resolvedModuleDataStack.pop();

            final NpmModule npmModule = new NpmModule(moduleData.getName(), moduleData.getPath());
            parseModuleContent(npmModule, moduleData.getJsonObject(), resolvedModuleDataStack);

            pathModuleMap.put(moduleData.getPath(), npmModule);
        }

        setPathModuleMap(pathModuleMap);

        if (CHECK_INVARIANTS) {
            verifyPathModuleMapInvariants(webModule);
        }
    }

    private void parseModuleContent(NpmModule dependentModule, JSONObject specificPackage, Stack<ResolvedModuleData> stack) {
         dependentModule.setUrl(specificPackage.optString("resolved"));
         dependentModule.setHash(specificPackage.optString("integrity"));
         dependentModule.setVersion(specificPackage.optString("version"));

        dependentModule.setDevDependency(specificPackage.optBoolean("dev"));
        dependentModule.setPeerDependency(specificPackage.optBoolean("peer"));
        dependentModule.setOptionalDependency(specificPackage.optBoolean("optional"));

        // requires uses a name to version range map
        final Map<String, ModuleData> required = collectNameVersionRangeMap(specificPackage, "requires");

        final Map<String, ModuleData> development = collectResolvedModuleMap(specificPackage, "dependencies", stack, o -> o.optBoolean("dev"), dependentModule);
        final Map<String, ModuleData> runtime = collectResolvedModuleMap(specificPackage, "dependencies", stack, o -> !o.optBoolean("dev"), dependentModule);

        // combine required and runtime
        runtime.putAll(required);

        dependentModule.setRuntimeDependencies(runtime);
        dependentModule.setDevDependencies(development);
    }

    protected Map<String, ModuleData> collectResolvedModuleMap(JSONObject specificPackage, String attribute, Stack<ResolvedModuleData> stack, Predicate<JSONObject> filter, NpmModule dependentModule) {
        final Map<String, ModuleData> pathVersionMap = new HashMap<>();
        final JSONObject jsonObject = specificPackage.optJSONObject(attribute);
        if (jsonObject != null) {
            for (String name : jsonObject.keySet()) {
                final JSONObject dependency = jsonObject.getJSONObject(name);
                final String version = dependency.optString("version");
                if (version != null) {
                    if (filter == null || filter.test(dependency)) {
                        String fullPath = buildFullPath(dependentModule, name);
                        pathVersionMap.put(fullPath, new ModuleData(name, fullPath, null, version));
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

    private static String buildFullPath(NpmModule dependentModule, String path) {
        final String dependentModulePath = dependentModule.getPath();
        if (StringUtils.isNotBlank(dependentModulePath)) {
            return dependentModule.getPath() + "/npm_modules/" + path;
        } else {
            return path;
        }
    }

    @Override
    public NpmModule resolveNpmModule(NpmModule dependentModule, String path, String versionRange) {
        NpmModule npmModule = resolveNpmModule(path);
        if (npmModule != null) return npmModule;

        final String dependentModulePath = dependentModule.getPath();
        if (StringUtils.isNotBlank(dependentModulePath)) {

            String queryPath = dependentModulePath + "/npm_modules/" + path;
            npmModule = resolveNpmModule(queryPath);
            if (npmModule != null) return npmModule;

            queryPath = dependentModule.getName() + "/npm_modules/" + path;
            npmModule = resolveNpmModule(queryPath);
            if (npmModule != null) return npmModule;

            // search one level up
            int slashIndex = dependentModulePath.lastIndexOf("/npm_modules/");
            if (slashIndex != -1) {
                String parentModulePath = dependentModulePath.substring(0, slashIndex);
                queryPath = parentModulePath + "/npm_modules/" + path;
                log.info(queryPath);
                npmModule = resolveNpmModule(queryPath);
                if (npmModule != null) return npmModule;
            }
        }

        return npmModule;
    }

}
