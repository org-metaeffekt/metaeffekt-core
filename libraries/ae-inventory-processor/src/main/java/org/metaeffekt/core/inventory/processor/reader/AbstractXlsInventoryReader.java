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

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;

import java.io.*;
import java.util.ArrayList;
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
        POIFSFileSystem myFileSystem = new POIFSFileSystem(in);
        HSSFWorkbook myWorkBook = new HSSFWorkbook(myFileSystem);

        Inventory inventory = new Inventory();

        readArtifactMetaData(myWorkBook, inventory);
        readLicenseMetaData(myWorkBook, inventory);

        return inventory;
    }

    protected void readArtifactMetaData(HSSFWorkbook myWorkBook, Inventory inventory) {
        HSSFSheet mySheet = myWorkBook.getSheetAt(0);
        Iterator<?> rows = mySheet.rowIterator();

        List<Artifact> artifacts = new ArrayList<Artifact>();
        inventory.setArtifacts(artifacts);

        if (rows.hasNext()) {
            readHeader((HSSFRow) rows.next());
        }

        while (rows.hasNext()) {
            HSSFRow row = (HSSFRow) rows.next();
            Artifact artifact = readArtifactMetaData(row);
            artifacts.add(artifact);
        }

        for (int i = 0; i < 15; i++) {
            int width = mySheet.getColumnWidth(i);
            inventory.getContextMap().put("artifacts.column[" + i + "].width", width);
        }
    }

    protected void readHeader(HSSFRow row) {
        // default implementation does nothing
    }

    protected void readLicenseMetaDataHeader(HSSFRow row) {
        // default implementation does nothing
    }

    protected void readLicenseMetaData(HSSFWorkbook myWorkBook, Inventory inventory) {
        if (myWorkBook.getNumberOfSheets() > 1) {
            HSSFSheet mySheet = myWorkBook.getSheetAt(1);
            if (mySheet != null) {
                Iterator<?> rows = mySheet.rowIterator();

                List<LicenseMetaData> licenseMetaDatas = new ArrayList<LicenseMetaData>();
                inventory.setLicenseMetaData(licenseMetaDatas);

                // skip first line being the header
                if (rows.hasNext()) {
                    readLicenseMetaDataHeader((HSSFRow) rows.next());
                }

                while (rows.hasNext()) {
                    HSSFRow row = (HSSFRow) rows.next();
                    LicenseMetaData licenseMetaData = readLicenseMetaData(row);
                    licenseMetaDatas.add(licenseMetaData);
                }
            }

            for (int i = 0; i < 5; i++) {
                int width = mySheet.getColumnWidth(i);
                inventory.getContextMap().put("obligations.column[" + i + "].width", width);
            }
        }
    }

    protected LicenseMetaData readLicenseMetaData(HSSFRow row) {
        throw new UnsupportedOperationException();
    }

    protected abstract Artifact readArtifactMetaData(HSSFRow row);

}
