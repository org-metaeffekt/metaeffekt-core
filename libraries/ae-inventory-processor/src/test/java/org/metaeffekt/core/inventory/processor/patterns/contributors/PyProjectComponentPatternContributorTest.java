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
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("poetry-flask-0.1.0");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("poetry-flask");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("0.1.0");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/poetry-001-inventory.xlsx"));

        List<Artifact> artifacts = inventory.getArtifacts();
        assertThat(artifacts.size()).isEqualTo(14);

        // filter runtime artifacts
        final String projectAssetId = "AID-" + cpd.get(COMPONENT_PART);
        final Map<String, Long> groupedDependencyCounts = artifacts.stream().collect(Collectors.groupingBy(a -> a.get(projectAssetId), Collectors.counting()));

        assertThat(groupedDependencyCounts.get("r")).isEqualTo(2);
        assertThat(groupedDependencyCounts.get("d")).isEqualTo(1);
        assertThat(groupedDependencyCounts.get("(r)")).isEqualTo(7);
        assertThat(groupedDependencyCounts.get("(d)")).isEqualTo(4);

        assertThat(groupedDependencyCounts.get("r") + groupedDependencyCounts.get("(r)")).isEqualTo(9);
        assertThat(groupedDependencyCounts.get("d") + groupedDependencyCounts.get("(d)")).isEqualTo(5);
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
        assertThat(cpd.get(COMPONENT_PART)).isEqualTo("pdm-dynamic:scm");
        assertThat(cpd.get(COMPONENT_NAME)).isEqualTo("pdm");
        assertThat(cpd.get(COMPONENT_VERSION)).isEqualTo("dynamic:scm");
        assertThat(cpd.get("Release")).isNull();

        final Inventory inventory = cpd.getExpansionInventorySupplier().get();

        new InventoryWriter().writeInventory(inventory, new File("target/pdm-001-inventory.xlsx"));

        List<Artifact> artifacts = inventory.getArtifacts();
        assertThat(artifacts.size()).isEqualTo(64);

        // filter runtime artifacts
        final String projectAssetId = "AID-" + cpd.get(COMPONENT_PART);
        final Map<String, Long> groupedDependencyCounts = artifacts.stream().collect(Collectors.groupingBy(a -> a.get(projectAssetId), Collectors.counting()));

        assertThat(groupedDependencyCounts.get("r")).isEqualTo(24);
        assertThat(groupedDependencyCounts.get("d")).isEqualTo(12);
        assertThat(groupedDependencyCounts.get("(r)")).isEqualTo(11);
        assertThat(groupedDependencyCounts.get("(d)")).isEqualTo(17);

        assertThat(groupedDependencyCounts.get("r") + groupedDependencyCounts.get("(r)")).isEqualTo(35);
        assertThat(groupedDependencyCounts.get("d") + groupedDependencyCounts.get("(d)")).isEqualTo(29);
    }
}
