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
package org.metaeffekt.core.inventory.processor.linux;

import lombok.Getter;
import org.metaeffekt.core.util.purl.PackageManager;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum describing linux distributions with additional information f.e. carried to build PURLs.
 * Only supported linux distributions, identified by the {@code ID} field of {@code /etc/os-release} file are listed.
 */
@Getter
public enum LinuxDistributionInfo {
    DEBIAN("debian", PackageManager.DEBIAN, "dpkg"),
    UBUNTU("ubuntu", PackageManager.DEBIAN, "dpkg"),
    RHEL("rhel", PackageManager.RPM, "redhat", "rpm"),
    ALPINE("alpine", PackageManager.APK, "apk"),
    ALMA("almalinux", PackageManager.RPM, "rpm"),
    ARCH("arch", PackageManager.ALPM, "pacman"),
    CENTOS("centos", PackageManager.RPM, "rpm"),
    FEDORA("fedora", PackageManager.RPM, "rpm"),
    ROCKY("rocky", PackageManager.RPM, "rpm");

    /**
     * Value of the {@code ID} field in {@code os-release.txt}, for example {@code "ubuntu"}.
     */
    private final String distroName;

    /**
     * Package Manager of the distribution, used f.e. as the PURL type.
     */
    private final PackageManager packageManager;

    /**
     * Namespace used in the purl, f.e. {@code "redhat"} for RHEL.
     */
    private final String purlNamespace;

    /**
     * Tool that manages the installed package database, f.e. {@code "dpkg"} for Debian packages.
     */
    private final String packageTool;

    LinuxDistributionInfo(String distroName, PackageManager packageManager, String packageTool) {
        this(distroName, packageManager, distroName, packageTool);
    }

    LinuxDistributionInfo(String distroName, PackageManager packageManager, String purlNamespace, String packageTool) {
        this.distroName = distroName;
        this.packageManager = packageManager;
        this.purlNamespace = purlNamespace;
        this.packageTool = packageTool;
    }

    /**
     * Returns the enum with the corresponding distribution id.
     *
     * @param id the id of the distribution (by default derived from os-release file)
     * @return the enum with the specified distribution id
     */
    public static Optional<LinuxDistributionInfo> fromId(String id) {
        return Arrays.stream(values())
                .filter(distro -> distro.getDistroName().equalsIgnoreCase(id))
                .findFirst();
    }
}
