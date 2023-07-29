/*
 * Copyright 2009-2022 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ContainerComponentPatternContributor extends ComponentPatternContributor {

    @Override
    public boolean applies(File contextBaseDir, String file) {
        return isContainerMetadata(file);
    }

    @Override
    public List<ComponentPatternData> contribute(File contextBaseDir,
                 String anchorRelPath, String anchorAbsPath, String anchorChecksum) {

        final File anchorFile = new File(contextBaseDir, anchorRelPath);
        final File anchorParentFile = anchorFile.getParentFile();

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR,
                FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile()) + "/" + anchorFile.getName());
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        try {
            JSONObject json = new JSONObject(FileUtils.readFileToString(anchorFile, FileUtils.ENCODING_UTF_8));

            // FIXME: need to check whether this is really a container

            final String id = json.optString("id");

            if (id != null) {

                componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/" + anchorParentFile.getName() + "/**/*");

                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, id);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, id);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, "layer-" + id);

                componentPatternData.set(Constants.KEY_TYPE, "container");
            }

            return Collections.singletonList(componentPatternData);

        } catch (IOException e) {
        }

        return Collections.emptyList();

    }

    boolean isContainerMetadata(String artifactPath) {
        return new File(artifactPath).getName().equalsIgnoreCase("json");
    }



}
