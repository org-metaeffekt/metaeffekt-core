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
 * Enum describing distributions and their corresponding package manager.
 */
@Getter
public enum LinuxDistribution {
    DEBIAN("debian", PackageManager.DEBIAN),
    UBUNTU("ubuntu", PackageManager.DEBIAN),
    RHEL("rhel", PackageManager.RPM, "redhat"),
    ALPINE("alpine", PackageManager.APK),
    ALMA("almalinux", PackageManager.RPM),
    ARCH("arch", PackageManager.ALPM),
    CENTOS("centos", PackageManager.RPM),
    FEDORA("fedora", PackageManager.RPM),
    ROCKY("rocky", PackageManager.RPM);

    private final String distroName;
    private final PackageManager packageManager;
    private final String purlNamespace;

    LinuxDistribution(String distroName, PackageManager packageManager) {
        this(distroName, packageManager, distroName);
    }

    LinuxDistribution(String distroName, PackageManager packageManager, String purlNamespace) {
        this.distroName = distroName;
        this.packageManager = packageManager;
        this.purlNamespace = purlNamespace;
    }

    /**
     * Returns the enum with the corresponding distribution id.
     *
     * @param id the id of the distribution (by default derived from os-release file)
     * @return the enum with the specified distribution id
     */
    public static Optional<LinuxDistribution> fromId(String id) {
        return Arrays.stream(values())
                .filter(distro -> distro.getDistroName().equalsIgnoreCase(id))
                .findFirst();
    }
}
