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
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriterTest;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;

public class WebModuleComponentPatternContributorTest {

    final NpmWebModuleComponentPatternContributor npm = new NpmWebModuleComponentPatternContributor();

    final BowerWebModuleComponentPatternContributor bower = new BowerWebModuleComponentPatternContributor();

    final ComposerWebModuleComponentPatternContributor composer = new ComposerWebModuleComponentPatternContributor();

    @Test
    public void testNpm001() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-001");
        final String relativeAnchorPath = "info/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("project-a-0.0.1-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("project-a");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(1535);
        inventory.getArtifacts().forEach(artifact -> {
            if (artifact.getId().contains("rimraf")){
                assertThat(artifact.getVersion()).isIn("3.0.2", "5.0.7");
            }
        });

        new InventoryWriter().writeInventory(inventory, new File("target/npm-001-inventory.xlsx"));

    }

    @Test
    public void testNpm002() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-002");
        final String relativeAnchorePath = "web-app/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-app-1.0.0-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-app");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.0.0-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(71);
    }

    @Test
    public void testNpm003_onlyLockFile() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-003");
        final String relativeAnchorPath = "web-app/package-lock.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-app-0.0.1-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-app");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(1539);
    }

    @Test
    public void testNpm004_onlyPackageFile() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-004");
        final String relativeAnchorePath = "web-app/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
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

    @Test
    public void testNpm005() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-005");
        final String relativeAnchorePath = "dashboard/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("ae-npm-vulnerability-assessment-dashboard-0.1.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("ae-npm-vulnerability-assessment-dashboard");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.1.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();
        assertThat(inventory.getArtifacts().size()).isEqualTo(56);
    }

    @Test
    public void testNpm006() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-006");
        final String relativeAnchorePath = "example-angular/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("intern-angular-1.0.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("intern-angular");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.0.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();
        assertThat(inventory.getArtifacts().size()).isEqualTo(28);
    }

    @Test
    public void testBower001() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/bower-001");
        final String relativeAnchorePath = "bower.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = bower.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));


        // this is expected to fail since there is no version information in the bower file and in the .bower file
        assertThat(cpdList.size()).isEqualTo(0);
    }

    @Test
    public void testBower002_dotBowerFileOnly() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/bower-002");
        final String relativeAnchorePath = ".bower.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = bower.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));


        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-component-tester-6.5.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-component-tester");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("6.5.0");

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(12);
    }

    @Test
    public void testComposer001() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/composer-001");
        final String relativeAnchorePath = "composer.json";
        final File anchorFile = new File(baseDir, relativeAnchorePath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = composer.contribute(baseDir,
                relativeAnchorePath, FileUtils.computeMD5Checksum(anchorFile));


        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("composer-test-3.15.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("composer-test");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("3.15.0");

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(257);
    }
}