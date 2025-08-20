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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ComposerLockParser extends PackageLockParser {

    public ComposerLockParser(File file) {
        super(file);
    }

    @Override
    public void parseModules(WebModule webModule) throws IOException {
        final Map<String, ResolvedModule> pathModuleMap = new HashMap<>();

        // construct and qualify root module; not represented in lock file
        final ResolvedModule rootResolvedModule = new ResolvedModule(webModule.getName(), "");
        rootResolvedModule.setVersion(webModule.getVersion());
        pathModuleMap.put(rootResolvedModule.getPath(), rootResolvedModule);
        setRootModule(rootResolvedModule);

        final JSONObject object = new JSONObject(FileUtils.readFileToString(getFile(), StandardCharsets.UTF_8));

        final JSONObject jsonObject = object.optJSONObject("platform");
        if (jsonObject != null) {
            final Map<String, Object> platformModules = jsonObject.toMap();
            setRuntimeEnvironmentModules(platformModules.keySet());
        }

        contributeModules(object.getJSONArray("packages"), pathModuleMap, true);
        contributeModules(object.getJSONArray("packages-dev"), pathModuleMap, false);

        setPathModuleMap(pathModuleMap);

        propagateDependencyDetails(webModule);
    }

    private void contributeModules(JSONArray packages, Map<String, ResolvedModule> pathModuleMap, boolean runtimeModules) {
        for (int j = 0; j < packages.length(); j++) {
            final JSONObject jsonObject = packages.getJSONObject(j);
            final String name = jsonObject.optString("name");
            String version = jsonObject.optString("version");

            if (version != null && version.startsWith("v")) {
                version = version.substring(1);
            }

            String url = null;
            String sourceArchiveUrl = null;

            // the url is the address of the source repository
            final JSONObject source = jsonObject.optJSONObject("source");
            if (source != null) {
                url = source.optString("url");
            }

            // the dist url is mapped to the source archive url
            final JSONObject dist = jsonObject.optJSONObject("dist");
            if (dist != null) {
                sourceArchiveUrl = dist.optString("url");
            }

            final ResolvedModule resolvedModule = new ResolvedModule(name, name);
            resolvedModule.setUrl(url);
            resolvedModule.setVersion(version);
            resolvedModule.setSourceArchiveUrl(sourceArchiveUrl);

            resolvedModule.setRuntimeDependency(runtimeModules);

            Map<String, UnresolvedModule> runtime = collectNameVersionRangeMap(jsonObject, "require");
            Map<String, UnresolvedModule> development = collectNameVersionRangeMap(jsonObject, "require-dev");

            resolvedModule.setRuntimeDependencies(runtime);
            resolvedModule.setDevDependencies(development);

            pathModuleMap.put(resolvedModule.getPath(), resolvedModule);
        }
    }

}
