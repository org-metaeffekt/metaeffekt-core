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
package org.metaeffekt.core.inventory.scan;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.configuration.DirectoryScanAggregatorConfiguration;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.report.DirectoryInventoryScan;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DirectoryInventoryScanTest {

    @Test
    public void testScanExtractedFiles() throws IOException {
        File inputDir = new File("src/test/resources/test-scan-inputs");
        File scanDir = new File("target/test-scan");
        String[] scanIncludes = new String[]{"**/*"};
        String[] scanExcludes = new String[]{"--none--"};

        File inventoryFile = new File("src/test/resources/test-inventory-01/artifact-inventory-01.xls");
        Inventory referenceInventory = new InventoryReader().readInventory(inventoryFile);

        final DirectoryInventoryScan scan = new DirectoryInventoryScan(inputDir, scanDir, scanIncludes, scanExcludes, referenceInventory);

        scan.setEnableImplicitUnpack(false);
        scan.setEnableDetectComponentPatterns(false);
        scan.setIncludeEmbedded(false);

        final Inventory resultInventory = scan.createScanInventory();

        new InventoryWriter().writeInventory(resultInventory, new File("target/test-scan/inventory-01.xls"));

        for (Artifact a : resultInventory.getArtifacts()) {
            System.out.println(a.getId() + " - " + a.getChecksum() + " - " + a.getProjects());
        }

        Assertions.assertThat(resultInventory.findAllWithId("file.txt").size()).isEqualTo(2);
        Assertions.assertThat(resultInventory.findArtifactByIdAndChecksum("file.txt", "6a38dfd8c715a9465f871d776267043e")).isNotNull();
        Assertions.assertThat(resultInventory.findArtifactByIdAndChecksum("file.txt", "8dc71de3cc97ca6d4cd8c9b876252823")).isNotNull();

        // these are covered by component patterns
        Assertions.assertThat(resultInventory.findArtifact("a.txt")).isNull();
        Assertions.assertThat(resultInventory.findArtifact("A Files")).isNotNull();
        Assertions.assertThat(resultInventory.findArtifact("b.txt")).isNull();
        Assertions.assertThat(resultInventory.findArtifact("B Files")).isNotNull();
        Assertions.assertThat(resultInventory.findArtifactByIdAndChecksum("file.txt", "6a38dfd8c715a9465f871d776267043e").getProjects()).hasSize(1);

        Assertions.assertThat(resultInventory.findArtifact("Please not")).isNull();

        Assertions.assertThat(resultInventory.findArtifact("test-alpha-1.0.0.jar")).isNotNull();

        // most primitive test on DirectoryScanAggregatorConfiguration
        DirectoryScanAggregatorConfiguration directoryScanAggregatorConfiguration =
                new DirectoryScanAggregatorConfiguration(referenceInventory, resultInventory, scanDir);

        final List<FilePatternQualifierMapper> filePatternQualifierMappers =
                directoryScanAggregatorConfiguration.mapArtifactsToCoveredFiles();

        Set<String> qualifiers = new HashSet<>();
        for (FilePatternQualifierMapper mapper : filePatternQualifierMappers) {
            qualifiers.add(mapper.getQualifier());
        }
        Assertions.assertThat(qualifiers.contains("test-alpha-1.0.0.jar")).isTrue();

    }

    @Test
    public void testJarMetaExtractorIntegration() {
        File inputDir = new File("src/test/resources/test-maven-jar-meta-extractor");
        File scanDir = new File("target/test-maven-jar-meta-extractor");
        String[] scanIncludes = new String[]{"**/*"};
        String[] scanExcludes = new String[]{"--none--"};

        Inventory inventory = new Inventory();
        final DirectoryInventoryScan scan = new DirectoryInventoryScan(inputDir, scanDir, scanIncludes, scanExcludes, inventory);
        scan.setEnableImplicitUnpack(false);

        final Inventory resultInventory = scan.createScanInventory();

        boolean found0000 = false;
        boolean found0001 = false;
        boolean found0002 = false;
        boolean found0003 = false;
        boolean found0004 = false;
        boolean found0005 = false;

        for (Artifact artifact : resultInventory.getArtifacts()) {
            artifact.deriveArtifactId();

            // check that 0000 was success
            if (!found0000) {
                found0000 = "dummy-artifact".equals(artifact.getArtifactId())
                        && "my.test.dummy.package".equals(artifact.getGroupId())
                        && "0.0.1".equals(artifact.getVersion());
            }

            if (!found0001) {
                found0001 = "dummy-artifact".equals(artifact.getArtifactId())
                        && "my.test.dummy.package".equals(artifact.getGroupId())
                        && "0.0.1".equals(artifact.getVersion());
            }

            if (!found0002) {
                found0002 = "dummy-artifact".equals(artifact.getArtifactId())
                        && "good.parent.groupid".equals(artifact.getGroupId())
                        && "0.0.2".equals(artifact.getVersion());
            }

            if (!found0003) {
                found0003 = artifact.getGroupId() == null
                        && artifact.getVersion() == null
                        && artifact.get("Errors") == null;
            }

            if (!found0004) {
                found0004 = artifact.getGroupId() == null
                        && artifact.getVersion() == null
                        && artifact.get("Errors") == null;
            }

            if (!found0005) {
                found0005 = "dummy-artifact-deep".equals(artifact.getArtifactId())
                        && "my.test.dummy.package".equals(artifact.getGroupId())
                        && "0.0.1".equals(artifact.getVersion());
            }
        }

        Assert.assertTrue(found0000);
        Assert.assertTrue(found0001);
        Assert.assertTrue(found0002);
        Assert.assertTrue(found0003);
        Assert.assertTrue(found0004);
        Assert.assertTrue(found0005);
    }

    @Ignore
    @Test
    public void testScanExtractedFiles_External() throws IOException {
        File inputDir = new File("<project.dir>/external-resources");
        File scanDir = new File("<project.dir>/target/scan");
        String[] scanIncludes = new String[]{"**/*"};
        String[] scanExcludes = new String[]{"--none--"};
        File inventoryFile = new File("<project.baseDir>/inventory/src/main/resources/inventory/artifact-inventory.xls");
        Inventory inventory = new InventoryReader().readInventory(inventoryFile);

        final DirectoryInventoryScan scan = new DirectoryInventoryScan(inputDir, scanDir, scanIncludes, scanExcludes, inventory);

        scan.setEnableImplicitUnpack(true);
        scan.setIncludeEmbedded(true);
        scan.setEnableDetectComponentPatterns(true);

        final Inventory resultInventory = scan.createScanInventory();

        for (Artifact a : resultInventory.getArtifacts()) {
            System.out.println(a.getId() + " - " + a.getVersion() + " - " + a.getProjects());
        }

        new InventoryWriter().writeInventory(resultInventory, new File("target/scan-inventory.xls"));
    }

    @Test
    public void testScan_LinuxDistro001() throws IOException {

        final File scanInputDir = new File("src/test/resources/component-pattern-contributor/linux-distro-001");
        final File scanDir = new File("target/scan/linux-distro-001");

        final File referenceInventoryDir = new File("src/test/resources/test-inventory-01");

        final Inventory inventory = scan(referenceInventoryDir, scanInputDir, scanDir);

        Assertions.assertThat(inventory.getAssetMetaData().size()).isEqualTo(1);

        new InventoryWriter().writeInventory(inventory, new File("target/linux-distro-001.xls"));

    }

    @Ignore
    @Test
    public void testScanExtractedFiles_ExternalNG() throws IOException {

        final File scanInputDir = new File("<path-to-input>");
        final File scanDir = new File("<path-to-scan>");

        final File referenceInventoryDir = new File("src/test/resources/test-inventory-01");

        final Inventory inventory = scan(referenceInventoryDir, scanInputDir, scanDir);
        new InventoryWriter().writeInventory(inventory, new File("target/scan-inventory.xlsx"));

        for (Artifact artifact : inventory.getArtifacts()) {
            final String pathInAsset = artifact.get(Artifact.Attribute.PATH_IN_ASSET);
            if (StringUtils.isBlank(pathInAsset)) {
                throw new IllegalStateException("Attribute [Path in Asset] must be available after scan.");
            }
        }

        final Inventory referenceInventory = InventoryUtils.readInventory(referenceInventoryDir, "*.xls");

        final DirectoryScanAggregatorConfiguration directoryScanAggregatorConfiguration =
                new DirectoryScanAggregatorConfiguration(referenceInventory, inventory, scanDir);

        final List<FilePatternQualifierMapper> filePatternQualifierMappers =
                directoryScanAggregatorConfiguration.mapArtifactsToCoveredFiles();
        // FIXME-AOE: add assertions

        directoryScanAggregatorConfiguration.aggregateFiles(new File("target/aggregation"));
        // FIXME-AOE: add assertions

        new InventoryWriter().writeInventory(inventory, new File("target/aggregated-inventory.xlsx"));

    }

    private static Inventory scan(File referenceInventoryDir, File scanInputDir, File scanDir) throws IOException {
        String[] scanIncludes = new String[] {
                "**/*"
        };
        String[] scanExcludes = new String[] {
                "**/.DS_Store", "**/._*" ,
                "**/.git/**/*", "**/.git*", "**/.git*"
        };

        String[] unwrapIncludes = new String[] {
                "**/*"
        };

        String[] unwrapExcludes = new String[] {
                // suffixed known to be non-strucutural
                "**/*.js.gz", "**/*.js.map.gz", "**/*.css.gz",
                "**/*.css.map.gz", "**/*.svg.gz", "**/*.json.gz",
                "**/*.ttf.gz", "**/*.eot.gz",
                "**/*.log.gz",

                // bad tars
                "**/invalid.tar",
                "**/testtar.tar",

                // suffix known to be non-archives;
                "**/*.c", "**/*.cpp",
                "**/*.js",
                "**/*.log",
        };

        // FIXME: at post-scan-filter (really; should the filter not apply after resolve/scan

        final Inventory referenceInventory = InventoryUtils.readInventory(referenceInventoryDir, "*.xls");

        final DirectoryInventoryScan scan = new DirectoryInventoryScan(
                scanInputDir, scanDir,
                scanIncludes, scanExcludes,
                unwrapIncludes, unwrapExcludes,
                referenceInventory);

        scan.setIncludeEmbedded(true);
        scan.setEnableImplicitUnpack(true);
        scan.setEnableDetectComponentPatterns(true);

        return scan.createScanInventory();
    }

    @Ignore
    @Test
    public void testScanContainersAndStuff() throws IOException {
        final File inputDir = new File("INPUT FILE GOES HERE");
        final File scanDir = new File("target/SCANDIR_HERE");
        final File outputInventory = new File("target/" + "TEST_INVENTORY_" + inputDir.getName() + ".xlsx");

        final String[] scanIncludes = new String[]{"**/*"};
        final String[] scanExcludes = new String[]{"--none--"};

        final Inventory inventory = new Inventory();
        final DirectoryInventoryScan scan =
                new DirectoryInventoryScan(inputDir, scanDir, scanIncludes, scanExcludes, inventory);
        scan.setEnableImplicitUnpack(true);
        scan.setEnableDetectComponentPatterns(true);

        final Inventory resultInventory = scan.createScanInventory();

        new InventoryWriter().writeInventory(resultInventory, outputInventory);
    }

}
