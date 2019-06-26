/**
 * Copyright 2009-2018 the original author or authors.
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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class InventoryReader extends AbstractXlsInventoryReader {

    private Map<Integer, String> artifactColumnMap = new HashMap<>();
    private Map<Integer, String> columnMap = new HashMap<>();

    @Override
    protected void readHeader(HSSFRow row) {
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            HSSFCell cell = row.getCell(i);
            String id = cell != null ? cell.getStringCellValue() : "Column " + (i + 1);
            artifactColumnMap.put(i, id);
        }
    }

    @Override
    protected void readLicenseMetaDataHeader(HSSFRow row) {
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            HSSFCell cell = row.getCell(i);
            if (cell != null) {
                columnMap.put(i, cell.getStringCellValue());
            }
        }
    }

    @Override
    protected Artifact readArtifactMetaData(HSSFRow row) {
        Artifact artifact = new Artifact();
        Set<String> projects = new LinkedHashSet<String>();
        artifact.setProjects(projects);

        for (int i = 0; i < artifactColumnMap.size(); i++) {
            final String columnName = artifactColumnMap.get(i);
            final HSSFCell myCell = row.getCell(i);
            final String value = myCell != null ? myCell.toString() : null;

            if (columnName.equalsIgnoreCase("id")) {
                artifact.setId(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("checksum")) {
                artifact.setChecksum(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("component") || columnName.equalsIgnoreCase("component / group")) {
                artifact.setComponent(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("group id")) {
                artifact.setGroupId(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("version")) {
                artifact.setVersion(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("license")) {
                artifact.setLicense(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("latest version")) {
                artifact.setLatestAvailableVersion(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("security relevance")) {
                artifact.setSecurityRelevant("X".equalsIgnoreCase(value));
                continue;
            }
            if (columnName.equalsIgnoreCase("security category")) {
                artifact.setSecurityCategory(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("vulnerability")) {
                artifact.setVulnerability(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("classification")) {
                artifact.setClassification(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("comment")) {
                artifact.setComment(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("analysis")) {
                artifact.setAnalysis(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("url")) {
                artifact.setUrl(value);
                continue;
            }
            if (columnName.equalsIgnoreCase("verified")) {
                artifact.setVerified("X".equalsIgnoreCase(value));
                continue;
            }
            if (columnName.equalsIgnoreCase("projects")) {
                String projectString = value;
                if (StringUtils.hasText(projectString)) {
                    projectString = projectString.trim();
                    String[] split = projectString.split(",");
                    for (int j = 0; j < split.length; j++) {
                        projects.add(split[j].trim());
                    }
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

        for (int i = 0; i < columnMap.size(); i++) {
            final String columnName = columnMap.get(i);
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

}
