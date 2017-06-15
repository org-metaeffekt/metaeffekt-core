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

import java.util.Iterator;


public class DsoInventoryReader extends AbstractXlsInventoryReader {

    protected Artifact readArtifactMetaData(HSSFRow row) {
        Iterator<?> cells = row.cellIterator();
        int columnIndex = 0;

        Artifact artifact = new DefaultArtifact();

        if (row.getPhysicalNumberOfCells() < 8) {
            throw new IllegalArgumentException("Line contains not all relevant information. " + row.getRowNum());
        }

        while (cells.hasNext()) {
            HSSFCell myCell = (HSSFCell) cells.next();
            switch (columnIndex) {
                case 0:
                    artifact.setName(myCell.toString());
                    break;
                case 1:
                    artifact.setId(myCell.toString());
                    break;
                case 2:
                    artifact.setVersion(myCell.toString());
                    break;
                case 3:
                    artifact.setLicense(myCell.toString());
                    break;
                case 4:
                    artifact.setUrl(myCell.toString());
                    break;
                case 5:
                    String project = myCell.toString();
                    artifact.addProject(project);
                    break;
                case 6:
                    String securityRelevant = myCell.toString();
                    if ("x".equalsIgnoreCase(securityRelevant.trim())) {
                        artifact.setSecurityRelevant(true);
                    }
                    break;
                case 7:
                    artifact.setSecurityCategory(myCell.toString());
                    break;
                default:
            }
            columnIndex++;
        }

        return artifact;
    }

}
