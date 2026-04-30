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
package org.metaeffekt.core.inventory.processor.inspector;

import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SharedObjectInspector implements ArtifactInspector {

    public static final Pattern PATTERN_DOT_SO_DOT = Pattern.compile("^.*\\.so\\.([0-9]+(\\.[0-9]+)*)$");
    public static final Pattern PATTERN_DOT_SO = Pattern.compile("^.*-([0-9]+(\\.[0-9]+)*)\\.so$");

    @Override
    public void run(Inventory inventory, Properties properties) {
        final ProjectPathParam projectPathParam = new ProjectPathParam(properties);

        final Stream<Artifact> filteredArtifacts = inventory.getArtifacts().stream()
                .filter(artifact -> artifact.getPathInAsset() != null &&
                        (artifact.getId().contains(".so.") || artifact.getId().endsWith(".so")));

        for (Artifact artifact : filteredArtifacts.collect(Collectors.toList())) {
            final File file = new File(projectPathParam.getProjectPath().getAbsolutePath() + "/" + artifact.getPathInAsset());
            if (file.exists()) {

                // check whether file is or was (corrupted by extraction) a symbolic link
                boolean isSymbolicLinkToHigherVersion = isSymbolicLinkToHigherVersion(file);

                if (isSymbolicLinkToHigherVersion) {
                    // do nothing; the file only references a more specific file
                    continue;
                }

                // apply mather dependending on conventions; xyz.so.3.11.3
                if (artifact.getId().endsWith(".so")) {
                    Matcher matcher = PATTERN_DOT_SO.matcher(artifact.getId());
                    if (matcher.matches()) {
                        artifact.setVersion(matcher.group(1));
                        continue;
                    }
                }

                // apply mather dependending on conventions; xyz-3.11.3.so.
                if (artifact.getId().contains(".so.")) {
                    Matcher matcher = PATTERN_DOT_SO_DOT.matcher(artifact.getId());
                    if (matcher.matches()) {
                        artifact.setVersion(matcher.group(1));
                    }
                }
            }
        }
    }

    private boolean isSymbolicLinkToHigherVersion(File file) {
        if (FileUtils.isSymlink(file)) {
            return true;
        }

        // maybe a corrupted symlink; check size; read content
        if (file.length() < 512) {
            try {
                String content = FileUtils.readFileToString(file, FileUtils.ENCODING_UTF_8);
                return content.startsWith(file.getName());
            } catch (IOException e) {
                // cannot determine
                return false;
            }
        }
        return false;
    }

}

