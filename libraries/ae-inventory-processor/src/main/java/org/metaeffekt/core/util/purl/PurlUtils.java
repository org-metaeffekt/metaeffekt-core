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

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class providing central methods to build PURLs.
 */
public class PurlUtils {

    /**
     * Build a (canonical) PURL as specified by {@link PackageURLBuilder}.
     *
     * @param packageInfo object containing data for building the PURL
     * @return the canonical PURL
     */
    public static String buildPurl(PackageInfo packageInfo) {
        return buildPackageUrl(packageInfo).canonicalize();
    }

    /**
     * Converts the canonical PURL into human-readable PURL (no '%' characters).
     *
     * @param packageInfo object containing data for building the PURL
     * @return the readable PURL
     */
    public static String buildReadablePurl(PackageInfo packageInfo) {
        return toReadablePurl(buildPackageUrl(packageInfo));
    }

    /**
     * Additional method for building PURLs for NPM where the name (scope) contains both the package name and the namespace which have to be extracted.
     *
     * @param name    the name containing the namespace and package name
     * @param version the version of the package
     * @return the PURL for the npm package
     */
    public static String buildNpmPurl(String name, String version) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        String namespace = null;
        String pkgName = name;
        if (name.startsWith("@")) {
            final int slash = name.indexOf('/');
            if (slash > 0) {
                // split name into package namespace and package name
                namespace = name.substring(0, slash);
                pkgName = name.substring(slash + 1);
            }
        }

        return buildPurl(PackageInfo.of(PackageManager.NPM, pkgName).namespace(namespace).version(normalizeVersion(version)).build());
    }

    /**
     * Normalizes a version by removing any leading 'v'.
     *
     * @param version the raw version string
     * @return normalized version
     */
    public static String normalizeVersion(String version) {
        if (StringUtils.isBlank(version) || "unspecific".equalsIgnoreCase(version)) {
            return null;
        }

        return version.startsWith("v") ? version.substring(1) : version;
    }

    private static PackageURL buildPackageUrl(PackageInfo packageInfo) {
        final PackageManager type = packageInfo.getType();

        try {
            final PackageURLBuilder builder = PackageURLBuilder.aPackageURL()
                    .withType(type.getPurlType())
                    .withNamespace(packageInfo.getNamespace())
                    .withName(packageInfo.getName())
                    .withVersion(packageInfo.getVersion());

            if (packageInfo.getDistro() != null) {
                builder.withQualifier("distro", packageInfo.getDistro());
            }
            if (packageInfo.getArch() != null) {
                builder.withQualifier("arch", packageInfo.getArch());
            }
            packageInfo.getQualifiers().forEach(builder::withQualifier);

            return builder.build();
        } catch (MalformedPackageURLException e) {
            throw new IllegalArgumentException("Invalid PURL for " + packageInfo.getName(), e);
        }
    }

    private static String toReadablePurl(PackageURL purl) {
        final StringBuilder sb = new StringBuilder("pkg:").append(purl.getType()).append('/');

        if (purl.getNamespace() != null && !purl.getNamespace().isEmpty()) {
            sb.append(purl.getNamespace()).append('/');
        }
        sb.append(purl.getName());

        if (purl.getVersion() != null && !purl.getVersion().isEmpty()) {
            sb.append('@').append(purl.getVersion());
        }

        final Map<String, String> qualifiers = purl.getQualifiers();
        if (qualifiers != null && !qualifiers.isEmpty()) {
            sb.append('?').append(qualifiers.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&")));
        }

        if (purl.getSubpath() != null && !purl.getSubpath().isEmpty()) {
            sb.append('#').append(purl.getSubpath());
        }

        return sb.toString();
    }
}