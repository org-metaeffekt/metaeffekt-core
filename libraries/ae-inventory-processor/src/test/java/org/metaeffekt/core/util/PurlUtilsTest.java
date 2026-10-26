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
package org.metaeffekt.core.util;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.metaeffekt.core.inventory.processor.linux.LinuxDistribution;
import org.metaeffekt.core.util.purl.PackageInfo;
import org.metaeffekt.core.util.purl.PackageManager;
import org.metaeffekt.core.util.purl.PurlUtils;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class PurlUtilsTest {
    @TestFactory
    Stream<DynamicTest> buildPurl_forEachPackageType() {
        return Stream.of(
                purlTest("debian",
                        PackageInfo.of(PackageManager.DEBIAN, "curl").namespace(LinuxDistribution.DEBIAN.getPurlNamespace()).version("7.88.1-10").distro("debian-12").arch("amd64").build(),
                        "pkg:deb/debian/curl@7.88.1-10?arch=amd64&distro=debian-12",
                        "pkg:deb/debian/curl@7.88.1-10?arch=amd64&distro=debian-12"),
                purlTest("rpm",
                        PackageInfo.of(PackageManager.RPM, "bash").namespace(LinuxDistribution.RHEL.getPurlNamespace()).version("5.1.8-6.el9").distro("rhel-9").arch("x86_64").build(),
                        "pkg:rpm/redhat/bash@5.1.8-6.el9?arch=x86_64&distro=rhel-9",
                        "pkg:rpm/redhat/bash@5.1.8-6.el9?arch=x86_64&distro=rhel-9"),
                purlTest("apk",
                        PackageInfo.of(PackageManager.APK, "busybox").namespace(LinuxDistribution.ALPINE.getPurlNamespace()).version("1.36.1-r5").distro("alpine-3.19").arch("x86_64").build(),
                        "pkg:apk/alpine/busybox@1.36.1-r5?arch=x86_64&distro=alpine-3.19",
                        "pkg:apk/alpine/busybox@1.36.1-r5?arch=x86_64&distro=alpine-3.19"),
                purlTest("alpm",
                        PackageInfo.of(PackageManager.ALPM, "pacman").namespace(LinuxDistribution.ARCH.getPurlNamespace()).version("6.0.2-1").arch("x86_64").build(),
                        "pkg:alpm/arch/pacman@6.0.2-1?arch=x86_64",
                        "pkg:alpm/arch/pacman@6.0.2-1?arch=x86_64"),
                purlTest("maven",
                        PackageInfo.of(PackageManager.MAVEN, "guava").namespace("com.google.guava").version("33.0.0-jre").build(),
                        "pkg:maven/com.google.guava/guava@33.0.0-jre",
                        "pkg:maven/com.google.guava/guava@33.0.0-jre"),
                purlTest("npm with Scope",
                        PackageInfo.of(PackageManager.NPM, "core").namespace("@angular").version("17.0.0").build(),
                        "pkg:npm/%40angular/core@17.0.0",
                        "pkg:npm/@angular/core@17.0.0"),
                // types without namespace
                purlTest("npm without Scope",
                        PackageInfo.of(PackageManager.NPM, "lodash").version("4.17.21").build(),
                        "pkg:npm/lodash@4.17.21",
                        "pkg:npm/lodash@4.17.21"),
                purlTest("cargo",
                        PackageInfo.of(PackageManager.CARGO, "serde").version("1.0.195").build(),
                        "pkg:cargo/serde@1.0.195",
                        "pkg:cargo/serde@1.0.195"),
                purlTest("pypi",
                        PackageInfo.of(PackageManager.PYPI, "requests").version("2.31.0").build(),
                        "pkg:pypi/requests@2.31.0",
                        "pkg:pypi/requests@2.31.0"),
                purlTest("cocoapods",
                        PackageInfo.of(PackageManager.COCOAPODS, "AFNetworking").version("4.0.1").build(),
                        "pkg:cocoapods/AFNetworking@4.0.1",
                        "pkg:cocoapods/AFNetworking@4.0.1"),
                purlTest("conan",
                        PackageInfo.of(PackageManager.CONAN, "openssl").version("3.2.0").build(),
                        "pkg:conan/openssl@3.2.0",
                        "pkg:conan/openssl@3.2.0"),
                purlTest("nuget",
                        PackageInfo.of(PackageManager.NUGET, "Newtonsoft.Json").version("13.0.3").build(),
                        "pkg:nuget/Newtonsoft.Json@13.0.3",
                        "pkg:nuget/Newtonsoft.Json@13.0.3"),
                purlTest("gem",
                        PackageInfo.of(PackageManager.GEM, "rails").version("7.1.2+abc-9").build(),
                        "pkg:gem/rails@7.1.2%2Babc-9",
                        "pkg:gem/rails@7.1.2+abc-9")
        );
    }

    private static DynamicTest purlTest(String name, PackageInfo packageInfo, String expectedPurl, String expectedReadablePurl) {
        return dynamicTest(name, () -> {
            assertEquals(expectedPurl, PurlUtils.buildPurl(packageInfo));
            assertEquals(expectedReadablePurl, PurlUtils.buildReadablePurl(packageInfo));
        });
    }

    @Test
    void explicitNamespaceOverridesDefaultNamespace() {
        final PackageInfo packageInfo = PackageInfo.of(PackageManager.RPM, "bash").namespace("rocky").version("5.1.8-6.el9").distro("rocky-9").arch("x86_64").build();
        assertEquals("pkg:rpm/rocky/bash@5.1.8-6.el9?arch=x86_64&distro=rocky-9", PurlUtils.buildPurl(packageInfo));
    }

    @Test
    void optionalVersion() {
        final PackageInfo packageInfo = PackageInfo.of(PackageManager.CARGO, "serde").build();
        assertEquals("pkg:cargo/serde", PurlUtils.buildPurl(packageInfo));
    }

    @Test
    void nullVersionIgnored() {
        final PackageInfo packageInfo = PackageInfo.of(PackageManager.CARGO, "serde").version(null).build();
        assertEquals("pkg:cargo/serde", PurlUtils.buildPurl(packageInfo));
    }

    @Test
    void blankVersionIgnored() {
        final PackageInfo packageInfo = PackageInfo.of(PackageManager.CARGO, "serde").version("  ").build();
        assertEquals("pkg:cargo/serde", PurlUtils.buildPurl(packageInfo));
    }

    @Test
    void optionalQualifiers() {
        final PackageInfo packageInfo = PackageInfo.of(PackageManager.DEBIAN, "curl").namespace(LinuxDistribution.DEBIAN.getPurlNamespace()).version("7.88.1-10").build();
        assertEquals("pkg:deb/debian/curl@7.88.1-10", PurlUtils.buildPurl(packageInfo));
    }

    @Test
    void missingTypeThrowingException() {
        assertThrows(NullPointerException.class, () -> PackageInfo.builder().name("curl").build());
    }

    @Test
    void missingNameThrowingException() {
        assertThrows(NullPointerException.class, () -> PackageInfo.builder().type(PackageManager.DEBIAN).build());
    }
}
