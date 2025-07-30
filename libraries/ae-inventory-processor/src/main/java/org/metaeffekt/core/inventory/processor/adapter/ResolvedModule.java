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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ToString
@Getter
public class ResolvedModule {

    /**
     * Name of the module.
     */
    final String name;

    /**
     * Path within the given context or source file the module is detected/located.
     */
    final String path;

    @Setter
    String version;

    @Setter
    String url;

    @Setter
    String hash;

    @Setter
    String sourceArchiveUrl;

    /**
     * Flag indicating that the module was actively resolved by the package manager.
     */
    @Setter
    boolean resolved;

    @Setter
    private boolean isRuntimeDependency;

    @Setter
    private boolean isDevDependency;

    @Setter
    private boolean isPeerDependency;

    @Setter
    private boolean isOptionalDependency;

    @Setter
    private Map<String, UnresolvedModule> devDependencies;

    @Setter
    private Map<String, UnresolvedModule> runtimeDependencies;

    @Setter
    private Map<String, UnresolvedModule> peerDependencies;

    @Setter
    private Map<String, UnresolvedModule> optionalDependencies;

    @ToString.Exclude
    @Setter
    private List<ResolvedModule> dependentModules = new ArrayList<>();

    public ResolvedModule(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String deriveQualifier() {
        if (version != null) {
            return name + "-" + version;
        }
        return name;
    }
}
