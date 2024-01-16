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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Assembles scripts according to a simple format.<br>
 * Useful in the creation of simple-to-run shell scripts.
 */
public class ShellScriptAssembler {
    private final File inputFile;
    private final File outputFile;
    private final File libraryDirectory;

    protected final Pattern incompleteReplacementRequest = Pattern.compile("^# ?INCLUDESOURCEHERE");
    protected final Pattern replacementRequest = Pattern.compile("^# ?INCLUDESOURCEHERE-[a-zA-Z]+$");

    public ShellScriptAssembler(File inputFile, File outputFile, File libraryDirectory) {
        // check inputs
        if (inputFile == null || outputFile == null) {
            throw new IllegalArgumentException("Missing input or output file.");
        }

        if (libraryDirectory == null) {
            throw new IllegalArgumentException("Missing libraryDirectory.");
        }

        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input file doesn't exist.");
        }

        if (!Files.isRegularFile(inputFile.toPath())) {
            throw new IllegalArgumentException("Input file isn't a file.");
        }

        if (outputFile.exists() && !Files.isRegularFile(outputFile.toPath())) {
            throw new IllegalArgumentException("Output file isn't a file");
        }

        try {
            if (inputFile.getCanonicalPath().equals(outputFile.getCanonicalPath())) {
                throw new IllegalArgumentException("Detected input/output files on same path. Refusing to write.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error checking if input and output paths refer to the same actual path.", e);
        }

        if (!libraryDirectory.isDirectory()) {
            throw new IllegalArgumentException("libraryDirectory isn't a directory");
        }

        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.libraryDirectory = libraryDirectory;
    }

    /**
     * Replaces or returns the current line, depending on if it matches.<br>
     * Will automatically read library content from {@link #libraryDirectory}.
     *
     * @param line the line to inspect.
     * @param num the line number for more helpful errors.
     *
     * @return either the line or (commented) library file content.
     *
     * @throws IOException throws on error reading library files.
     * @throws MalformedIncludeException thrown when replacement cannot be performed as expected.
     */
    public String getReplacement(String line, long num) throws IOException, MalformedIncludeException {
        if (!incompleteReplacementRequest.matcher(line).find()) {
            return line;
        }

        if (!replacementRequest.matcher(line.trim()).matches()) {
            throw new MalformedIncludeException("Bad replacement request in " + inputFile.getPath() + ":" + num);
        }

        String libFilePrefix = line.trim().substring(line.trim().lastIndexOf("-") + 1);
        File libraryFile = new File(libraryDirectory, libFilePrefix + "-functions.sh");

        if (!Files.isRegularFile(libraryFile.toPath())) {
            throw new IOException("libraryFile '" + libraryFile + "' was not a file");
        }

        // read libraryFile, return processed string
        char[] readChars = new char[16384];
        StringBuilder inclusionContent = new StringBuilder();
        inclusionContent.append("# BEGIN INCLUSION (").append(libFilePrefix).append(")\n");
        try (InputStream inputStream = Files.newInputStream(libraryFile.toPath());
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int readNum;
            while (true) {
                readNum = reader.read(readChars);
                if (readNum == -1) {
                    break;
                }
                inclusionContent.append(readChars, 0, readNum);
            }
        }
        inclusionContent.append("# END INCLUSION (").append(libFilePrefix).append(")\n");
        return inclusionContent.toString();
    }

    public void assemble() throws IOException, MalformedIncludeException {
        try (OutputStream outputStream = Files.newOutputStream(outputFile.toPath(),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            List<String> lines = Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8);

            long lineNumber = 0;
            for (String line : lines) {
                lineNumber++;
                String processedLine = getReplacement(line, lineNumber);
                writer.write(processedLine + "\n");
            }
        }
    }
}
