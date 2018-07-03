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
            artifactColumnMap.put(i, row.getCell(i).getStringCellValue());
        }
    }

    @Override
    protected void readLicenseMetaDataHeader(HSSFRow row) {
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            columnMap.put(i, row.getCell(i).getStringCellValue());
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
            }
            if (columnName.equalsIgnoreCase("checksum")) {
                artifact.setChecksum(value);
            }
            if (columnName.equalsIgnoreCase("component") || columnName.equalsIgnoreCase("component / group")) {
                artifact.setComponent(value);
            }
            if (columnName.equalsIgnoreCase("group id")) {
                artifact.setGroupId(value);
            }
            if (columnName.equalsIgnoreCase("version")) {
                artifact.setVersion(value);
            }
            if (columnName.equalsIgnoreCase("license")) {
                artifact.setLicense(value);
            }
            if (columnName.equalsIgnoreCase("latest version")) {
                artifact.setLatestAvailableVersion(value);
            }
            if (columnName.equalsIgnoreCase("security relevance")) {
                artifact.setSecurityRelevant("X".equalsIgnoreCase(value));
            }
            if (columnName.equalsIgnoreCase("security category")) {
                artifact.setSecurityCategory(value);
            }
            if (columnName.equalsIgnoreCase("vulnerability")) {
                artifact.setVulnerability(value);
            }
            if (columnName.equalsIgnoreCase("classification")) {
                artifact.setClassification(value);
            }
            if (columnName.equalsIgnoreCase("comment")) {
                artifact.setComment(value);
            }
            if (columnName.equalsIgnoreCase("analysis")) {
                artifact.setAnalysis(value);
            }
            if (columnName.equalsIgnoreCase("url")) {
                artifact.setUrl(value);
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
            }
            if (columnName.equalsIgnoreCase("version")) {
                licenseMetaData.setVersion(value);
            }
            if (columnName.equalsIgnoreCase("license")) {
                licenseMetaData.setLicense(value);
            }
            if (columnName.equalsIgnoreCase("license in effect")) {
                licenseMetaData.setLicenseInEffect(value);
            }
            if (columnName.equalsIgnoreCase("license notice") || columnName.equalsIgnoreCase("text")) {
                licenseMetaData.setNotice(value);
            }
            if (columnName.equalsIgnoreCase("comment")) {
                licenseMetaData.setComment(value);
            }
            if (columnName.equalsIgnoreCase("source category")) {
                licenseMetaData.setSourceCategory(value);
            }
        }

        if (licenseMetaData.isValid()) {
            return licenseMetaData;
        }

        return null;
    }

}
