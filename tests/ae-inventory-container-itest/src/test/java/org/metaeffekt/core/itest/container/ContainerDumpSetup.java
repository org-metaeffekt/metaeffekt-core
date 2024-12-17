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

import org.metaeffekt.core.util.ExecUtils;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.metaeffekt.core.itest.common.setup.TestConfig.getDownloadFolder;
import static org.metaeffekt.core.itest.common.setup.TestConfig.getScanFolder;

// FIXME; integrate with TestSetup check hash
public class ContainerDumpSetup {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerDumpSetup.class);

    public static File saveContainerFromRegistryByRepositoryAndTag(String registryUrl, String repository, String tag, String scanSubFolder) {
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
        String downloadFolder = getDownloadFolder() + folder;
        if (new File(downloadFolder).mkdirs()) {
            LOG.info("Created download folder {}", downloadFolder);
        }

        String outputFilePath = downloadFolder + filename;
        File outputFile = new File(outputFilePath);
        File outputInspectFile = new File(outputFilePath.replace(".tar", ".json"));

        if (registryUrl == null || registryUrl.isEmpty()) {
            registryUrl = "docker.io";
        }

        String imageName = registryUrl + "/" + repository + ":" + tag;

        // pull and save image if required
        if (!outputFile.exists()) {
            try {
                LOG.info("Downloading image {} from registry", imageName);
                executeCommand(new String[]{"docker", "pull", imageName});
                LOG.info("Downloaded image {} from registry", imageName);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to pull image from registry", e);
            }

            try {
                executeCommand(new String[]{"docker", "save", imageName, "-o", outputFilePath});
                LOG.info("Saved container {} to file {}", imageName, outputFilePath);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to save container filesystem", e);
            }
        }

        // run inspect if required
        if (!outputInspectFile.exists()) {
            try {
                String content = executeCommand(new String[]{"docker", "inspect", imageName});
                FileUtils.write(outputInspectFile, content, FileUtils.ENCODING_UTF_8);
                LOG.info("Inspected container image {}", imageName);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to inspect container", e);
            }
        }

        // return the folder
        return outputFile.getParentFile();
    }

    // FIXME: check whether this can be replaced with / moved into ExecUtils
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

        final int exitValue = ExecUtils.waitForProcessToTerminate(process, Arrays.stream(commands).collect(Collectors.joining(" ")));
        if (exitValue != 0) {
            // Handle the case where the process did not complete successfully.
            throw new IOException("Command execution failed with exit code " + exitValue);
        }

        return output.toString().trim(); // Return the accumulated output, which includes the container ID at the end.
    }
}
