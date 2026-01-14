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
package org.metaeffekt.core.inventory.processor.patterns.contributors.web;

import lombok.Data;

/**
 * Represents a dependency of a WebModule. The qualifying boolean indicate the character of the dependency in the
 * context of the WenModule.
 */
@Data
public class WebModuleDependency {

    private String name;

    /**
     * The version qualifier may be imprecise.
     */
    private String versionRange;

    /**
     * The resolved version.
     */
    private String resolvedVersion;

    // NOTE: these details refer to the outer context
    private boolean isRuntimeDependency;
    private boolean isDevDependency;
    private boolean isPeerDependency;
    private boolean isOptionalDependency;
    private boolean isTestDependency;
    private boolean isOverwriteDependency;

    public String deriveQualifier() {
        if (resolvedVersion != null) {
            return name + "-" + resolvedVersion;
        }
        return name;
    }

    // NOTE: here no dependency tree is covered
}
