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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.metaeffekt.core.itest.common.setup.TestConfig.getScanFolder;

public class ContainerDumpSetup {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerDumpSetup.class);


    public static String exportContainerFromRegistryByRepositoryAndTag(String registryUrl, String repository, String tag, String scanSubFolder) {

        if (tag == null || tag.isEmpty()) {
            tag = "latest";
        }

        // Parse the tag for docker
        if (tag.contains("@")) {
            tag = tag.substring(0, tag.indexOf("@"));
        }

        // Create a filename from the image name based on the last slash in the repository name
        repository = repository.replaceAll("test$", "");
        String filename = repository.substring(repository.lastIndexOf("/") + 1) + "-" + tag + ".tar";
        String myDir = scanSubFolder.replace("org.metaeffekt.core.itest.", "");
        String folder = myDir.replace(".", "/") + "/";
        String scanFolder = getScanFolder() + folder;
        if (new File(scanFolder).mkdirs()) {
            LOG.info("Created scan folder {}", getScanFolder() + scanSubFolder);
        }
        String outputFilePath = scanFolder + filename;
        File outputFile = new File(outputFilePath);

        // Check if the file already exists
        if (outputFile.exists()) {
            LOG.info("Container dump already exists at {}", outputFilePath);
            return outputFile.getAbsolutePath();
        }

        if (registryUrl == null || registryUrl.isEmpty()) {
            registryUrl = "docker.io";
        }

        String imageName = registryUrl + "/" + repository + ":" + tag;

        // Pull the image
        try {
            LOG.info("Downloading image {} from registry", imageName);
            executeCommand(new String[]{"docker", "pull", imageName});
            LOG.info("Downloaded image {} from registry", imageName);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to pull image from registry", e);
        }

        // Create a container from the image
        String containerId;
        try {
            containerId = executeCommand(new String[]{"docker", "create", imageName});
            LOG.info("Created container {} from image {}", containerId, imageName);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create container from image", e);
        }

        // Export the container's filesystem to a tar file
        try {
            executeCommand(new String[]{"docker", "export", containerId, "-o", outputFilePath});
            LOG.info("Exported container {} to file {}", containerId, outputFilePath);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to export container filesystem", e);
        }

        // Remove the created container
        try {
            executeCommand(new String[]{"docker", "rm", containerId});
            LOG.info("Removed container {}", containerId);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to remove container", e);
        }

        // Return the path to the exported container file
        return outputFile.getAbsolutePath();
    }

    private static String executeCommand(String[] commands) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();

        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                String part = new String(buffer, 0, bytesRead);
                output.append(part);
            }
        }

        int exitVal = process.waitFor(); // Wait for the process to complete.
        if (exitVal != 0) {
            // Handle the case where the process did not complete successfully.
            throw new IOException("Command execution failed with exit code " + exitVal);
        }

        return output.toString().trim(); // Return the accumulated output, which includes the container ID at the end.
    }
}
