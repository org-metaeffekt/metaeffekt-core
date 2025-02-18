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

import static org.metaeffekt.core.itest.common.setup.TestConfig.getDownloadFolder;

// FIXME; integrate with TestSetup check hash
public class ContainerDumpSetup {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerDumpSetup.class);

    public static File saveContainerFromRegistryByRepositoryAndTag(String registryUrl, String repository, String tag, String scanSubFolder) {
        return saveContainerFromRegistryByRepositoryAndTag(registryUrl, repository, tag, null, scanSubFolder);
    }

    public static File saveContainerFromRegistryByRepositoryAndTag(String registryUrl, String repository, String tag, String digest, String scanSubFolder) {
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
            LOG.info("Created download folder: {}", downloadFolder);
        }

        String outputFilePath = downloadFolder + filename;
        File outputFile = new File(outputFilePath);
        File outputInspectFile = new File(outputFilePath.replace(".tar", ".json"));

        if (registryUrl == null || registryUrl.isEmpty()) {
            registryUrl = "docker.io";
        }

        String imageName;

        if (digest != null && !digest.isEmpty()) {
            imageName = registryUrl + "/" + repository + "@" + digest;
        } else {
            imageName = registryUrl + "/" + repository + ":" + tag;
        }

        // pull and save image if required
        if (!outputFile.exists()) {
            try {
                LOG.info("Downloading image [{}] from registry...", imageName);
                executeCommand(new String[]{"docker", "pull", imageName});
                LOG.info("Downloading image [{}] from registry completed.", imageName);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to pull image from registry.", e);
            }

            try {
                executeCommand(new String[] {"docker", "save", imageName, "-o", outputFilePath});
                FileUtils.validateExists(outputFile);
                LOG.info("Saved container [{}] to file [{}].", imageName, outputFilePath);
            } catch (IOException | IllegalStateException | InterruptedException e) {
                throw new RuntimeException("Failed to save container filesystem.", e);
            }
        }

        // run inspect if required
        if (!outputInspectFile.exists()) {
            try {
                String content = executeCommand(new String[] {"docker", "inspect", imageName});
                FileUtils.write(outputInspectFile, content, FileUtils.ENCODING_UTF_8);
                LOG.info("Inspected container image [{}].", imageName);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to inspect container.", e);
            }
        }

        // return the folder
        return outputFile.getParentFile();
    }

    private static String executeCommand(String[] commands) throws IOException, InterruptedException {
        final ExecUtils.ExecParam execParam = new ExecUtils.ExecParam(commands);
        execParam.retainErrorOutputs();
        execParam.retainOutputs();

        ExecUtils.ExecMonitor execMonitor = ExecUtils.executeCommandAndWaitForProcessToTerminate(execParam);

        final Integer exitCode = execMonitor.getExitCode().orElse(0);
        if (exitCode != 0) {
            final String output = execMonitor.getErrorOutput().orElse("<no output>");
            throw new IOException(String.format("Command not successful; error code=%s: %s", exitCode, output));
        }

        // return the accumulated output
        return execMonitor.getOutput().orElseThrow(IOException::new).trim();
    }
}
