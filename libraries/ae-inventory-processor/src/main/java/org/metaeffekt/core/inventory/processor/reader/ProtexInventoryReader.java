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
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.DefaultArtifact;

import java.util.Iterator;

public class ProtexInventoryReader extends AbstractXlsInventoryReader {

    protected Artifact readArtifactMetaData(HSSFRow row) {
        Iterator<?> cells = row.cellIterator();
        int columnIndex = 0;

        if (row.getPhysicalNumberOfCells() < 5) {
            throw new IllegalArgumentException("Line contains not all relevant information. " + row.getRowNum());
        }

        Artifact artifact = new DefaultArtifact();
        artifact.setReported(true);
        artifact.setUsed(true);

        while (cells.hasNext()) {
            HSSFCell myCell = (HSSFCell) cells.next();
            switch (columnIndex) {
                case 0:
                    artifact.setName(myCell.toString());
                    break;
                case 1:
                    artifact.setVersion(myCell.toString());
                    break;
                case 2:
                    String projectString = myCell.toString();
                    if (projectString != null) {
                        projectString = projectString.trim();
                        String[] split = projectString.split(",");
                        for (int i = 0; i < split.length; i++) {
                            artifact.getProjects().add(split[i]);
                        }
                    }
                    break;
                case 3:
                    artifact.setLicense(myCell.toString());
                    break;
                case 4:
                    artifact.setUrl(myCell.toString());
                    break;
                default:
            }

            columnIndex++;
        }

        return artifact;
    }

}
