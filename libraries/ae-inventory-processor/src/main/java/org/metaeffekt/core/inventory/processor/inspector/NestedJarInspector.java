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
package org.metaeffekt.core.inventory.processor.inspector;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.metaeffekt.core.inventory.processor.filescan.FileSystemScanConstants.*;

/**
 * Checks if a file (for particular file types) has other files inside of it (with particular extensions). If this
 * is the case, the file is marked with a 'scan' classification.
 */
public class NestedJarInspector implements ArtifactInspector {

    /**
     * The accepted list of extensions for project files.<br>
     * Ensure that these are lower-case, as the check later is made case-insensitive by toLowerCase.
     */
    private final String[] outsideExtensions = {".jar", ".war", ".ear", ".sar", ".webjar", ".xar"};

    /**
     * The accepted list of extensions for files inside the project files.
     */
    private final String[] insideExtensions = outsideExtensions;

    /**
     * Checks whether filename ends with one of the accepted suffixes.
     * @param toCheck will be checked for all suffixes.
     * @param acceptedSuffix are suffixes that will be accepted by the method.
     * @return whether the input string ends with one of the suffixes.
     */
    private static boolean hasExtension(String toCheck, String[] acceptedSuffix) {
        // Optimization: could be improved by cutting the end of the string (after the last, second to last etc dot)
        //  and then check for contains from a Set of accepted extensions. should be good enough for now though.
        for (String extension : acceptedSuffix) {
            if (toCheck.toLowerCase(Locale.ENGLISH).endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    private boolean outsideHasExtension(String filename) {
        return hasExtension(filename, outsideExtensions);
    }

    private boolean insideHasExtension(String filename) {
        return hasExtension(filename, insideExtensions);
    }

    protected void addError(Artifact artifact, String errorString) {
        artifact.append("Errors", errorString, ", ");
    }

    @Override
    public void run(Inventory inventory, Properties properties) {
        for (Artifact artifact : inventory.getArtifacts()) {

            if (artifact.hasClassification(HINT_ATOMIC)) continue;
            if (artifact.hasClassification(HINT_IGNORE)) continue;

            run(artifact, properties);
        }
    }

    public void run(Artifact artifact, Properties properties) {
        final Set<String> paths = collectPaths(artifact);

        final ProjectPathParam projectPathParam = new ProjectPathParam(properties);

        File projectFile;
        for (String projectString : paths) {
            if (projectString == null) {
                continue;
            }

            if (!outsideHasExtension(projectString)) {
                continue;
            }

            projectFile = new File(projectPathParam.getProjectPath(), projectString);

            try (ZipFile jarZip = new ZipFile(projectFile)) {
                // loop all entries and check their extensions
                final boolean semaphore[] = new boolean[1];
                semaphore[0] = false;
                jarZip.stream().forEach(entry -> {
                    if (!semaphore[0]) {
                        if (insideHasExtension(entry.getName())) {
                            // one of the files has one of the specified extensions!
                            artifact.setClassification(HINT_SCAN);
                            semaphore[0] = true;
                        }
                    }
                });
            } catch (IOException e) {
                // if we can't read this project entry, just carry on as if nothing happened
                addError(artifact, "NestedJavaInspector couldn't read as zip archive");
                continue;
            }

            // since this is our only job, we can stop processing other getProjects entries
            if (HINT_SCAN.equals(artifact.getClassification())) {
                break;
            }
        }
    }

    private static Set<String> collectPaths(Artifact artifact) {
        Set<String> paths = new HashSet<>();
        String jarPath = artifact.get(FileSystemScanConstants.ATTRIBUTE_KEY_ARTIFACT_PATH);
        if (StringUtils.isNotBlank(jarPath)) {
            paths.add(jarPath);
        }
        paths.addAll(artifact.getProjects());
        return paths;
    }
}
