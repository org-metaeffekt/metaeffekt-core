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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModuleDependency;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
public abstract class PackageLockParser {

    public static final String PATH_SEPARATOR = "/";

    @Getter
    private final File file;

    /**
     * Maps different representations (paths) to NpmModules. One NpmModule may be mapped by several paths.
     */
    @Getter
    private Map<String, ResolvedModule> pathModuleMap;

    @Setter
    @Getter
    private Set<String> runtimeEnvironmentModules = new HashSet<>();

    @Getter
    @Setter
    private ResolvedModule rootModule;

    protected PackageLockParser(File file) {
        this.file = file;
    }

    /**
     * <p>
     *     Parses all the modules relevant for the WebModule. Builds the internal pathModuleMap, which maps paths,
     *     where the module was detected, to the instantiated NpmModules.
     * </p>
     * <p>
     *     The resulting NpmModules contain different levels of information. The downstream relationship is kept in
     *     the Map&lt;String, ModuleData&gt; instances for the different dependency type.
     * </p>
     * <p>
     *     As invariant the keys in the &lt;String, ModuleData&gt; maps need to be resolvable via the pathModuleMap.
     * </p>
     *
     * @param webModule The web module being the root of the dependency tree.
     *
     * @throws IOException May be thrown in case of file system access.
     */
    public abstract void parseModules(WebModule webModule) throws IOException;

    public ResolvedModule resolveNpmModule(ResolvedModule dependentModule, String path, String versionRange) {
        ResolvedModule resolvedModule;

        if (StringUtils.isNotBlank(dependentModule.getPath())) {
            String dependentModulePath = dependentModule.getPath();

            resolvedModule = resolveNpmModule(dependentModulePath + PATH_SEPARATOR + "node_modules" + PATH_SEPARATOR + path);
            if (resolvedModule != null) return resolvedModule;

            // should be obsolete
            resolvedModule = resolveNpmModule(dependentModulePath + PATH_SEPARATOR + path);
            if (resolvedModule != null) return resolvedModule;
        }

        resolvedModule = resolveNpmModule("node_modules" + PATH_SEPARATOR + path);
        if (resolvedModule != null) return resolvedModule;

        resolvedModule = resolveNpmModule(path);
        if (resolvedModule != null) return resolvedModule;

        return resolvedModule;
    }

    protected ResolvedModule resolveNpmModule(String queryPath) {
        final ResolvedModule resolvedModule = getPathModuleMap().get(queryPath);
        if (log.isDebugEnabled()) {
            if (resolvedModule != null) {
                log.debug("Resolving NPM module with query path [{}]: {}", queryPath, resolvedModule.deriveQualifier());
            } else {
                log.debug("Resolving NPM module with query path [{}]: null", queryPath);
            }
        }
        return resolvedModule;
    }

    protected void setPathModuleMap(Map<String, ResolvedModule> pathModuleMap) {
        this.pathModuleMap = pathModuleMap;
    }

    protected Map<String, UnresolvedModule> collectNameVersionRangeMap(JSONObject specificPackage, String dependencies) {
        final Map<String, UnresolvedModule> nameVersionMap = new HashMap<>();
        if (dependencies != null) {
            JSONObject jsonObject = specificPackage.optJSONObject(dependencies);
            if (jsonObject != null) {
                Map<String, Object> map = jsonObject.toMap();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    nameVersionMap.put(entry.getKey(), new UnresolvedModule(entry.getKey(), null, String.valueOf(entry.getValue())));
                }
            }
        }
        return nameVersionMap;
    }

    protected void propagateDependencyDetails(WebModule webModule) {
        final List<WebModuleDependency> directDependencies = webModule.getDirectDependencies();

        if (directDependencies == null || directDependencies.isEmpty()) return;

        final Map<String, UnresolvedModule> runtimeDependencies = new HashMap<>();
        final Map<String, UnresolvedModule> devDependencies = new HashMap<>();
        final Map<String, UnresolvedModule> peerDependencies = new HashMap<>();
        final Map<String, UnresolvedModule> optionalDependencies = new HashMap<>();

        for (WebModuleDependency wmd : directDependencies) {
            final ResolvedModule dependencyModule = resolveNpmModule(rootModule, wmd.getName(), wmd.getVersionRange());
            if (dependencyModule != null) {
                // module dependency attributes in the context
                dependencyModule.setRuntimeDependency(wmd.isRuntimeDependency());
                dependencyModule.setDevDependency(wmd.isDevDependency());
                dependencyModule.setOptionalDependency(wmd.isOptionalDependency());
                dependencyModule.setPeerDependency(wmd.isPeerDependency());

                UnresolvedModule unresolvedModule = new UnresolvedModule(wmd.getName(), wmd.getName() + "-" + wmd.getVersionRange(), wmd.getVersionRange());

                if (wmd.isRuntimeDependency()) {
                    runtimeDependencies.put(wmd.getName(), unresolvedModule);
                }
                if (wmd.isDevDependency()) {
                    devDependencies.put(wmd.getName(), unresolvedModule);
                }
                if (wmd.isPeerDependency()) {
                    peerDependencies.put(wmd.getName(), unresolvedModule);
                }
                if (wmd.isOptionalDependency()) {
                    optionalDependencies.put(wmd.getName(), unresolvedModule);
                }

            } else {
                if (!getRuntimeEnvironmentModules().contains(wmd.getName())) {
                    log.warn("Module [{}] not found using version range [{}].", wmd.getName(), wmd.getVersionRange());
                }
            }
        }

        rootModule.setRuntimeDependencies(runtimeDependencies);
        rootModule.setDevDependencies(devDependencies);
        rootModule.setPeerDependencies(peerDependencies);
        rootModule.setOptionalDependencies(optionalDependencies);
    }

    protected void verifyPathModuleMapInvariants(WebModule webModule) {
        final Map<String, ResolvedModule> pathModuleMap = getPathModuleMap();
        for (ResolvedModule resolvedModule : pathModuleMap.values()) {

            // check whether module can by found by its path
            final ResolvedModule mappedResolvedModule = pathModuleMap.get(resolvedModule.getPath());
            if (mappedResolvedModule != resolvedModule) {
                throw new IllegalStateException("Module [" + resolvedModule.getName() + "] does not map to itself.");
            }

            // check for null key
            for (String key : pathModuleMap.keySet()) {
                if (key == null) {
                    throw new IllegalStateException("Path module map contains null key.");
                }
            }

            checkDependenciesMap(mappedResolvedModule.getRuntimeDependencies(), pathModuleMap);
            checkDependenciesMap(mappedResolvedModule.getDevDependencies(), pathModuleMap);
            checkDependenciesMap(mappedResolvedModule.getOptionalDependencies(), pathModuleMap);
            checkDependenciesMap(mappedResolvedModule.getPeerDependencies(), pathModuleMap);
        }
    }

    private static void checkDependenciesMap(Map<String, UnresolvedModule> dependencies, Map<String, ResolvedModule> pathModuleMap) {
        if (dependencies == null || dependencies.isEmpty()) return;
        for (UnresolvedModule unresolvedModule : dependencies.values()) {
            String modulePath = unresolvedModule.getPath();
            if (modulePath != null) {
                final ResolvedModule mappedModuleData = pathModuleMap.get(modulePath);
                if (mappedModuleData == null) {
                    for (String key : pathModuleMap.keySet()) {
                        if (key.endsWith(modulePath)) {
                            break;
                        }
                        throw new IllegalStateException("Module [" + modulePath + "] does not have prerequisites to map.");
                    }
                }
            }
        }
    }

}
