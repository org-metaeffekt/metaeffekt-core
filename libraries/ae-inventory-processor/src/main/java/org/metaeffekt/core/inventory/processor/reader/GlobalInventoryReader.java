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
package org.metaeffekt.core.inventory.processor.reader;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.DefaultArtifact;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GlobalInventoryReader extends AbstractXlsInventoryReader {

    private Map<Integer, String> columnMap = new HashMap<>();

    @Override
    protected void readHeader(HSSFRow row) {
        super.readHeader(row);

        if (row.getPhysicalNumberOfCells() < 15) {
            throw new IllegalArgumentException("Header does not contain not all relevant information. " +
                    row.getPhysicalNumberOfCells());
        }
    }

    @Override
    protected void readLicenseMetaDataHeader(HSSFRow row) {
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            columnMap.put(i, row.getCell(i).getStringCellValue());
        }
    }

    protected Artifact readArtifactMetaData(HSSFRow row) {
        Artifact artifact = new DefaultArtifact();
        Set<String> projects = new LinkedHashSet<String>();
        artifact.setProjects(projects);
        artifact.setReported(true);

        int i = 0;

        // component
        HSSFCell myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setId(myCell.toString());
        }

        // name
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setName(myCell.toString());
        }

        // groupId
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setGroupId(myCell.toString());
        }

        // version
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setVersion(myCell.toString());
        }

        // latest available version
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setLatestAvailableVersion(myCell.toString());
        }

        // license
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setLicense(myCell.toString());
        }

        // security relevant
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setSecurityRelevant("X".equalsIgnoreCase(myCell.toString()));
        }

        // security category
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setSecurityCategory(myCell.toString());
        }

        // classification
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setClassification(myCell.toString());
        }

        // classification
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setComment(myCell.toString());
        }

        // license url 
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setUrl(myCell.toString());
        }

        // project list
        myCell = row.getCell(i++);
        if (myCell != null) {
            String projectString = myCell.toString();
            if (StringUtils.hasText(projectString)) {
                projectString = projectString.trim();
                String[] split = projectString.split(",");
                for (int j = 0; j < split.length; j++) {
                    projects.add(split[j].trim());
                }
            }
        }

        // used
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setUsed("X".equalsIgnoreCase(myCell.toString()));
        }

        // reported (protex)
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setReported("X".equalsIgnoreCase(myCell.toString()));
        }

        // version reported explicitly (protex)
        myCell = row.getCell(i++);
        if (myCell != null) {
            artifact.setVersionReported("X".equalsIgnoreCase(myCell.toString()));
        }

        return artifact;
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
        }

        if (licenseMetaData.isValid()) {
            return licenseMetaData;
        }

        return licenseMetaData;
    }

}
