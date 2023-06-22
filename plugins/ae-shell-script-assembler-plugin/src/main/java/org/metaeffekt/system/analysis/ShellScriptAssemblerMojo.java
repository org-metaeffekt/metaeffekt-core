/*
 * Copyright 2022 the original author or authors.
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
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

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

    @Override
    public void execute() throws MojoExecutionException {
        if (!inputDirectory.exists()) {
            throw new MojoExecutionException("Mojo's inputDirectory doesn't exist.");
        }
        if (!libraryDirectory.exists()) {
            throw new MojoExecutionException("Mojo's libraryDirectory doesn't exist.");
        }

        try {
            Path inputDirPath = inputDirectory.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path outputDirPath;
            if (outputDirectory.exists()) {
                outputDirPath = outputDirectory.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
            } else {
                outputDirPath = outputDirectory.toPath().toAbsolutePath();
            }

            // iterate dir structure and process files individually with ShellScriptAssembler
            Files.createDirectories(outputDirPath);

            Map<Path, Path> filesToProcess = new TreeMap<>();
            Files.walkFileTree(inputDirectory.toPath(), EnumSet.noneOf(FileVisitOption.class), 64, new SimpleFileVisitor<Path>() {
                private Path getRelativized(Path path) throws IOException {
                    return inputDirPath.relativize(path.toRealPath(LinkOption.NOFOLLOW_LINKS));
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

            for (Map.Entry<Path, Path> entry : filesToProcess.entrySet()) {
                Path inputPath = entry.getKey();
                Path outputPath = entry.getValue();
                File inputFile = inputPath.toFile();
                File outputFile = outputPath.toFile();

                if (outputFile.exists()) {
                    throw new IOException("Refusing to overwrite output file '" + outputFile.getAbsolutePath() + "'");
                }

                if (Files.isDirectory(inputPath, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("A directory should not have made it to filesToProcess.");
                }

                if (Files.isRegularFile(inputPath, LinkOption.NOFOLLOW_LINKS)) {
                    if (inputPath.getFileName().toString().endsWith(".sh")) {
                        // identified as a shell script. attempt to process it
                        ShellScriptAssembler assembler =
                                new ShellScriptAssembler(inputFile, outputFile, libraryDirectory);
                        assembler.assemble();
                    } else {
                        // copy this file over as it is
                        // TODO: what to do with files other than shell scripts? copy or ignore?
                        Files.copy(inputPath, outputPath);
                    }
                } else {
                    throw new IOException("Entry '" + inputFile + "' is neither file nor directory.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Failed to assemble script.", e);
        }
    }
}
