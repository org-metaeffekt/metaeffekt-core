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

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemBinariesComponentPatternContributor extends ComponentPatternContributor {

    private static final Logger LOG = LoggerFactory.getLogger(SystemBinariesComponentPatternContributor.class);
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("/usr/bin/**");
    }});

    public static final String TYPE_VALUE_SYSTEM_BINARY = "system-binary";

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.contains("/usr/bin/");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile().getParentFile();

        try {
            if (!isFileExecutable(anchorFile.getAbsolutePath())) {
                LOG.warn("File {} is not executable. Skipping.", anchorFile.getAbsolutePath());
                return Collections.emptyList();
            }
            executeCommand(new String[]{"chmod", "+x", anchorFile.getAbsolutePath()});
            String version = executeCommand(new String[]{anchorFile.getAbsolutePath(), "--version"});
            if (version.equals("N/A")) {
                version = executeCommand(new String[]{anchorFile.getAbsolutePath(), "-V"});
                if (version.equals("N/A")) {
                    version = executeCommand(new String[]{anchorFile.getAbsolutePath(), "-W", "version"});
                }
            }
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

    private static String executeCommand(String[] commands) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        String versionString = null; // This will hold the first matched version string

        // Pattern to match version numbers in the format x.x or x.x.x
        Pattern versionPattern = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?(\\.\\d+)?");

        try (InputStream is = process.getInputStream();
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader reader = new BufferedReader(isr)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Only search for version string if not already found
                    Matcher matcher = versionPattern.matcher(line);
                    if (matcher.find()) {
                        versionString = matcher.group();
                        break;
                    }
                }
            }

        int exitVal = process.waitFor(); // Wait for the process to complete.
        if (exitVal != 0) {
            // Handle the case where the process did not complete successfully.
            LOG.error("Command execution for command {} failed with exit code {} and output: {}", commands, exitVal, output);
        }

        if (versionString != null) {
            return versionString; // Return the found version string
        } else {
            return "N/A"; // Return a default message if no version string is found
        }
    }

    private static boolean isFileExecutable(String filePath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("file", filePath);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        boolean isExecutable = false;

        try (InputStream is = process.getInputStream();
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (line.contains("executable") && !line.contains("ASCII text") && !line.contains("script")) {
                    isExecutable = true;
                    break; // Exit the loop early if the file is found to be an executable
                }
            }
        }

        int exitVal = process.waitFor(); // Wait for the process to complete.
        if (exitVal != 0) {
            // Handle the case where the process did not complete successfully.
            LOG.error("isFileExecutable command execution failed with exit code {} and output: {}", exitVal, output);
        }

        return isExecutable;
    }
}
