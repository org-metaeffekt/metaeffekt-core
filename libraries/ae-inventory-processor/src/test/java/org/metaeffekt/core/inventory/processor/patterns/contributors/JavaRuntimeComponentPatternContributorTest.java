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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.List;

import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;

public class JavaRuntimeComponentPatternContributorTest {

    private final JavaRuntimeComponentPatternContributor contributor = new JavaRuntimeComponentPatternContributor();

    @Test
    public void testVariant001() {
        File file = new File("src/test/resources/component-pattern-contributor/java-runtime-001/release");

        final List<ComponentPatternData> cpdList = contributor.contribute(file.getParentFile(), file.getParent(), "release", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        Assertions.assertThat(cpd.get(COMPONENT_PART)).isEqualTo("temurin-jre-11.0.20.1");
        Assertions.assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("Eclipse Adoptium Temurin");
        Assertions.assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("11.0.20.1");
        Assertions.assertThat(cpd.get("Release")).isEqualTo("1");
    }

    @Test
    public void testVariant002() {
        final File file = new File("src/test/resources/component-pattern-contributor/java-runtime-002/release");

        final JavaRuntimeComponentPatternContributor contributor = new JavaRuntimeComponentPatternContributor();
        final List<ComponentPatternData> cpdList = contributor.contribute(file.getParentFile(), file.getParent(), "release", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        Assertions.assertThat(cpd.get(COMPONENT_PART)).isEqualTo("oracle-jdk-19.0.1");
        Assertions.assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("Oracle JDK");
        Assertions.assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("19.0.1");
        Assertions.assertThat(cpd.get("Release")).isNull();
    }

    @Test
    public void testVariant003() {
        final File file = new File("src/test/resources/component-pattern-contributor/java-runtime-003/openjdk-17/release");

        final List<ComponentPatternData> cpdList = contributor.contribute(file.getParentFile(), file.getParent(), "release", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        Assertions.assertThat(cpd.get(COMPONENT_PART)).isEqualTo("temurin-openjdk-17.0.8.1");
        Assertions.assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("Eclipse Adoptium Temurin");
        Assertions.assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("17.0.8.1");
        Assertions.assertThat(cpd.get("Release")).isEqualTo("1");
    }


    @Test
    public void testVariant004() {
        final File file = new File("src/test/resources/component-pattern-contributor/java-runtime-004/release");

        final List<ComponentPatternData> cpdList = contributor.contribute(file.getParentFile(), file.getParent(), "release", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        Assertions.assertThat(cpd.get(COMPONENT_PART)).isEqualTo("debian-jdk-17.0.8");
        Assertions.assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("Debian JDK");
        Assertions.assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("17.0.8");
        Assertions.assertThat(cpd.get("Release")).isEqualTo("7-Debian-1deb12u1");
    }

    @Test
    public void testVariant005() {
        File file = new File("src/test/resources/component-pattern-contributor/java-runtime-005/release");

        final List<ComponentPatternData> cpdList = contributor.contribute(file.getParentFile(), file.getParent(), "release", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);

        final String componentPart = cpd.get(COMPONENT_PART);
        final String componentName = cpd.get(COMPONENT_NAME);
        final String componentVersion = cpd.get(COMPONENT_VERSION);
        final String componentRelease = cpd.get("Release");

        Assertions.assertThat(componentPart).isEqualTo("bellsoft-jdk-11.0.17");
        Assertions.assertThat(componentName).isEqualTo("BellSoft JDK");
        Assertions.assertThat(componentVersion).isEqualTo("11.0.17");
        Assertions.assertThat(componentRelease).isNull();
    }

    @Test
    public void testVariant006() {
        File file = new File("src/test/resources/component-pattern-contributor/java-runtime-006/release");

        final List<ComponentPatternData> cpdList = contributor.contribute(file.getParentFile(), file.getParent(), "release", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);

        final String componentPart = cpd.get(COMPONENT_PART);
        final String componentName = cpd.get(COMPONENT_NAME);
        final String componentVersion = cpd.get(COMPONENT_VERSION);
        final String componentRelease = cpd.get("Release");

        Assertions.assertThat(componentPart).isEqualTo("temurin-jdk-17.0.2");
        Assertions.assertThat(componentName).isEqualTo("Java Temurin");
        Assertions.assertThat(componentVersion).isEqualTo("17.0.2");
        Assertions.assertThat(componentRelease).isEqualTo("8");
    }

    @Test
    public void testVariant007() {
        File file = new File("src/test/resources/component-pattern-contributor/java-runtime-007/release");

        final List<ComponentPatternData> cpdList = contributor.contribute(file.getParentFile(), file.getParent(), "release", FileUtils.computeMD5Checksum(file));

        Assertions.assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);

        final String componentPart = cpd.get(COMPONENT_PART);
        final String componentName = cpd.get(COMPONENT_NAME);
        final String componentVersion = cpd.get(COMPONENT_VERSION);
        final String componentRelease = cpd.get("Release");

        Assertions.assertThat(componentPart).isEqualTo("red_hat-jdk-17.0.8");
        Assertions.assertThat(componentName).isEqualTo("Red Hat Java");
        Assertions.assertThat(componentVersion).isEqualTo("17.0.8");
        Assertions.assertThat(componentRelease).isEqualTo("0.7-1");
    }

}