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

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum defining package manager name representations in a PURL.
 */
@Getter
public enum PackageManager {
    ALPM("alpm"),
    APK("apk"),
    BOWER("bower"),
    CARGO("cargo"),
    COCOAPODS("cocoapods"),
    COMPOSER("composer"),
    CONAN("conan"),
    DEBIAN("deb"),
    GEM("gem"),
    GENERIC("generic"),
    GOLANG("golang"),
    HEX("hex"),
    JENKINS("jenkins"),
    MAVEN("maven"),
    NPM("npm"),
    NUGET("nuget"),
    PUB("pub"),
    PYPI("pypi"),
    RPM("rpm");

    private final String purlType;

    PackageManager(String purlType) {
        this.purlType = purlType;
    }

    /**
     * Returns the enum with the corresponding purl package manager type
     *
     * @param purlType the package manager type
     * @return the enum representing the specified package manager type
     */
    public static Optional<PackageManager> fromPurlType(String purlType) {
        if (purlType == null) {
            return Optional.empty();
        }
        final String normalized = purlType.trim();
        return Arrays.stream(values())
                .filter(t -> t.purlType.equalsIgnoreCase(normalized))
                .findFirst();
    }
}
