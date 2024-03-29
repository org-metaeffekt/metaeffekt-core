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

package org.metaeffekt.core.itest.container;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ContainerDumpSetup {

    private static final String BASE_DUMP_PATH = "target/.test/inventory/dumps/";

    // obviously, this setup only works if you have docker-cli installed and running
    public static String exportContainer(String imageName) throws IOException, InterruptedException {
        // Ensure the dump directory exists
        new File(BASE_DUMP_PATH).mkdirs();

        // Create a filename from the image name
        String filename = imageName.replace("/", "_") + ".tar";
        String outputFilePath = BASE_DUMP_PATH + filename;
        File outputFile = new File(outputFilePath);

        // Pull the image
        executeCommand(new String[]{"docker", "pull", imageName});

        // Create a container from the image
        String containerId = executeCommand(new String[]{"docker", "create", imageName});

        // Export the container's filesystem to a tar file
        executeCommand(new String[]{"docker", "export", containerId, "-o", outputFilePath});

        // Remove the created container
        executeCommand(new String[]{"docker", "rm", containerId});

        // Return the path to the exported container file
        return outputFile.getAbsolutePath();
    }

    private static String executeCommand(String[] commands) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true); // Redirect error stream to the standard output stream
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        }

        int exitVal = process.waitFor(); // Wait for the process to complete
        if (exitVal == 0) {
            // Process completed successfully
            return output.toString().trim();
        } else {
            // Handle the case where the process did not complete successfully
            throw new IOException("Command execution failed with exit code " + exitVal + " and output: " + output);
        }
    }
}
