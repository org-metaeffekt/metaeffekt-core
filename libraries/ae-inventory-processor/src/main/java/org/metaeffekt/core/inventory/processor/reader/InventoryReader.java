/*
 * Copyright 2009-2021 the original author or authors.
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

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.metaeffekt.core.inventory.processor.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;

public class InventoryReader extends AbstractXlsInventoryReader {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReader.class);

    private static final String DEPRECATED_KEY_V2_SCORE = "CVSS Score (v2)";
    private static final String DEPRECATED_KEY_V3_SCORE = "CVSS Base Score (v3)";
    private static final String DEPRECATED_KEY_MAX_SCORE = "Maximum Score";

    private Map<Integer, String> artifactColumnMap = new HashMap<>();

    private Map<Integer, String> licenseMetaDataColumnMap = new HashMap<>();
    private Map<Integer, String> licenseDataColumnMap = new HashMap<>();
    private Map<Integer, String> componentPatternDataColumnMap = new HashMap<>();
    private Map<Integer, String> vulnerabilityMetaDataColumnMap = new HashMap<>();
    private Map<Integer, String> certMetaDataColumnMap = new HashMap<>();
    private Map<Integer, String> assetMetaDataColumnMap = new HashMap<>();

    @Override
    protected List<String> readArtifactHeader(HSSFRow row) {
        return parseColumns(row, artifactColumnMap);
    }

    @Override
    protected void readLicenseMetaDataHeader(HSSFRow row) {
        parseColumns(row, licenseMetaDataColumnMap);
    }

    @Override
    protected void readLicenseDataHeader(HSSFRow row) {
        parseColumns(row, licenseDataColumnMap);
    }

    @Override
    protected void readComponentPatternDataHeader(HSSFRow row) {
        parseColumns(row, componentPatternDataColumnMap);
    }

    @Override
    protected void readAssetMetaDataHeader(HSSFRow row) {
        parseColumns(row, assetMetaDataColumnMap);
    }

    protected List<String> parseColumns(HSSFRow row, Map<Integer, String> map) {
        List<String> columnList = new ArrayList<>();
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            HSSFCell cell = row.getCell(i);
            if (cell != null) {
                String value = cell.getStringCellValue();
                map.put(i, value);
                columnList.add(value);
            }
        }
        return columnList;
    }

    @Override
    protected Artifact readArtifactMetaData(HSSFRow row) {
        Artifact artifact = new Artifact();
        Set<String> projects = new LinkedHashSet<>();
        artifact.setProjects(projects);

        for (int i = 0; i < artifactColumnMap.size(); i++) {
            final String columnName = artifactColumnMap.get(i).trim();
            final HSSFCell myCell = row.getCell(i);
            final String value = myCell != null ? myCell.toString() : null;

            // compatibility
            if (columnName.equalsIgnoreCase("component / group")) {
                if (StringUtils.isEmpty(artifact.getComponent())) {
                    artifact.setComponent(value);
                }
                continue;
            }

            // if the column in not known we store the content in the key/value store
            if (StringUtils.hasText(value)) {
                artifact.set(columnName, value.trim());
            }
        }

        if (artifact.isValid()) {
            return artifact;
        }

        return null;
    }

    @Override
    protected LicenseMetaData readLicenseMetaData(HSSFRow row) {
        final LicenseMetaData licenseMetaData = new LicenseMetaData();

        for (int i = 0; i < licenseMetaDataColumnMap.size(); i++) {
            final String columnName = licenseMetaDataColumnMap.get(i).trim();
            final HSSFCell myCell = row.getCell(i);
            final String value = myCell != null ? myCell.toString() : null;

            if (columnName.equalsIgnoreCase("component")) {
                licenseMetaData.setComponent(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("version")) {
                licenseMetaData.setVersion(value);
                continue;
            }

            if (columnName.equalsIgnoreCase("license")) {
                licenseMetaData.setLicense(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("license in effect")) {
                licenseMetaData.setLicenseInEffect(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("license notice") || columnName.equalsIgnoreCase("text")) {
                licenseMetaData.setNotice(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("comment")) {
                licenseMetaData.setComment(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("source category")) {
                licenseMetaData.setSourceCategory(value);
                continue;
            }

            // if the column in not known we store the content in the key/value store
            if (value != null) {
                licenseMetaData.set(columnName, value.trim());
            }
        }

        if (licenseMetaData.isValid()) {
            return licenseMetaData;
        }

        return null;
    }

    @Override
    protected ComponentPatternData readComponentPatternData(HSSFRow row) {
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        readValues(row, componentPatternData, this.componentPatternDataColumnMap);
        if (componentPatternData.isValid()) {
            return componentPatternData;
        }
        return null;
    }

    @Override
    protected LicenseData readLicenseData(HSSFRow row) {
        final LicenseData licenseData = new LicenseData();
        readValues(row, licenseData, this.licenseDataColumnMap);
        if (licenseData.isValid()) {
            return licenseData;
        }
        return null;
    }

    @Override
    protected void readVulnerabilityMetaDataHeader(HSSFRow row) {
        parseColumns(row, vulnerabilityMetaDataColumnMap);
    }


    @Override
    protected VulnerabilityMetaData readVulnerabilityMetaData(HSSFRow row) {
        final VulnerabilityMetaData vulnerabilityMetaData = new VulnerabilityMetaData();
        readValues(row, vulnerabilityMetaData, this.vulnerabilityMetaDataColumnMap);

        if (vulnerabilityMetaData.isValid()) {
            // compensate rename of attributes
            mapContent(vulnerabilityMetaData, DEPRECATED_KEY_V2_SCORE, VulnerabilityMetaData.Attribute.V2_SCORE);
            mapContent(vulnerabilityMetaData, DEPRECATED_KEY_V3_SCORE, VulnerabilityMetaData.Attribute.V3_SCORE);
            mapContent(vulnerabilityMetaData, DEPRECATED_KEY_MAX_SCORE, VulnerabilityMetaData.Attribute.MAX_SCORE);
            return vulnerabilityMetaData;
        }
        return null;
    }


    @Override
    protected CertMetaData readCertMetaData(HSSFRow row) {
        final CertMetaData certMetaData = new CertMetaData();
        readValues(row, certMetaData, this.certMetaDataColumnMap);
        if (certMetaData.isValid()) {
            return certMetaData;
        }
        return null;
    }

    @Override
    protected AssetMetaData readAssetMetaData(HSSFRow row) {
        final AssetMetaData assetMetaData = new AssetMetaData();
        readValues(row, assetMetaData, this.assetMetaDataColumnMap);
        if (assetMetaData.isValid()) {
            return assetMetaData;
        }
        return null;
    }

    private void readValues(HSSFRow row, AbstractModelBase modelBase, Map<Integer, String> map) {
        for (int i = 0; i < map.size(); i++) {
            final String columnName = map.get(i).trim();
            final HSSFCell cell = row.getCell(i);
            final String value = cell != null ? cell.toString() : null;
            if (value != null) {
                modelBase.set(columnName, value.trim());
            }
        }
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

}
