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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.maven.inventory.extractor.ExcludePatternsConfig;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractorUtil;
import org.metaeffekt.core.maven.inventory.extractor.PatternSetMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Class for testing ContainerInventoryExtractionMojoTest.
 */
public class ContainerInventoryExtractionMojoTest {

    /**
     * Tests whether the files to be excluded having the specified patterns in the yaml config are not in the inventory.
     * To run the test, edit the exclude-config (src/test/resources/ContainerInventoryExtractionMojoTest/analysis-001/config/file-exclude-patterns-config.yaml) and specify file patterns to exclude files matching them.
     * Also provide an "analysis" folder under src/test/resources/ContainerInventoryExtractionMojoTest/analysis-001 containing the extracted data.
     *
     * @throws MojoExecutionException if an exception during the execution of the mojo occurs.
     * @throws MojoFailureException   if a failure during the execution of the mojo occurs.
     * @throws IOException            if an I/O exception occurs.
     */
    @Disabled
    @Test
    public void testExecute_excluded_files() throws MojoExecutionException, MojoFailureException, IOException {
        final ContainerInventoryExtractionMojo mojo = new ContainerInventoryExtractionMojo();

        final File baseDir = new File("src/test/resources/ContainerInventoryExtractionMojoTest/example-001");
        final File yamlExcludeConfigFile = new File(baseDir + "/config", "file-exclude-patterns-config.yaml");
        final File analysisDir = new File(baseDir, "01_analysis");
        final File resultInventoryDir = new File(baseDir, "02_inventory");
        final File targetInventoryFile = new File(baseDir + "/02_inventory", "result-inventory.xlsx");

        mojo.inputArchiveFile = new File(baseDir + "/00_input", "extracted_files.tar.gz");
        mojo.analysisDir = analysisDir;
        mojo.targetInventoryFile = targetInventoryFile;
        mojo.excludePatternsFile = yamlExcludeConfigFile;

        mojo.execute();

        Assertions.assertTrue(analysisDir.isDirectory());
        Assertions.assertTrue(unpackedSuccessfully(analysisDir));

        Assertions.assertTrue(resultInventoryDir.isDirectory());
        Assertions.assertTrue(new File(resultInventoryDir, "filtered-files.txt").isFile());
        Assertions.assertTrue(targetInventoryFile.isFile());

        final Inventory inventory = new InventoryReader().readInventory(targetInventoryFile);

        final List<Artifact> fileArtifacts = inventory.getArtifacts().stream().filter(artifact -> artifact.getType() == null || artifact.getType().equals(Constants.ARTIFACT_TYPE_PACKAGE)).toList();
        final ExcludePatternsConfig excludePatternsConfig = loadExcludePatternsConfig(yamlExcludeConfigFile);
        final PatternSetMatcher excludePatternSetMatcher = new PatternSetMatcher(excludePatternsConfig.getExcludes());
        for (Artifact fileArtifact : fileArtifacts) {
            // no file to be excluded was added to artifacts
            fileArtifact.getRootPaths().forEach(rootPath -> Assertions.assertFalse(excludePatternSetMatcher.matches(rootPath)));
        }
    }

    private ExcludePatternsConfig loadExcludePatternsConfig(File yamlExcludeConfigFile) throws IOException {
        return InventoryExtractorUtil.loadExcludeConfigFromYamlFile(yamlExcludeConfigFile);
    }

    private boolean unpackedSuccessfully(File analysisDir) throws IOException {
        try (Stream<Path> paths = Files.walk(analysisDir.toPath())) {
            return paths.anyMatch(this::isLinuxExtractedDirectory);
        }
    }

    private boolean isLinuxExtractedDirectory(Path dir) {
        return Files.isDirectory(dir) && Files.exists(dir.resolve("issue.txt")) && Files.exists(dir.resolve("release.txt"));
    }

}
