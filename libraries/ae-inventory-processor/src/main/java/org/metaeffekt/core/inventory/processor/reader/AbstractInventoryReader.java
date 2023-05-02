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
package org.metaeffekt.core.inventory.processor.reader;

import org.metaeffekt.core.inventory.processor.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractInventoryReader {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractInventoryReader.class);

    public static final String WORKSHEET_NAME_ARTIFACT_DATA = "Artifacts";
    public static final String WORKSHEET_NAME_INVENTORY_INFO = "Info";
    public static final String WORKSHEET_NAME_REPORT_DATA = "Report";
    public static final String WORKSHEET_NAME_ASSET_DATA = "Assets";
    public static final String WORKSHEET_NAME_COMPONENT_PATTERN_DATA = "Component Patterns";
    public static final String WORKSHEET_NAME_VULNERABILITY_DATA = "Vulnerabilities";
    public static final String WORKSHEET_NAME_LICENSE_NOTICES_DATA = "License Notices";
    public static final String WORKSHEET_NAME_LICENSE_DATA = "Licenses";
    public static final String WORKSHEET_NAME_ADVISORY_DATA = "Advisories";

    public Inventory readInventory(File file) throws IOException {
        final FileInputStream myInput = new FileInputStream(file);
        try {
            return readInventory(myInput);
        } finally {
            myInput.close();
        }
    }

    public abstract Inventory readInventory(InputStream in) throws IOException;

    protected void update(Artifact artifact) {
        resolveRename(artifact, "Component / Group", Artifact.Attribute.COMPONENT.getKey());
    }

    protected void update(LicenseMetaData licenseMetaData) {
        resolveRename(licenseMetaData, "Text", LicenseMetaData.Attribute.NOTICE.getKey());
    }

    protected void resolveRename(AbstractModelBase item, String oldKey, String newKey) {
        final String oldKeyValue = item.get(oldKey);
        final String newKeyValue = item.get(newKey);

        if (org.apache.commons.lang3.StringUtils.isEmpty(newKeyValue)) {
            item.set(newKey, oldKeyValue);
        }
    }

    protected void update(VulnerabilityMetaData vulnerabilityMetaData) {
        // compensate rename of attributes
        mapContent(vulnerabilityMetaData, "CVSS Score (v2)", VulnerabilityMetaData.Attribute.V2_SCORE);
        mapContent(vulnerabilityMetaData, "CVSS Base Score (v3)", VulnerabilityMetaData.Attribute.V3_SCORE);
        mapContent(vulnerabilityMetaData, "Maximum Score", VulnerabilityMetaData.Attribute.MAX_SCORE);
    }

    private void mapContent(final VulnerabilityMetaData vulnerabilityMetaData,
                            final String originalKey, final VulnerabilityMetaData.Attribute updatedAttribute) {

        // read the original content
        final String originalKeyContent = vulnerabilityMetaData.get(originalKey);

        // check if there is original content
        if (StringUtils.hasText(originalKeyContent)) {

            // read updated attribute content
            final String updatedAttributeContent = vulnerabilityMetaData.get(updatedAttribute);

            if (!StringUtils.hasText(updatedAttributeContent)) {
                // original content available; no updated content: transfer and delete
                vulnerabilityMetaData.set(updatedAttribute, originalKeyContent);
                vulnerabilityMetaData.set(originalKey, null);
            } else {
                // original content and updated content available
                if (!originalKeyContent.equals(updatedAttributeContent)) {

                    // warn in case the values differ
                    LOG.warn("Vulnerability metadata inconsistent: " +
                                    "[{}] shows different content in attributes [{}] and [{}]. Please consolidate to [{}].",
                            vulnerabilityMetaData.get(VulnerabilityMetaData.Attribute.NAME), originalKey,
                            updatedAttribute.getKey(), updatedAttribute.getKey());
                }

                // values are identical or we warned the consumer: remove the obsolete original key value
                vulnerabilityMetaData.set(originalKey, null);
            }
        }
    }

    protected void applyModificationsForCompatibility(Inventory inventory) {
        inventory.getArtifacts().forEach(this::update);
        inventory.getLicenseMetaData().forEach(this::update);
        inventory.getVulnerabilityMetaData().forEach(this::update);
    }

    protected static class ParsingContext {

        public ParsingContext() {
            this.columns = new ArrayList<>();
            this.columnsMap = new HashMap<>();
        }

        final List<String> columns;
        final Map<Integer, String> columnsMap;
    }

}
