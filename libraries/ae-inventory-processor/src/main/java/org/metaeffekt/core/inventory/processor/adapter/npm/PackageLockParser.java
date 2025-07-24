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
import org.metaeffekt.core.inventory.processor.adapter.NpmModule;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public abstract class PackageLockParser {

    @Getter
    private final File file;

    /**
     * Maps different representations (paths) to NpmModules. One NpmModule may be mapped by several paths.
     */
    @Getter
    private Map<String, NpmModule> pathModuleMap;

    protected PackageLockParser(File file) {
        this.file = file;
    }

    public abstract void parseModules(WebModule webModule) throws IOException;

    public NpmModule resolveNpmModule(NpmModule dependentModule, String name, String versionRange) {
        NpmModule npmModule;

        if (dependentModule != null) {
            String dependentModulePath = dependentModule.getPath();

            npmModule = pathModuleMap.get(dependentModulePath + "/node_modules/" + name);
            if (npmModule != null) return npmModule;

            npmModule = pathModuleMap.get(dependentModulePath + "/" + name);
            if (npmModule != null) return npmModule;
        }

        npmModule = pathModuleMap.get("node_modules/" + name);
        if (npmModule != null) return npmModule;

        npmModule = pathModuleMap.get(name);
        if (npmModule != null) return npmModule;

        return npmModule;
    }

    protected void setPathModuleMap(Map<String, NpmModule> pathModuleMap) {
        this.pathModuleMap = pathModuleMap;
    }

}
