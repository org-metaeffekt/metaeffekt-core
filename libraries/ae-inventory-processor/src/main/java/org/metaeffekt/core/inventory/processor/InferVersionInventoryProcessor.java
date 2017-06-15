/**
 * Copyright 2009-2017 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.ProtexInventoryReader;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;


public class InferVersionInventoryProcessor extends AbstractInventoryProcessor {

    public InferVersionInventoryProcessor() {
        super();
    }

    public InferVersionInventoryProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {

        final List<Artifact> artifacts = inventory.getArtifacts();
        for (Artifact artifact: artifacts) {
            if (StringUtils.isEmpty(artifact.getVersion()) && !StringUtils.isEmpty(artifact.getId())) {

                String filename = artifact.getId();
                int index = filename.lastIndexOf("-");
                if (index != -1) {
                    String version = filename.substring(index + 1);
                    index = version.lastIndexOf(".");
                    if (index > 0) {
                        version = version.substring(0, index);
                    }
                    artifact.setVersion(version);
                    System.out.println(version);
                }
            }
        }
    }

}
