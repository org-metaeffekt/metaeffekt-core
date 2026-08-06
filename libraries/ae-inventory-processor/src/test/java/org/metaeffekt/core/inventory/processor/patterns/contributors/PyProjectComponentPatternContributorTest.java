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

import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;
import static org.metaeffekt.core.util.FileUtils.computeMD5Checksum;

public class PyProjectComponentPatternContributorTest {
    final PyProjectComponentPatternContributor pyProjectComponentPatternContributor = new PyProjectComponentPatternContributor();

    @Test
    public void testPoetry001() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/pyproject");
        final String relativeAnchorPath = "poetry001/pyproject.toml";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = pyProjectComponentPatternContributor.contribute(baseDir, relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(VERSION_ANCHOR)).isEqualTo("pyproject.toml");
        assertThat(cpd.get(INCLUDE_PATTERN)).isEqualTo("pyproject.toml, poetry.lock");
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("poetry-flask-0.1.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("poetry-flask");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.1.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/poetry-001-inventory.xlsx"));

        List<Artifact> artifacts = inventory.getArtifacts();
        assertThat(artifacts.size()).isEqualTo(14);

        final String projectAssetId = "AID-" + cpd.get(COMPONENT_PART);
        final Map<String, Long> groupedDependencyCounts = artifacts.stream().collect(Collectors.groupingBy(a -> a.get(projectAssetId), Collectors.counting()));

        assertThat(groupedDependencyCounts.get("r")).isEqualTo(2);
        assertThat(groupedDependencyCounts.get("d")).isEqualTo(1);
        assertThat(groupedDependencyCounts.get("(r)")).isEqualTo(8);
        assertThat(groupedDependencyCounts.get("(d)")).isEqualTo(3);

        assertThat(groupedDependencyCounts.get("r") + groupedDependencyCounts.get("(r)")).isEqualTo(10);
        assertThat(groupedDependencyCounts.get("d") + groupedDependencyCounts.get("(d)")).isEqualTo(4);
    }

    /**
     * With legacy lock version 1.1.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testPoetry002() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/pyproject");
        final String relativeAnchorPath = "poetry002/pyproject.toml";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = pyProjectComponentPatternContributor.contribute(baseDir, relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(VERSION_ANCHOR)).isEqualTo("pyproject.toml");
        assertThat(cpd.get(INCLUDE_PATTERN)).isEqualTo("pyproject.toml, poetry.lock");
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("example-project-0.1.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("example-project");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.1.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/poetry-002-inventory.xlsx"));

        List<Artifact> artifacts = inventory.getArtifacts();
        assertThat(artifacts.size()).isEqualTo(3);

        final String projectAssetId = "AID-" + cpd.get(COMPONENT_PART);
        final Map<String, Long> groupedDependencyCounts = artifacts.stream().collect(Collectors.groupingBy(a -> a.get(projectAssetId), Collectors.counting()));

        assertThat(groupedDependencyCounts.get("r")).isEqualTo(1);
        assertThat(groupedDependencyCounts.get("d")).isNull();
        assertThat(groupedDependencyCounts.get("(r)")).isEqualTo(2);
        assertThat(groupedDependencyCounts.get("(d)")).isNull();

        assertThat(groupedDependencyCounts.get("r") + groupedDependencyCounts.get("(r)")).isEqualTo(3);
    }

    @Test
    public void testPdm001() throws IOException {
        final File baseDir = new File("src/test/resources/component-pattern-contributor/pyproject");
        final String relativeAnchorPath = "pdm001/pyproject.toml";
        final File anchorFile = new File(baseDir, relativeAnchorPath);

        if (!anchorFile.exists()) {
            throw new IllegalStateException("File does not exist: " + anchorFile.getAbsolutePath());
        }

        final List<ComponentPatternData> cpdList = pyProjectComponentPatternContributor.contribute(baseDir, relativeAnchorPath, computeMD5Checksum(anchorFile));

        assertThat(cpdList.size()).isEqualTo(1);

        final ComponentPatternData cpd = cpdList.get(0);
        assertThat(cpd.get(VERSION_ANCHOR)).isEqualTo("pyproject.toml");
        assertThat(cpd.get(INCLUDE_PATTERN)).isEqualTo("pyproject.toml, pdm.lock");
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("pdm-dynamic:scm");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("pdm");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("dynamic:scm");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/pdm-001-inventory.xlsx"));

        List<Artifact> artifacts = inventory.getArtifacts();
        assertThat(artifacts.size()).isEqualTo(84);

        final String projectAssetId = "AID-" + cpd.get(COMPONENT_PART);
        final Map<String, Long> groupedDependencyCounts = artifacts.stream().collect(Collectors.groupingBy(a -> a.get(projectAssetId), Collectors.counting()));

        assertThat(groupedDependencyCounts.get("r")).isEqualTo(24);
        assertThat(groupedDependencyCounts.get("d")).isEqualTo(12);
        assertThat(groupedDependencyCounts.get("(r)")).isEqualTo(15);
        assertThat(groupedDependencyCounts.get("(d)")).isEqualTo(33);

        assertThat(groupedDependencyCounts.get("r") + groupedDependencyCounts.get("(r)")).isEqualTo(39);
        assertThat(groupedDependencyCounts.get("d") + groupedDependencyCounts.get("(d)")).isEqualTo(45);
    }
}
