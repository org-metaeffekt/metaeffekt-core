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
package org.metaeffekt.core.inventory.processor.writer;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.util.CellRangeAddress;
import org.metaeffekt.core.inventory.processor.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class InventoryWriter {

    private Artifact.Attribute[] artifactColumnOrder = new Artifact.Attribute[]{
            Artifact.Attribute.ID,
            Artifact.Attribute.CHECKSUM,
            Artifact.Attribute.COMPONENT,
            Artifact.Attribute.GROUPID,
            Artifact.Attribute.VERSION,
            Artifact.Attribute.LATEST_VERSION,
            Artifact.Attribute.LICENSE,
            Artifact.Attribute.CLASSIFICATION,
            Artifact.Attribute.SECURITY_RELEVANT,
            Artifact.Attribute.SECURITY_CATEGORY,
            Artifact.Attribute.VULNERABILITY,
            Artifact.Attribute.COMMENT,
            Artifact.Attribute.URL,
            Artifact.Attribute.PROJECTS,
            Artifact.Attribute.VERIFIED
    };

    public void writeInventory(Inventory inventory, File file) throws IOException {
        HSSFWorkbook myWorkBook = new HSSFWorkbook();

        writeArtifacts(inventory, myWorkBook);
        writeNotices(inventory, myWorkBook);
        writeComponentPatterns(inventory, myWorkBook);
        writeVulnerabilities(inventory, myWorkBook);
        writeLicenseData(inventory, myWorkBook);

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
        mySheet.setDefaultColumnWidth(20);

        int rowNum = 0;

        // create header row
        HSSFRow headerRow = mySheet.createRow(rowNum++);
        HSSFCellStyle headerStyle = createHeaderStyle(myWorkBook);

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (Artifact artifact : inventory.getArtifacts()) {
            attributes.addAll(artifact.getAttributes());
        }

        List<String> contextColumnList = (List<String>)
                inventory.getContextMap().get("artifact-column-list");
        if (contextColumnList != null) {
            attributes.addAll(contextColumnList);
        }

        // add minimum columns
        attributes.add(Artifact.Attribute.ID.getKey());
        attributes.add(Artifact.Attribute.COMPONENT.getKey());
        attributes.add(Artifact.Attribute.VERSION.getKey());

        // impose context or default order
        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);
        int insertIndex = 0;
        if (contextColumnList != null) {
            for (String key : contextColumnList) {
                insertIndex = reinsert(insertIndex, key, ordered, attributes);
            }
        } else {
            for (Artifact.Attribute a : artifactColumnOrder) {
                String key = a.getKey();
                insertIndex = reinsert(insertIndex, key, ordered, attributes);
            }
        }

        // fill header row
        int cellNum = 0;
        for (String key : ordered) {
            HSSFCell myCell = headerRow.createCell(cellNum++);
            myCell.setCellStyle(headerStyle);
            myCell.setCellValue(new HSSFRichTextString(key));
        }

        // create data rows
        for (Artifact artifact : inventory.getArtifacts()) {
            HSSFRow dataRow = mySheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : ordered) {
                HSSFCell myCell = dataRow.createCell(cellNum++);
                String value = artifact.get(key);
                if (value != null && value.length() > 32760) {
                    // FIXME: log something
                    value = value.substring(0, Math.min(value.length(), 32760));
                    value = value + "...";
                }
                myCell.setCellValue(new HSSFRichTextString(value));
            }
        }

        mySheet.setAutoFilter(new CellRangeAddress(0, 65000, 0, ordered.size() - 1));
    }

    private int reinsert(int insertIndex, String key, List<String> orderedAttributesList, Set<String> attributesSet) {
        if (attributesSet.contains(key)) {
            orderedAttributesList.remove(key);
            orderedAttributesList.add(Math.min(insertIndex, orderedAttributesList.size()), key);
            insertIndex++;
        }
        return insertIndex;
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

    private void writeComponentPatterns(Inventory inventory, HSSFWorkbook myWorkBook) {
        HSSFSheet mySheet = myWorkBook.createSheet("Component Patterns");
        mySheet.createFreezePane(0, 1);
        mySheet.setDefaultColumnWidth(20);

        HSSFRow myRow = null;
        HSSFCell myCell = null;

        int rowNum = 0;

        myRow = mySheet.createRow(rowNum++);

        HSSFCellStyle headerStyle = createHeaderStyle(myWorkBook);

        int cellNum = 0;

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (ComponentPatternData cpd : inventory.getComponentPatternData()) {
            attributes.addAll(cpd.getAttributes());
        }

        attributes.removeAll(ComponentPatternData.CORE_ATTRIBUTES);

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        List<String> finalOrder = new ArrayList<>(ComponentPatternData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        for (String key : finalOrder) {
            myCell = myRow.createCell(cellNum++);
            myCell.setCellStyle(headerStyle);
            myCell.setCellValue(new HSSFRichTextString(key));
        }

        int numCol = cellNum;

        for (ComponentPatternData cpd : inventory.getComponentPatternData()) {
            myRow = mySheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                myCell = myRow.createCell(cellNum++);
                myCell.setCellValue(new HSSFRichTextString(cpd.get(key)));
            }
        }

        mySheet.setAutoFilter(new CellRangeAddress(0, mySheet.getLastRowNum(), 0, numCol - 1));
    }


    private void writeNotices(Inventory inventory, HSSFWorkbook myWorkBook) {
        HSSFSheet mySheet = myWorkBook.createSheet("License Notices");
        mySheet.createFreezePane(0, 1);
        mySheet.setDefaultColumnWidth(20);

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

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            attributes.addAll(licenseMetaData.getAttributes());
        }

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        for (String key : ordered) {
            myCell = myRow.createCell(cellNum++);
            myCell.setCellStyle(headerStyle);
            myCell.setCellValue(new HSSFRichTextString(key));
        }

        int numCol = cellNum;

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

            for (String key : ordered) {
                myCell = myRow.createCell(cellNum++);
                myCell.setCellValue(new HSSFRichTextString(licenseMetaData.get(key)));
            }
        }

        /**
         for (int i = 0; i < 6; i++) {
         Integer width = (Integer) inventory.getContextMap().get("obligations.column[" + i + "].width");
         if (width != null) {
         mySheet.setColumnWidth(i, Math.min(width, 255));
         }
         }
         */

        mySheet.setAutoFilter(new CellRangeAddress(0, 65000, 0, numCol - 1));

    }

    private void writeVulnerabilities(Inventory inventory, HSSFWorkbook myWorkBook) {
        HSSFSheet sheet = myWorkBook.createSheet("Vulnerabilities");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        HSSFRow row = null;
        HSSFCell cell = null;

        int rowNum = 0;

        row = sheet.createRow(rowNum++);

        HSSFCellStyle headerStyle = createHeaderStyle(myWorkBook);

        int cellNum = 0;

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (VulnerabilityMetaData vmd : inventory.getVulnerabilityMetaData()) {
            attributes.addAll(vmd.getAttributes());
        }

        attributes.removeAll(VulnerabilityMetaData.CORE_ATTRIBUTES);

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        List<String> finalOrder = new ArrayList<>(VulnerabilityMetaData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        for (String key : finalOrder) {
            cell = row.createCell(cellNum++);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(new HSSFRichTextString(key));
        }

        int numCol = cellNum;

        for (VulnerabilityMetaData cpd : inventory.getVulnerabilityMetaData()) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                cell = row.createCell(cellNum++);
                String value = cpd.get(key);

                VulnerabilityMetaData.Attribute attribute = VulnerabilityMetaData.Attribute.match(key);
                if (attribute != null) {
                    switch (attribute) {
                        case MAX_SCORE:
                        case V3_SCORE:
                        case V2_SCORE:
                            if (value != null && !value.isEmpty()) {
                                try {
                                    cell.setCellValue(Double.valueOf(value));
                                } catch (NumberFormatException e) {
                                    cell.setCellValue(value);
                                }
                            }
                            break;
                        case URL:
                            cell.setCellValue(new HSSFRichTextString(value));
                            Hyperlink link = myWorkBook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                            link.setAddress(value);
                            cell.setHyperlink(link);
                            break;
                        default:
                            cell.setCellValue(new HSSFRichTextString(value));
                            break;
                    }
                } else {
                    cell.setCellValue(new HSSFRichTextString(value));
                }
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, numCol - 1));
    }

    private void writeLicenseData(Inventory inventory, HSSFWorkbook myWorkBook) {
        HSSFSheet sheet = myWorkBook.createSheet("Licenses");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        HSSFRow row = null;
        HSSFCell cell = null;

        int rowNum = 0;

        row = sheet.createRow(rowNum++);

        HSSFCellStyle headerStyle = createHeaderStyle(myWorkBook);

        int cellNum = 0;

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (LicenseData vmd : inventory.getLicenseData()) {
            attributes.addAll(vmd.getAttributes());
        }

        attributes.removeAll(LicenseData.CORE_ATTRIBUTES);

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        List<String> finalOrder = new ArrayList<>(LicenseData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        for (String key : finalOrder) {
            cell = row.createCell(cellNum++);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(new HSSFRichTextString(key));
        }

        int numCol = cellNum;

        for (LicenseData cpd : inventory.getLicenseData()) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                cell = row.createCell(cellNum++);
                cell.setCellValue(new HSSFRichTextString(cpd.get(key)));
            }
        }
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, numCol - 1));
    }

}
