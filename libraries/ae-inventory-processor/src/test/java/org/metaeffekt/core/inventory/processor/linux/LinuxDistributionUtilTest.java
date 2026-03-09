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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class LinuxDistributionUtilTest {

    @Test
    public void test001() {
        File folder = new File("src/test/resources/component-pattern-contributor/linux-distro-001");
        LinuxDistributionUtil.LinuxDistro distro = LinuxDistributionUtil.parseDistro(folder);
        Assertions.assertThat(distro.issue).isEqualTo("Red Hat Enterprise Linux");
        Assertions.assertThat(distro.version).isEqualTo("9.4 (Plow)");
        Assertions.assertThat(distro.id).isEqualTo("rhel");
        Assertions.assertThat(distro.versionId).isEqualTo("9.4");
        Assertions.assertThat(distro.cpe).isEqualTo("cpe:/o:redhat:enterprise_linux:9::baseos");
        Assertions.assertThat(distro.url).isEqualTo("https://www.redhat.com/");
    }

    @Test
    public void test002() {
        File folder = new File("src/test/resources/component-pattern-contributor/linux-distro-002");
        LinuxDistributionUtil.LinuxDistro distro = LinuxDistributionUtil.parseDistro(folder);
        Assertions.assertThat(distro.issue).isEqualTo("Debian GNU/Linux");
        Assertions.assertThat(distro.version).isEqualTo("11.7");
        Assertions.assertThat(distro.id).isEqualTo("debian");
        Assertions.assertThat(distro.versionId).isEqualTo("11");
        Assertions.assertThat(distro.url).isEqualTo("https://www.debian.org/");
    }

    @Test
    public void test003() {
        File folder = new File("src/test/resources/component-pattern-contributor/linux-distro-003");
        LinuxDistributionUtil.LinuxDistro distro = LinuxDistributionUtil.parseDistro(folder);
        Assertions.assertThat(distro.issue).isEqualTo("Alpine Linux");
        Assertions.assertThat(distro.version).isEqualTo("3.20.2");
        Assertions.assertThat(distro.id).isEqualTo("alpine");
        Assertions.assertThat(distro.versionId).isEqualTo("3.20.2");
        Assertions.assertThat(distro.url).isEqualTo("https://alpinelinux.org/");
    }

}