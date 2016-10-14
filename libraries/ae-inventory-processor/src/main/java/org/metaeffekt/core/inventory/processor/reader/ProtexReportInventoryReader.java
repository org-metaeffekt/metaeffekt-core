/**
 * Copyright 2009-2016 the original author or authors.
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
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.DefaultArtifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProtexReportInventoryReader extends AbstractXlsInventoryReader {

    public Inventory readInventory(File file) throws FileNotFoundException, IOException {

        FileInputStream myInput = new FileInputStream(file);
        POIFSFileSystem myFileSystem = new POIFSFileSystem(myInput);
        HSSFWorkbook myWorkBook = new HSSFWorkbook(myFileSystem);
        HSSFSheet mySheet = myWorkBook.getSheetAt(6);
        Iterator<?> rows = mySheet.rowIterator();

        Inventory inventory = new Inventory();
        List<Artifact> artifacts = new ArrayList<Artifact>();
        inventory.setArtifacts(artifacts);

        // skip first two lines being the header and a separator
        if (rows.hasNext()) rows.next();
        if (rows.hasNext()) rows.next();

        while (rows.hasNext()) {
            HSSFRow row = (HSSFRow) rows.next();
            Artifact artifact = readArtifactMetaData(row);
            if (artifact != null) {
                artifacts.add(artifact);
            }
        }

        return inventory;
    }

    protected Artifact readArtifactMetaData(HSSFRow row) {
        Iterator<?> cells = row.cellIterator();
        int columnIndex = 0;

        if (row.getPhysicalNumberOfCells() < 7) {
            throw new IllegalArgumentException("Line contains not all relevant information. " + row.getRowNum());
        }

        Artifact artifact = new DefaultArtifact();
        artifact.setReported(true);

        while (cells.hasNext()) {
            HSSFCell myCell = (HSSFCell) cells.next();
            switch (columnIndex) {
                case 2:
                    String s = myCell.toString();
                    artifact.setId(s.substring(s.lastIndexOf("/") + 1));
                    if (!s.endsWith(".jar")) {
                        return null;
                    }
                    break;
                case 6:
                    artifact.setName(myCell.toString());
                    break;
                case 7:
                    artifact.setVersion(myCell.toString());
                    break;
                case 8:
                    artifact.setLicense(myCell.toString());
                    break;
                default:
            }

            columnIndex++;
        }

        return artifact;
    }

}
