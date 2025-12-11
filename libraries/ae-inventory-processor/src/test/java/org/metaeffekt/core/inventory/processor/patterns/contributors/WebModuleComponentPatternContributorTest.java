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
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.adapter.npm.PackageLockParser1;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;
import static org.metaeffekt.core.util.FileUtils.computeMD5Checksum;

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
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("project-a-0.0.1-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("project-a");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-001-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(1590);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-project-a-0.0.1-SNAPSHOT"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(92);

        inventory.getArtifacts().forEach(artifact -> {
            if (artifact.getId().contains("rimraf")){
                assertThat(artifact.getVersion()).isIn("3.0.2", "5.0.7");
            }
        });

    }

    @Test
    public void testNpm001_Lock() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-001-lock");
        final String relativeAnchorPath = "info/package-lock.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        // currently the process cannot deal with a workspace lock file
        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-001-lock-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(1400);

        // FIX outline:
        // - enable detection of case "" --> workspace
        // - create multiple WebModules, one for each project in the workspace
        // - parse all projects (since we have no present selection)
    }

    @Test
    public void testNpm002() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-002");
        final String relativeAnchorPath = "web-app/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir, relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-app-1.0.0-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-app");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.0.0-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-002-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(1316);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-web-app-1.0.0-SNAPSHOT"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(84);
    }

    @Test
    public void testNpm002_lock() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-002-lock");
        final String relativeAnchorPath = "web-app/package-lock.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-app-1.0.0-SNAPSHOT");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-app");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.0.0-SNAPSHOT");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-002-lock-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(1316);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-web-app-1.0.0-SNAPSHOT"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(84);
    }

    @Test
    public void testNpm003() throws IOException {
        PackageLockParser1.CHECK_INVARIANTS = true;

        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-003");
        final String relativeAnchorPath = "sample/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("sample-1.1.14");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("sample");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.1.14");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-003-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(1306);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-sample-1.1.14"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(348);
    }

    @Test
    public void testNpm003_Lock() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-003-lock");
        final String relativeAnchorPath = "sample/package-lock.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("sample-1.1.14");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("sample");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.1.14");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-003-lock-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(1306);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-sample-1.1.14"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(348);
    }


    @Test
    public void testNpm004_onlyPackageFile() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-004");
        final String relativeAnchorPath = "web-app/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

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
    public void testNpm005() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-005");
        final String relativeAnchorPath = "dashboard/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("ae-npm-vulnerability-assessment-dashboard-0.1.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("ae-npm-vulnerability-assessment-dashboard");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.1.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-005-inventory.xlsx"));

        // assert specific content

        final String rootAssetId = "AID-ae-npm-vulnerability-assessment-dashboard-0.1.0";

        // the inventory must have two versions 3.0.3 and 2.0.11 of unist
        final Artifact unist3 = inventory.findArtifact("@types/unist-3.0.3");
        Assertions.assertThat(unist3).isNotNull();
        Assertions.assertThat(unist3.get(Artifact.Attribute.VERSION)).isEqualTo("3.0.3");
        Assertions.assertThat(unist3.get(Artifact.Attribute.TYPE)).isEqualTo("web-module");
        Assertions.assertThat(unist3.get(Artifact.Attribute.COMPONENT_SOURCE_TYPE)).isEqualTo("npm-module");
        Assertions.assertThat(unist3.get(Artifact.Attribute.PURL)).isEqualTo("pkg:npm/@types/unist@3.0.3");
        Assertions.assertThat(unist3.get(Artifact.Attribute.COMPONENT)).isEqualTo("@types/unist");
        Assertions.assertThat(unist3.get(Artifact.Attribute.PATH_IN_ASSET)).isEqualTo("dashboard/package.json[@types/unist@3.0.3]");

        final Artifact unist2 = inventory.findArtifact("@types/unist-2.0.11");
        Assertions.assertThat(unist2).isNotNull();
        Assertions.assertThat(unist2.get(Artifact.Attribute.COMPONENT)).isEqualTo("@types/unist");
        Assertions.assertThat(unist2.get(Artifact.Attribute.VERSION)).isEqualTo("2.0.11");
        Assertions.assertThat(unist2.get(Artifact.Attribute.TYPE)).isEqualTo("web-module");
        Assertions.assertThat(unist2.get(Artifact.Attribute.COMPONENT_SOURCE_TYPE)).isEqualTo("npm-module");
        Assertions.assertThat(unist2.get(Artifact.Attribute.PATH_IN_ASSET)).isEqualTo("dashboard/package.json[@types/unist@2.0.11]");
        Assertions.assertThat(unist2.get(Artifact.Attribute.PURL)).isEqualTo("pkg:npm/@types/unist@2.0.11");

        // both are transitive via react-markdown as direct dependency
        Assertions.assertThat(unist2.get(rootAssetId)).isEqualTo("(r)");
        Assertions.assertThat(unist3.get(rootAssetId)).isEqualTo("(r)");

        assertThat(inventory.getArtifacts().size()).isEqualTo(1542);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-ae-npm-vulnerability-assessment-dashboard-0.1.0"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(224);
    }

    @Test
    public void testNpm005_Lock() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-005-lock");
        final String relativeAnchorPath = "dashboard/package-lock.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("ae-npm-vulnerability-assessment-dashboard-0.1.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("ae-npm-vulnerability-assessment-dashboard");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.1.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-005-lock-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(1542);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-ae-npm-vulnerability-assessment-dashboard-0.1.0"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(224);
    }

    @Test
    public void testNpm006() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-006");
        final String relativeAnchorPath = "example-angular/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("intern-angular-1.0.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("intern-angular");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.0.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-006-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(520);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-intern-angular-1.0.0"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(16);
    }

    @Test
    public void testNpm006_Lock() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/npm-006-lock");
        final String relativeAnchorPath = "example-angular/package-lock.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("intern-angular-1.0.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("intern-angular");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.0.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/npm-006-lock-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(520);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-intern-angular-1.0.0"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(16);
    }

    @Test
    public void testYarn001() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/yarn-001");
        final String relativeAnchorPath = "test/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("example-yarn-package-1.0.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("example-yarn-package");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("1.0.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/yarn-001-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(343);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-example-yarn-package-1.0.0"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(14);
    }

    @Test
    public void testYarn002() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/yarn-002");
        final String relativeAnchorPath = "test/package.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = npm.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("test/package.json-v4.4.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("test/package.json");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("v4.4.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/yarn-002-inventory.xlsx"));

        final Artifact artifact001 = inventory.findArtifact("@cesium/engine-15.0.0");
        Assertions.assertThat(artifact001).isNotNull();
        Assertions.assertThat(artifact001.get(Artifact.Attribute.COMPONENT)).isEqualTo("@cesium/engine");
        Assertions.assertThat(artifact001.get(Artifact.Attribute.VERSION)).isEqualTo("15.0.0");
        Assertions.assertThat(artifact001.get(Artifact.Attribute.TYPE)).isEqualTo("web-module");
        Assertions.assertThat(artifact001.get(Artifact.Attribute.COMPONENT_SOURCE_TYPE)).isEqualTo("npm-module");
        Assertions.assertThat(artifact001.get(Artifact.Attribute.PATH_IN_ASSET)).isEqualTo("test/package.json[@cesium/engine@15.0.0]");
        Assertions.assertThat(artifact001.get(Artifact.Attribute.PURL)).isEqualTo("pkg:npm/@cesium/engine@15.0.0");
        Assertions.assertThat(artifact001.get("AID-test/package.json-v4.4.0")).isEqualTo("r");

        final Artifact artifact002 = inventory.findArtifact("@babel/core-7.26.10");
        Assertions.assertThat(artifact002).isNotNull();
        Assertions.assertThat(artifact002.get(Artifact.Attribute.COMPONENT)).isEqualTo("@babel/core");
        Assertions.assertThat(artifact002.get(Artifact.Attribute.VERSION)).isEqualTo("7.26.10");
        Assertions.assertThat(artifact002.get(Artifact.Attribute.TYPE)).isEqualTo("web-module");
        Assertions.assertThat(artifact002.get(Artifact.Attribute.COMPONENT_SOURCE_TYPE)).isEqualTo("npm-module");
        Assertions.assertThat(artifact002.get(Artifact.Attribute.PATH_IN_ASSET)).isEqualTo("test/package.json[@babel/core@7.26.10]");
        Assertions.assertThat(artifact002.get(Artifact.Attribute.PURL)).isEqualTo("pkg:npm/@babel/core@7.26.10");
        Assertions.assertThat(artifact002.get("AID-test/package.json-v4.4.0")).isEqualTo("d");

        assertThat(inventory.getArtifacts().size()).isEqualTo(1362);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-test/package.json-v4.4.0"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(235);

    }

    @Test
    public void testBower001() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/bower-001");
        final String relativeAnchorPath = "bower.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = bower.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        // this is expected to fail since there is no version information in the bower file and in the .bower file
        assertThat(cpdList.size()).isEqualTo(0);
    }

    @Test
    public void testBower002_dotBowerFileOnly() {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/bower-002");
        final String relativeAnchorPath = ".bower.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = bower.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));


        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("web-component-tester-6.5.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("web-component-tester");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("6.5.0");

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        assertThat(inventory.getArtifacts().size()).isEqualTo(12);
    }

    @Test
    public void testComposer001() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/composer-001");
        final String relativeAnchorPath = "composer.json";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = composer.contribute(baseDir,
                relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("composer-test-3.15.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("composer-test");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("3.15.0");

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/composer-001-inventory.xlsx"));

        assertThat(inventory.getArtifacts().size()).isEqualTo(328);

        // filter runtime artifacts
        InventoryUtils.filterInventoryForRuntimeArtifacts(inventory, Collections.singleton("AID-composer-test-3.15.0"));
        assertThat(inventory.getArtifacts().size()).isEqualTo(214);

    }

}