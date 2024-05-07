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

import org.metaeffekt.core.inventory.processor.adapter.LicenseSummaryAdapter;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NordeckAppComponentPatternContributor extends ComponentPatternContributor {

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
            add("app/lib/licenses.json");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("app/lib/licenses.json");
    }

    public List<ComponentPatternData> contribute(File baseDir, String virtualRootPath, String relativeAnchorPath, String anchorChecksum) {
        try {
            final File anchorFile = new File(baseDir, relativeAnchorPath);
            final File contextBaseDir = anchorFile.getParentFile().getParentFile();
            final String contextRelPath = FileUtils.asRelativePath(contextBaseDir, anchorFile.getParentFile());
            final String relPath = FileUtils.asRelativePath(baseDir, anchorFile) + "/" + anchorFile.getName();
            final String relativeVersionAnchorPath = contextRelPath + "/" + anchorFile.getName();

            final Inventory inventoryFromLicenseSummary = new LicenseSummaryAdapter().
                    createInventoryFromLicenseSummary(anchorFile, relPath);

            // construct component pattern
            final ComponentPatternData componentPatternData = new ComponentPatternData();
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, relativeVersionAnchorPath);
            componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, "nordeck-application");
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, "$NORDECK_APPLICATION_VERSION");
            componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, "nordeck-application-$NORDECK_APPLICATION_VERSION");

            componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");
            componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN, "**/node_modules/**/*");

            componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);

            componentPatternData.setExpansionInventorySupplier(() -> inventoryFromLicenseSummary);

            return Collections.singletonList(componentPatternData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

}
