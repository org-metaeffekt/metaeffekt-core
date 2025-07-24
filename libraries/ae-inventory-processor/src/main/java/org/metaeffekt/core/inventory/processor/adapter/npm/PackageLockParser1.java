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
import org.apache.commons.math3.util.Pair;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.NpmModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;

@Slf4j
public class PackageLockParser1 extends PackageLockParser {

    @Getter
    private final JSONObject object;

    public PackageLockParser1(File file, JSONObject object) {
        super(file);
        this.object = object;
    }

    @Override
    public void parseModules(WebModule webModule) {
        final Map<String, NpmModule> pathModuleMap = new HashMap<>();

        Stack<Pair<String, JSONObject>> stack = new Stack<>();

        final NpmModule rootNpmModule = new NpmModule(webModule.getName(), webModule.getName());
        parseModuleContent(rootNpmModule, object, stack, pathModuleMap);
        pathModuleMap.put(rootNpmModule.getName(), rootNpmModule);

        while (!stack.isEmpty()) {
            Pair<String, JSONObject> pair = stack.pop();

            String name = pair.getKey();
            JSONObject jsonObject = pair.getValue();

            final NpmModule npmModule = new NpmModule(name, jsonObject.getString("version"));
            parseModuleContent(npmModule, jsonObject, stack, pathModuleMap);
            pathModuleMap.put(name, npmModule);
        }

        setPathModuleMap(pathModuleMap);
    }

    private void parseModuleContent(NpmModule npmModule, JSONObject specificPackage, Stack<Pair<String, JSONObject>> stack, Map<String, NpmModule> pathModuleMap) {
        npmModule.setUrl(specificPackage.optString("resolved"));
        npmModule.setHash(specificPackage.optString("integrity"));
        npmModule.setVersion(specificPackage.optString("version"));

        npmModule.setDevDependency(specificPackage.optBoolean("dev"));
        npmModule.setPeerDependency(specificPackage.optBoolean("peer"));
        npmModule.setOptionalDependency(specificPackage.optBoolean("optional"));

        final Map<String, String> required = collectModuleMap(specificPackage, "required", stack, null);
        final Map<String, String> development = collectModuleMap(specificPackage, "dependencies", stack, o -> o.optBoolean("dev"));
        final Map<String, String> runtime = collectModuleMap(specificPackage, "dependencies", stack, o -> !o.optBoolean("dev"));

        // combine required and runtime
        runtime.putAll(required);

        npmModule.setRuntimeDependencies(runtime);
        npmModule.setDevDependencies(development);
    }

    protected Map<String, String> collectModuleMap(JSONObject specificPackage, String dependencies, Stack<Pair<String, JSONObject>> stack, Predicate<JSONObject> filter) {
        final Map<String, String> nameVersionMap = new HashMap<>();
        if (dependencies != null) {
            JSONObject jsonObject = specificPackage.optJSONObject(dependencies);
            if (jsonObject != null) {
                for (String path : jsonObject.keySet()) {
                    JSONObject dependency = jsonObject.getJSONObject(path);
                    String version = dependency.optString("version");
                    if (version != null) {
                        if (filter == null || filter.test(dependency)) {
                            nameVersionMap.put(path, version);
                            final Pair<String, JSONObject> pair = Pair.create(path, dependency);

                            // only push those that pass the filter (location) and have not been put on the stack yet
                            if (!stack.contains(pair)) {
                                stack.push(pair);
                            }
                        }
                    }
                }
            }
        }
        return nameVersionMap;
    }

}
