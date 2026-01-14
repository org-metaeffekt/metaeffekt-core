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

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemBinaryComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(SystemBinaryComponentPatternContributor.class);
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("/usr/bin/**");
        add("/usr/sbin/**");
    }});

    public static final String TYPE_VALUE_SYSTEM_BINARY = "system-binary";

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.contains("/usr/bin/") || pathInContext.contains("/usr/sbin/");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile().getParentFile();

        try {
            if (!isFileExecutable(anchorFile.getAbsolutePath())) {
                LOG.warn("File {} is not executable. Skipping.", anchorFile.getAbsolutePath());
                return Collections.emptyList();
            }
            executeCommand(new String[]{"chmod", "+x", anchorFile.getAbsolutePath()});
            String version = getVersion(anchorFile);
            // construct component pattern
            final ComponentPatternData componentPatternData = new ComponentPatternData();
            final String contextRelPath = FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile());
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, contextRelPath + "/" + anchorFile.getName());
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, anchorFile.getName());
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, version);
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, anchorFile.getName() + "-" + version);

            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");

            componentPatternData.set(Constants.KEY_TYPE, TYPE_VALUE_SYSTEM_BINARY);

            return Collections.singletonList(componentPatternData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 2;
    }

    public static String executeCommand(String[] commands) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            // FIXME: replace with ExecUtils / ExecParam
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.redirectErrorStream(true);
            StringBuilder output = new StringBuilder();
            Process process;
            try {
                process = processBuilder.start();
            } catch (IOException e) {
                LOG.warn("Cannot run program: {}", e.getMessage());
                return "N/A";
            }
            String versionString = null; // This will hold the first matched version string

            // pattern to match version numbers in the format x.x or x.x.x
            Pattern versionPattern = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?(\\.\\d+)?");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // only search for version string if not already found
                    Matcher matcher = versionPattern.matcher(line);
                    if (matcher.find()) {
                        versionString = matcher.group();
                        break;
                    }
                }
            }
            int exitVal = process.waitFor();
            if (exitVal != 0) {
                LOG.warn("Command execution failed with exit code {} and output: {}", exitVal, output);
                return "N/A";
            }
            if (versionString != null) {
                return versionString; // return the found version string
            } else {
                return "N/A"; // return a default version if no version string is found
            }
        });

        try {
            // wait for the command to complete within 500 milliseconds
            return future.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOG.warn("Command execution timed out: {}", e.getMessage());
            future.cancel(true);  // attempt to cancel the ongoing command
            return "N/A";
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Failed to execute command: {}", e.getMessage());
            return "N/A";
        } finally {
            executor.shutdownNow();  // ensure the executor is properly shut down
        }
    }

    private static boolean isFileExecutable(String filePath) throws IOException, InterruptedException {
        // check if the file is executable and get detailed info
        String fileInfo = getFileDetails(filePath);
        if (!fileInfo.contains("executable") || fileInfo.contains("ASCII text") || fileInfo.contains("script")) {
            LOG.info("File {} is not an executable. Skipping.", filePath);
            return false;
        }

        // check for compatibility with the current architecture
        if (!isArchitectureCompatible(fileInfo)) {
            LOG.info("File {} is not compatible with the current architecture. Skipping.", filePath);
            return false;
        }

        // extract and verify the interpreter (dynamic linker)
        String interpreter = extractInterpreter(fileInfo);
        if (interpreter != null && !new File(interpreter).exists()) {
            LOG.info("Interpreter not found: {}.", interpreter);
            return false;
        }
        return true;
    }

    private static String getVersion(File anchorFile) {
        // array of command arguments to try in sequence
        String[][] commands = {
                {anchorFile.getAbsolutePath(), "--version"},
                {anchorFile.getAbsolutePath(), "-V"},
                {anchorFile.getAbsolutePath(), "-W", "version"}
        };

        String version = "N/A";  // TODO: default value if no valid version is found
        for (String[] command : commands) {
            version = executeCommand(command);
            if (version != null && !version.equals("N/A")) {
                break;  // break out of the loop if a valid version is found
            }
        }
        return version;
    }

    private static String getFileDetails(String filePath) throws IOException, InterruptedException {
        // FIXME: replace with ExecUtils / ExecParam
        ProcessBuilder processBuilder = new ProcessBuilder("file", filePath);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitVal = process.waitFor();
        if (exitVal != 0) {
            LOG.error("File command failed for: {}", filePath);
            return "";
        }
        return output.toString();
    }

    private static boolean isArchitectureCompatible(String fileInfo) {
        // TODO: adjust regex to match different architectures as necessary
        Pattern archPattern = Pattern.compile("x86-64|amd64|x86_64|arm64|aarch64|ppc64|ppc64le|s390x");
        Matcher matcher = archPattern.matcher(fileInfo);
        return matcher.find();
    }

    private static String extractInterpreter(String fileInfo) {
        Pattern pattern = Pattern.compile("interpreter (.*?),");
        Matcher matcher = pattern.matcher(fileInfo);
        if (matcher.find()) {
            return matcher.group(1);  // return the interpreter path
        }
        return null;
    }
}
