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

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;

public class NpmComponentPatternContributorTest {

    final WebModuleComponentPatternContributor contributor = new WebModuleComponentPatternContributor();

    @Test
    public void testVariant001() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-001");
        final String relativeAnchorePath = "info/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = contributor.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("project-a-0.0.1-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("project-a");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(645);
    }

    @Test
    public void testVariant002() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-002");
        final String relativeAnchorePath = "web-app/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = contributor.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-app-0.0.1-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-app");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(190);
    }

    @Test
    public void testVariant003_onlyLockFile() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-003");
        final String relativeAnchorePath = "web-app/package-lock.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = contributor.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-app-0.0.1-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-app");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(190);
    }

    @Test
    public void testVariant004_onlyPackageFile() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-004");
        final String relativeAnchorePath = "web-app/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = contributor.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-app-0.0.1-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-app");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        // FIXME: ew should at least identify the stub dependencies from the json file?; versions not fixed.
        assertThat(cpd.getExpansionInventorySupplier()).isNull();
    }

}