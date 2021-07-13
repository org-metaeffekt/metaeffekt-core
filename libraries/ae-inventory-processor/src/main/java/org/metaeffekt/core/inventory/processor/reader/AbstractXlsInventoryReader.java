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

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.metaeffekt.core.inventory.processor.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public abstract class AbstractXlsInventoryReader {

    public Inventory readInventory(File file) throws FileNotFoundException, IOException {
        FileInputStream myInput = new FileInputStream(file);
        try {
            return readInventory(myInput);
        } finally {
            myInput.close();
        }
    }

    public Inventory readInventory(InputStream in) throws FileNotFoundException, IOException {
        POIFSFileSystem fileSystem = new POIFSFileSystem(in);
        HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);

        Inventory inventory = new Inventory();

        readArtifactMetaData(workbook, inventory);
        readLicenseMetaData(workbook, inventory);
        readLicenseData(workbook, inventory);
        readComponentPatternData(workbook, inventory);
        readVulnerabilityMetaData(workbook, inventory);

        return inventory;
    }

    protected void readArtifactMetaData(HSSFWorkbook workbook, Inventory inventory) {
        HSSFSheet sheet = workbook.getSheet("Artifact Inventory");
        if (sheet == null) return;

        Iterator<?> rows = sheet.rowIterator();

        List<Artifact> artifacts = new ArrayList<Artifact>();
        inventory.setArtifacts(artifacts);

        if (rows.hasNext()) {
            List<String> columns = readArtifactHeader((HSSFRow) rows.next());
            while (rows.hasNext()) {
                HSSFRow row = (HSSFRow) rows.next();
                Artifact artifact = readArtifactMetaData(row);
                if (artifact != null) {
                    artifacts.add(artifact);
                }
            }
            inventory.getContextMap().put("artifact-column-list", columns);
        }
    }


    protected void readComponentPatternData(HSSFWorkbook workBook, Inventory inventory) {
        HSSFSheet sheet = workBook.getSheet("Component Patterns");
        if (sheet == null) return;
        Iterator<?> rows = sheet.rowIterator();

        List<ComponentPatternData> componentPatternData = new ArrayList<>();
        inventory.setComponentPatternData(componentPatternData);

        if (rows.hasNext()) {
            readComponentPatternDataHeader((HSSFRow) rows.next());
        }

        int columns = 0;

        while (rows.hasNext()) {
            HSSFRow row = (HSSFRow) rows.next();
            ComponentPatternData cpd = readComponentPatternData(row);
            if (cpd != null) {
                componentPatternData.add(cpd);
                columns = cpd.numAttributes();
            }
        }

        for (int i = 0; i < columns; i++) {
            int width = sheet.getColumnWidth(i);
            inventory.getContextMap().put("patterns.column[" + i + "].width", width);
        }
    }

    protected void readVulnerabilityMetaData(HSSFWorkbook workbook, Inventory inventory) {
        HSSFSheet sheet = workbook.getSheet("Vulnerabilities");
        if (sheet == null) return;
        Iterator<?> rows = sheet.rowIterator();

        List<VulnerabilityMetaData> vulnerabilityMetaData = new ArrayList<>();
        inventory.setVulnerabilityMetaData(vulnerabilityMetaData);

        if (rows.hasNext()) {
            readVulnerabilityMetaDataHeader((HSSFRow) rows.next());
        }

        int columns = 0;

        while (rows.hasNext()) {
            HSSFRow row = (HSSFRow) rows.next();
            VulnerabilityMetaData vmd = readVulnerabilityMetaData(row);
            if (vmd != null) {
                vulnerabilityMetaData.add(vmd);
                columns = vmd.numAttributes();
            }
        }

        for (int i = 0; i < columns; i++) {
            int width = sheet.getColumnWidth(i);
            inventory.getContextMap().put("vulnerabilities.column[" + i + "].width", width);
        }
    }


    protected List<String> readArtifactHeader(HSSFRow row) {
        // default implementation does nothing
        return Collections.emptyList();
    }

    protected void readLicenseMetaDataHeader(HSSFRow row) {
        // default implementation does nothing
    }

    protected void readLicenseDataHeader(HSSFRow row) {
        // default implementation does nothing
    }

    protected void readComponentPatternDataHeader(HSSFRow row) {
        // default implementation does nothing
    }

    protected void readVulnerabilityMetaDataHeader(HSSFRow row) {
        // default implementation does nothing
    }

    protected void readLicenseMetaData(HSSFWorkbook workbook, Inventory inventory) {
        HSSFSheet sheet = workbook.getSheet("License Notices");
        if (sheet == null) sheet = workbook.getSheet("Obligation Notices");
        if (sheet == null) sheet = workbook.getSheet("Component Notices");
        if (sheet == null) return;

        Iterator<?> rows = sheet.rowIterator();

        List<LicenseMetaData> licenseMetaDatas = new ArrayList<LicenseMetaData>();
        inventory.setLicenseMetaData(licenseMetaDatas);

        // skip first line being the header
        if (rows.hasNext()) {
            readLicenseMetaDataHeader((HSSFRow) rows.next());
        }

        while (rows.hasNext()) {
            HSSFRow row = (HSSFRow) rows.next();
            LicenseMetaData licenseMetaData = readLicenseMetaData(row);
            if (licenseMetaData != null) {
                licenseMetaDatas.add(licenseMetaData);
            }
        }

        for (int i = 0; i < 5; i++) {
            int width = sheet.getColumnWidth(i);
            inventory.getContextMap().put("obligations.column[" + i + "].width", width);
        }
    }

    protected void readLicenseData(HSSFWorkbook workbook, Inventory inventory) {
        final HSSFSheet sheet = workbook.getSheet("Licenses");
        if (sheet == null) return;

        final Iterator<?> rows = sheet.rowIterator();

        final List<LicenseData> licenseDatas = new ArrayList<LicenseData>();
        inventory.setLicenseData(licenseDatas);

        // skip first line being the header
        if (rows.hasNext()) {
            readLicenseDataHeader((HSSFRow) rows.next());
        }

        while (rows.hasNext()) {
            HSSFRow row = (HSSFRow) rows.next();
            LicenseData licenseData = readLicenseData(row);
            if (licenseData != null) {
                licenseDatas.add(licenseData);
            }
        }

        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            int width = sheet.getColumnWidth(i);
            inventory.getContextMap().put("licenses.column[" + i + "].width", width);
        }
    }

    protected LicenseMetaData readLicenseMetaData(HSSFRow row) {
        throw new UnsupportedOperationException();
    }
    protected LicenseData readLicenseData(HSSFRow row) {
        throw new UnsupportedOperationException();
    }
    protected ComponentPatternData readComponentPatternData(HSSFRow row) {
        throw new UnsupportedOperationException();
    }
    protected VulnerabilityMetaData readVulnerabilityMetaData(HSSFRow row) {
        throw new UnsupportedOperationException();
    }

    protected abstract Artifact readArtifactMetaData(HSSFRow row);

}
