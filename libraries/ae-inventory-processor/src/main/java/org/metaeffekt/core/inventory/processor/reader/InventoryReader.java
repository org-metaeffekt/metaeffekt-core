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
import org.springframework.util.StringUtils;

import java.util.*;

public class InventoryReader extends AbstractXlsInventoryReader {

    private Map<Integer, String> artifactColumnMap = new HashMap<>();

    private Map<Integer, String> licenseMetaDataColumnMap = new HashMap<>();
    private Map<Integer, String> licenseDataColumnMap = new HashMap<>();
    private Map<Integer, String> componentPatternDataColumnMap = new HashMap<>();
    private Map<Integer, String> vulnerabilityMetaDataColumnMap = new HashMap<>();

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
        Map<Integer, String> map = this.componentPatternDataColumnMap;
        for (int i = 0; i < map.size(); i++) {
            final String columnName = map.get(i).trim();
            final HSSFCell myCell = row.getCell(i);
            final String value = myCell != null ? myCell.toString() : null;
            if (value != null) {
                componentPatternData.set(columnName, value.trim());
            }
        }

        if (componentPatternData.isValid()) {
            return componentPatternData;
        }

        return null;
    }

    @Override
    protected LicenseData readLicenseData(HSSFRow row) {
        final LicenseData licenseData = new LicenseData();
        Map<Integer, String> map = this.licenseDataColumnMap;
        for (int i = 0; i < map.size(); i++) {
            final String columnName = map.get(i).trim();
            final HSSFCell myCell = row.getCell(i);
            final String value = myCell != null ? myCell.toString() : null;
            if (value != null) {
                licenseData.set(columnName, value.trim());
            }
        }

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
        Map<Integer, String> map = this.vulnerabilityMetaDataColumnMap;
        for (int i = 0; i < map.size(); i++) {
            final String columnName = map.get(i).trim();
            final HSSFCell myCell = row.getCell(i);
            final String value = myCell != null ? myCell.toString() : null;
            if (value != null) {
                vulnerabilityMetaData.set(columnName, value.trim());
            }
        }

        if (vulnerabilityMetaData.isValid()) {
            return vulnerabilityMetaData;
        }

        return null;
    }

}
