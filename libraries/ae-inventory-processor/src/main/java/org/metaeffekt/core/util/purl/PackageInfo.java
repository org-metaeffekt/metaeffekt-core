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
package org.metaeffekt.core.util.purl;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * Class for encapsulating package metadata used for building PURLs.
 */
@Value
@Builder(toBuilder = true)
public class PackageInfo {
    @NonNull
    PackageManager type;
    @NonNull
    String name;
    String namespace;
    String version;
    String distro;
    String arch;
    // Singular annotation generates qualifier(String key, string value) allowing adding additional qualifiers.
    @Singular
    Map<String, String> qualifiers;

    /**
     * Builder constructor.
     */
    public static class PackageInfoBuilder {
        // extended by lombok automatically
    }

    /**
     * Method to enforce not null package manager type and name at compile time.
     *
     * @param type the package manager type
     * @param name the name of the package
     * @return a partially built PackageDef.
     */
    public static PackageInfoBuilder of(PackageManager type, String name) {
        return builder().type(type).name(name);
    }
}
