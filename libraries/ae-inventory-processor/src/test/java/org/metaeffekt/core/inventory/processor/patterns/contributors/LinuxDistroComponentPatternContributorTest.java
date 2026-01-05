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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;

public class LinuxDistroComponentPatternContributorTest {

    private final LinuxDistributionAssetContributor contributor = new LinuxDistributionAssetContributor();

    @Test
    public void testVariant001() {
        File file = new File("src/test/resources/component-pattern-contributor/linux-distro-001");

        final List<ComponentPatternData> cpdList = contributor.contribute(file, "etc/issue", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        Assertions.assertThat(cpd.get(COMPONENT_PART)).isEqualTo("rhel-9.4");
        Assertions.assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("Red Hat Enterprise Linux");
        Assertions.assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("9.4");
    }

    @Test
    public void testVariant002() {
        File file = new File("src/test/resources/component-pattern-contributor/linux-distro-002");

        final List<ComponentPatternData> cpdList = contributor.contribute(file, "etc/issue", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        Assertions.assertThat(cpd.get(COMPONENT_PART)).isEqualTo("debian-11");
        Assertions.assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("Debian GNU/Linux");
        Assertions.assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("11");
    }

    @Test
    public void testVariant003() {
        File file = new File("src/test/resources/component-pattern-contributor/linux-distro-003");

        final List<ComponentPatternData> cpdList = contributor.contribute(file, "etc/issue", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        Assertions.assertThat(cpd.get(COMPONENT_PART)).isEqualTo("alpine-3.20.2");
        Assertions.assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("Alpine Linux");
        Assertions.assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("3.20.2");
    }

    @Test
    public void testVariant004() {
        File file = new File("src/test/resources/component-pattern-contributor/linux-distro-004");

        final List<ComponentPatternData> cpdList = contributor.contribute(file, "os-01/etc/issue", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        Assertions.assertThat(cpd.get(COMPONENT_PART)).isEqualTo("alpine-3.20.2");
        Assertions.assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("Alpine Linux");
        Assertions.assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("3.20.2");
    }

}