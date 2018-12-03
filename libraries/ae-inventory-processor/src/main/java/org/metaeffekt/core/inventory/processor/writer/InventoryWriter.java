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
package org.metaeffekt.core.inventory.processor.writer;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class InventoryWriter {

    public void writeInventory(Inventory inventory, File file) throws IOException {
        HSSFWorkbook myWorkBook = new HSSFWorkbook();

        writeArtifacts(inventory, myWorkBook);
        writeNotices(inventory, myWorkBook);

        FileOutputStream out = new FileOutputStream(file);
        try {
            myWorkBook.write(out);
        } finally {
            out.flush();
            out.close();
        }

    }

    private void writeArtifacts(Inventory inventory, HSSFWorkbook myWorkBook) {
        HSSFSheet mySheet = myWorkBook.createSheet("Artifact Inventory");
        mySheet.createFreezePane(0, 1);
        mySheet.setAutoFilter(new CellRangeAddress(0, 65000, 0, 14));
        mySheet.setDefaultColumnWidth(20);

        HSSFRow myRow = null;
        HSSFCell myCell = null;

        int rowNum = 0;

        myRow = mySheet.createRow(rowNum++);

        HSSFCellStyle headerStyle = createHeaderStyle(myWorkBook);

        int cellNum = 0;
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Id"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Checksum"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Component"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Group Id"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Version"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Latest Version"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("License"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Security Relevance"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Security Category"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Vulnerability"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Classification"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Comment"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("URL"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Projects"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Verified"));

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            attributes.addAll(artifact.getAttributes());
        }

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        for (String key : ordered) {
            myCell = myRow.createCell(cellNum++);
            myCell.setCellStyle(headerStyle);
            myCell.setCellValue(new HSSFRichTextString(key));
        }

        int numCol = cellNum;

        for (Artifact artifact : inventory.getArtifacts()) {
            myRow = mySheet.createRow(rowNum++);

            cellNum = 0;
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getId()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getChecksum()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getComponent()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getGroupId()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getVersion()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getLatestAvailableVersion()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getLicense()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.isSecurityRelevant() ? "X" : ""));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getSecurityCategory()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getVulnerability()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getClassification()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getComment()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.getUrl()));
            myCell = myRow.createCell(cellNum++);
            String projects = artifact.getProjects().toString();
            projects = projects.substring(1, projects.length() - 1);
            myCell.setCellValue(new HSSFRichTextString(projects));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(artifact.isVerified() ? "X" : ""));

            for (String key : ordered) {
                myCell = myRow.createCell(cellNum++);
                myCell.setCellValue(new HSSFRichTextString(artifact.get(key)));
            }
        }

        // adjust with of cells
        for (int i = 0; i <= numCol; i++) {
            Integer width = (Integer) inventory.getContextMap().get("artifacts.column[" + i + "].width");
            if (width != null) {
                mySheet.setColumnWidth(i, width);
            }
        }

    }

    private HSSFCellStyle createHeaderStyle(HSSFWorkbook myWorkBook) {
        Font headerFont = myWorkBook.createFont();
        headerFont.setColor(Font.COLOR_NORMAL);

        HSSFPalette palette = myWorkBook.getCustomPalette();
        HSSFColor headerColor = palette.findSimilarColor((byte) 149, (byte) 179, (byte) 215);

        HSSFCellStyle headerStyle = myWorkBook.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor.getIndex());
        headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        return headerStyle;
    }

    private void writeNotices(Inventory inventory, HSSFWorkbook myWorkBook) {
        HSSFSheet mySheet = myWorkBook.createSheet("License Notices");
        mySheet.createFreezePane(0, 1);
        mySheet.setAutoFilter(new CellRangeAddress(0, 65000, 0, 4));
        mySheet.setDefaultColumnWidth(80);

        HSSFRow myRow = null;
        HSSFCell myCell = null;

        int rowNum = 0;

        myRow = mySheet.createRow(rowNum++);

        HSSFCellStyle headerStyle = createHeaderStyle(myWorkBook);

        int cellNum = 0;
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Component"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Version"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("License"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("License in Effect"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Source Category"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("License Notice"));
        myCell = myRow.createCell(cellNum++);
        myCell.setCellStyle(headerStyle);
        myCell.setCellValue(new HSSFRichTextString("Comment"));

        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            myRow = mySheet.createRow(rowNum++);

            cellNum = 0;
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(licenseMetaData.getComponent()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(licenseMetaData.getVersion()));
            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(licenseMetaData.getLicense()));
            myCell = myRow.createCell(cellNum++);
            String licenseInEffect = licenseMetaData.getLicenseInEffect();
            if (StringUtils.isEmpty(licenseInEffect)) {
                licenseInEffect = licenseMetaData.getLicense();
            }
            myCell.setCellValue(new HSSFRichTextString(licenseInEffect));

            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(licenseMetaData.getSourceCategory()));

            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(licenseMetaData.getNotice()));

            myCell = myRow.createCell(cellNum++);
            myCell.setCellValue(new HSSFRichTextString(licenseMetaData.getComment()));
        }

        for (int i = 0; i < 6; i++) {
            Integer width = (Integer) inventory.getContextMap().get("obligations.column[" + i + "].width");
            if (width != null) {
                mySheet.setColumnWidth(i, width);
            }
        }

    }

}
