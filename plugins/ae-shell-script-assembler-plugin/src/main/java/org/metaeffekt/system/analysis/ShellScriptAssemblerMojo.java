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
package org.metaeffekt.system.analysis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

/**
 * Assembles scripts according to a custom little format.<br>
 * Made to create more easily shippable shell scripts by putting all function libraries into one file with the code.
 */
@Mojo(name = "assemble-shell-script")
public class ShellScriptAssemblerMojo extends AbstractMojo {
    /**
     * Contains source files.<br>
     * This is the source directory.<br>
     * All files ending in ".sh" will be processed and output. Other files will be copied to the output directory.
     */
    @Parameter(name = "inputDirectory", required = true)
    private File inputDirectory;

    /**
     * Where to write processing results and other files.
     */
    @Parameter(name= "outputDirectory", required = true)
    private File outputDirectory;

    /**
     * Where to look for library files. Function libraries should be directly under this directory.
     */
    @Parameter(name="libraryDirectory", required = true)
    private File libraryDirectory;

    /**
     * Distinguishes files (shell script or not) and processes them accordingly.
     * @param filesToProcess Map (inputPath to outputPath) of files to process.
     * @return returns a summary of files processed.
     * @throws IOException if any of the files can't be processed.
     * @throws MalformedIncludeException if there was a malformed inclusion comment in a script.
     */
    protected Map<String, List<Path>> processFiles(Map<Path, Path> filesToProcess) throws IOException, MalformedIncludeException {
        Map<String, List<Path>> actionToProcessedPath = new HashMap<>();

        for (Map.Entry<Path, Path> entry : filesToProcess.entrySet()) {
            Path inputPath = entry.getKey();
            Path outputPath = entry.getValue();
            File inputFile = inputPath.toFile();
            File outputFile = outputPath.toFile();

            if (outputFile.exists()) {
                throw new IOException("Refusing to overwrite existing file '" + outputFile.getAbsolutePath() + "'");
            }
            if (Files.isDirectory(inputPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("A directory should not have made it to filesToProcess.");
            }

            if (Files.isRegularFile(inputPath, LinkOption.NOFOLLOW_LINKS)) {

                // simple test: does file name end in ".sh"?
                if (inputPath.getFileName().toString().endsWith(".sh")) {
                    // identified as a shell script. attempt to process it
                    ShellScriptAssembler assembler =
                            new ShellScriptAssembler(inputFile, outputFile, libraryDirectory);
                    assembler.assemble();
                    actionToProcessedPath.computeIfAbsent("assemble", (k) -> new ArrayList<>()).add(inputPath);
                } else {
                    // non-script files will be copied over
                    Files.copy(inputPath, outputPath);
                    actionToProcessedPath.computeIfAbsent("copy", (k) -> new ArrayList<>()).add(inputPath);
                }
            } else {
                throw new IOException("Entry '" + inputFile + "' is neither file nor directory.");
            }
        }

        return actionToProcessedPath;
    }

    private Path getRelativized(Path path, Path basePath) throws IOException {
        return basePath.relativize(path.toRealPath(LinkOption.NOFOLLOW_LINKS));
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (!inputDirectory.exists()) {
            throw new MojoExecutionException("Mojo's inputDirectory doesn't exist.");
        }
        if (!libraryDirectory.exists()) {
            throw new MojoExecutionException("Mojo's libraryDirectory doesn't exist.");
        }

        try {
            if (outputDirectory.exists()) {
                Path existingOutputDir = outputDirectory.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .appendInstant(6)
                        .toFormatter(Locale.ENGLISH);
                String newName = outputDirectory.getName() + "-OLD-" + formatter.format(Instant.now())
                        .replaceAll("[^a-zA-Z0-9_.:\\-]", "_");
                Path newPath = existingOutputDir.getParent().resolve(newName);
                getLog().info("Moving old output directory to [" + newPath + "]");

                Files.move(existingOutputDir, newPath);
            }

            Path inputDirPath = inputDirectory.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path outputDirPath = outputDirectory.toPath().toAbsolutePath();

            // iterate dir structure and queue files for processing
            Files.createDirectories(outputDirPath);

            Map<Path, Path> filesToProcess = new TreeMap<>();
            Files.walkFileTree(inputDirectory.toPath(), EnumSet.noneOf(FileVisitOption.class), 64, new SimpleFileVisitor<Path>() {
                private Path getRelativized(Path path) throws IOException {
                    return ShellScriptAssemblerMojo.this.getRelativized(path, inputDirPath);
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path outputPath = outputDirPath.resolve(getRelativized(dir));
                    Files.createDirectories(outputPath);
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path outputPath = outputDirPath.resolve(getRelativized(file));
                    filesToProcess.put(file, outputPath);
                    return FileVisitResult.CONTINUE;
                }
            });

            // process files
            Map<String, List<Path>> processedSummary = processFiles(filesToProcess);

            // print summary
            getLog().info("ShellScriptAssembler Summary:");
            for (String action : processedSummary.keySet()) {
                getLog().info("Performed [" + action + "] for:");
                for (Path path : processedSummary.get(action)) {
                    getLog().info("  - " + getRelativized(path, inputDirPath));
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to assemble script.", e);
        }
    }
}
