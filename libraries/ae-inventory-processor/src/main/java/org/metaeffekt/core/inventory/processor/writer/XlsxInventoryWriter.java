/*
 * Copyright 2009-2022 the original author or authors.
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
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.model.CertMetaData.Attribute;
import org.metaeffekt.core.inventory.processor.reader.AbstractInventoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class XlsxInventoryWriter extends AbstractXlsxInventoryWriter {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * Defines a default order.
     * <p>
     * FIXME: column-metadata
     */
    private final Artifact.Attribute[] artifactColumnOrder = new Artifact.Attribute[]{
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
        final XSSFWorkbook workbook = new XSSFWorkbook();

        writeArtifacts(inventory, workbook);
        writeNotices(inventory, workbook);
        writeComponentPatterns(inventory, workbook);
        writeVulnerabilities(inventory, workbook);
        writeAdvisoryMetaData(inventory, workbook);
        writeInventoryInfo(inventory, workbook);
        writeLicenseData(inventory, workbook);
        writeAssetMetaData(inventory, workbook);
        writeReportData(inventory, workbook);

        final FileOutputStream out = new FileOutputStream(file);
        try {
            workbook.write(out);
        } finally {
            out.flush();
            out.close();
        }
    }

    private void writeArtifacts(Inventory inventory, XSSFWorkbook workbook) {
        // an artifact inventory is always written (an xls without sheets is regarded damaged by Excel)

        final XSSFSheet sheet = workbook.createSheet(AbstractInventoryReader.WORKSHEET_NAME_ARTIFACT_DATA);
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        int rowNum = 0;

        // create header row
        final XSSFRow headerRow = sheet.createRow(rowNum++);
        final XSSFCellStyle headerStyle = createHeaderStyle(workbook);
        final XSSFCellStyle assetHeaderStyle = createAssetHeaderStyle(workbook);
        final XSSFCellStyle warnHeaderStyle = createWarnHeaderStyle(workbook);
        final XSSFCellStyle errorHeaderStyle = createErrorHeaderStyle(workbook);
        final XSSFCellStyle centeredCellStyle = createCenteredStyle(workbook);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (final Artifact artifact : inventory.getArtifacts()) {
            attributes.addAll(artifact.getAttributes());
        }

        final List<String> contextColumnList = inventory.getSerializationContext().
                get(InventorySerializationContext.CONTEXT_ARTIFACT_COLUMN_LIST);
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
            XSSFCell cell = headerRow.createCell(cellNum);
            // FIXME: resolve current workaround
            if (isAssetId(key)) {
                cell.setCellStyle(assetHeaderStyle);
                // number of pixel times magic number
                sheet.setColumnWidth(cellNum, 20 * 42);
                headerRow.setHeight((short) (170 * 20));
            } else if (key.equalsIgnoreCase("Incomplete Match")) {
                cell.setCellStyle(warnHeaderStyle);
                sheet.setColumnWidth(cellNum, 20 * 42);
                headerRow.setHeight((short) (170 * 20));
            } else if (key.equalsIgnoreCase("Errors")) {
                cell.setCellStyle(errorHeaderStyle);
            } else {
                cell.setCellStyle(headerStyle);
            }
            cell.setCellValue(key);

            cellNum++;
        }

        // create data rows
        for (Artifact artifact : inventory.getArtifacts()) {
            XSSFRow dataRow = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : ordered) {
                XSSFCell cell = dataRow.createCell(cellNum);
                String value = artifact.get(key);
                if (value != null && value.length() > MAX_CELL_LENGTH) {
                    LOG.warn("Cell content [{}] is longer than the anticipated max cell length of [{}] and will " +
                            "potentially cropped", key, MAX_CELL_LENGTH);
                    value = value.substring(0, MAX_CELL_LENGTH);
                    value = value + "...";
                }
                cell.setCellValue(value);

                if (isAssetId(key)) {
                    cell.setCellStyle(centeredCellStyle);
                } else if (key.equalsIgnoreCase("Incomplete Match")) {
                    cell.setCellStyle(centeredCellStyle);
                }

                cellNum++;
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, 65000, 0, ordered.size() - 1));
    }

    private static boolean isAssetId(String key) {
        return key.startsWith("CID-") || key.startsWith("AID-") || key.startsWith("EID-") || key.startsWith("IID-");
    }

    private void writeComponentPatterns(Inventory inventory, XSSFWorkbook workbook) {
        if (isEmpty(inventory.getComponentPatternData())) return;

        XSSFSheet sheet = workbook.createSheet("Component Patterns");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        XSSFCellStyle headerStyle = createHeaderStyle(workbook);

        int rowNum = 0;

        XSSFRow row = sheet.createRow(rowNum++);

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (ComponentPatternData cpd : inventory.getComponentPatternData()) {
            attributes.addAll(cpd.getAttributes());
        }

        ComponentPatternData.CORE_ATTRIBUTES.forEach(attributes::remove);

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        List<String> finalOrder = new ArrayList<>(ComponentPatternData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        int cellNum = 0;
        for (String key : finalOrder) {
            XSSFCell cell = row.createCell(cellNum++);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(new XSSFRichTextString(key));
        }
        int numCol = cellNum;

        for (ComponentPatternData cpd : inventory.getComponentPatternData()) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                XSSFCell cell = row.createCell(cellNum++);
                cell.setCellValue(new XSSFRichTextString(cpd.get(key)));
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, numCol - 1));
    }


    private void writeNotices(Inventory inventory, XSSFWorkbook workbook) {
        if (isEmpty(inventory.getLicenseMetaData())) return;

        XSSFSheet sheet = workbook.createSheet("License Notices");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        int rowNum = 0;

        XSSFRow row = sheet.createRow(rowNum++);

        XSSFCellStyle headerStyle = createHeaderStyle(workbook);

        int cellNum = 0;

        // create columns for key / value map content
        Set<String> attributes = new LinkedHashSet<>(LicenseMetaData.CORE_ATTRIBUTES);
        Set<String> orderedOtherAttributes = new TreeSet<>();
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            orderedOtherAttributes.addAll(licenseMetaData.getAttributes());
        }
        attributes.addAll(orderedOtherAttributes);

        for (String key : attributes) {
            XSSFCell cell = row.createCell(cellNum++);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(key);
        }

        int numCol = cellNum;

        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : attributes) {
                XSSFCell cell = row.createCell(cellNum++);
                cell.setCellValue(licenseMetaData.get(key));
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, 65000, 0, numCol - 1));

    }

    private void writeVulnerabilities(Inventory inventory, XSSFWorkbook workbook) {
        inventory.getVulnerabilityMetaDataContexts().stream()
                .filter(StringUtils::isNotEmpty)
                .forEach(context -> writeVulnerabilities(inventory, workbook, context));
    }

    private void writeVulnerabilities(Inventory inventory, XSSFWorkbook workbook, String context) {
        if (isEmpty(inventory.getVulnerabilityMetaData(context))) return;

        final XSSFSheet sheet = workbook.createSheet(VulnerabilityMetaData.contextToSheetName(context));
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        int rowNum = 0;

        XSSFRow row = sheet.createRow(rowNum++);

        XSSFCellStyle headerStyle = createHeaderStyle(workbook);

        int cellNum = 0;

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (VulnerabilityMetaData vmd : inventory.getVulnerabilityMetaData(context)) {
            attributes.addAll(vmd.getAttributes());
        }

        VulnerabilityMetaData.CORE_ATTRIBUTES.forEach(attributes::remove);

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        List<String> finalOrder = new ArrayList<>(VulnerabilityMetaData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        for (String key : finalOrder) {
            XSSFCell cell = row.createCell(cellNum++);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(new XSSFRichTextString(key));
        }

        int numCol = cellNum;

        for (VulnerabilityMetaData vmd : inventory.getVulnerabilityMetaData(context)) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                final XSSFCell cell = row.createCell(cellNum++);
                final String value = vmd.get(key);

                final VulnerabilityMetaData.Attribute attribute = VulnerabilityMetaData.Attribute.match(key);
                if (attribute != null) {
                    switch (attribute) {
                        case MAX_SCORE:
                        case V3_SCORE:
                        case V2_SCORE:
                            if (value != null && !value.isEmpty()) {
                                try {
                                    cell.setCellValue(Double.parseDouble(value));
                                } catch (NumberFormatException e) {
                                    cell.setCellValue(value);
                                }
                            }
                            break;
                        case URL:
                            cell.setCellValue(new XSSFRichTextString(value));
                            if (StringUtils.isNotBlank(value)) {
                                Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                                link.setAddress(value);
                                cell.setHyperlink(link);
                            }
                            break;
                        default:
                            cell.setCellValue(new XSSFRichTextString(value));
                            break;
                    }
                } else {
                    cell.setCellValue(new XSSFRichTextString(value));
                }
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, numCol - 1));
    }

    private void writeAdvisoryMetaData(Inventory inventory, XSSFWorkbook workbook) {
        if (isEmpty(inventory.getCertMetaData())) return;

        XSSFSheet sheet = workbook.createSheet("Cert");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        int rowNum = 0;

        XSSFRow row = sheet.createRow(rowNum++);
        XSSFCellStyle headerStyle = createHeaderStyle(workbook);

        int cellNum = 0;

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (CertMetaData cm : inventory.getCertMetaData()) {
            attributes.addAll(cm.getAttributes());
        }

        CertMetaData.CORE_ATTRIBUTES.forEach(attributes::remove);

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        List<String> finalOrder = new ArrayList<>(CertMetaData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        for (String key : finalOrder) {
            final XSSFCell cell = row.createCell(cellNum++);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(new XSSFRichTextString(key));
        }

        int numCol = cellNum;

        for (CertMetaData cm : inventory.getCertMetaData()) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                XSSFCell cell = row.createCell(cellNum++);
                String value = cm.get(key);

                Attribute attribute = Attribute.match(key);
                if (attribute != null) {
                    switch (attribute) {
                        case URL:
                            cell.setCellValue(new XSSFRichTextString(value));
                            if (StringUtils.isNotEmpty(value)) {
                                Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                                link.setAddress(value);
                                cell.setHyperlink(link);
                            }
                            break;
                        default:
                            cell.setCellValue(new XSSFRichTextString(value));
                            break;
                    }
                } else {
                    cell.setCellValue(new XSSFRichTextString(value));
                }
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, numCol - 1));
    }

    private void writeInventoryInfo(Inventory inventory, XSSFWorkbook workbook) {
        if (isEmpty(inventory.getInventoryInfo())) return;

        XSSFSheet sheet = workbook.createSheet("Info");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        int rowNum = 0;
        int cellNum = 0;

        XSSFRow row = sheet.createRow(rowNum++);

        XSSFCellStyle headerStyle = createHeaderStyle(workbook);

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (InventoryInfo info : inventory.getInventoryInfo()) {
            attributes.addAll(info.getAttributes());
        }

        InventoryInfo.CORE_ATTRIBUTES.forEach(attributes::remove);

        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        List<String> finalOrder = new ArrayList<>(InventoryInfo.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        for (String key : finalOrder) {
            final XSSFCell cell = row.createCell(cellNum++);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(new XSSFRichTextString(key));
        }

        int numCol = cellNum;

        for (InventoryInfo info : inventory.getInventoryInfo()) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                XSSFCell cell = row.createCell(cellNum++);
                String value = info.get(key);

                cell.setCellValue(value);
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, numCol - 1));
    }

    private void writeLicenseData(Inventory inventory, XSSFWorkbook workbook) {
        if (isEmpty(inventory.getLicenseData())) return;

        XSSFSheet sheet = workbook.createSheet("Licenses");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        int rowNum = 0;

        XSSFRow row = sheet.createRow(rowNum++);

        final XSSFCellStyle headerStyle = createHeaderStyle(workbook);
        final XSSFCellStyle assetHeaderStyle = createAssetHeaderStyle(workbook);
        final XSSFCellStyle centeredCellStyle = createCenteredStyle(workbook);

        int cellNum = 0;

        // create columns for key / value map content
        Set<String> attributes = new HashSet<>();
        for (LicenseData vmd : inventory.getLicenseData()) {
            attributes.addAll(vmd.getAttributes());
        }

        final InventorySerializationContext serializationContext = inventory.getSerializationContext();
        final List<String> contextColumnList = serializationContext.
                get(InventorySerializationContext.CONTEXT_LICENSEDATA_COLUMN_LIST);
        if (contextColumnList != null) {
            attributes.addAll(contextColumnList);
        }

        // add minimum columns
        attributes.addAll(LicenseData.CORE_ATTRIBUTES);

        // impose context or default order
        List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);
        int insertIndex = 0;
        if (contextColumnList != null) {
            for (String key : contextColumnList) {
                insertIndex = reinsert(insertIndex, key, ordered, attributes);
            }
        } else {
            for (String key : LicenseData.CORE_ATTRIBUTES) {
                insertIndex = reinsert(insertIndex, key, ordered, attributes);
            }
        }

        for (String key : ordered) {
            final XSSFCell cell = row.createCell(cellNum);
            if (isAssetId(key)) {
                cell.setCellStyle(assetHeaderStyle);
                // number of pixel times magic number
                sheet.setColumnWidth(cellNum, 20 * 42);
                row.setHeight((short) (170 * 20));
            } else {
                final String customHeaderColor = serializationContext.get("licensedata.header.[" + key + "].fg");
                if (customHeaderColor != null) {
                    XSSFCellStyle customHeaderStyle = createHeaderStyle(workbook);
                    XSSFColor headerColor = resolveColor(workbook, customHeaderColor);
                    customHeaderStyle.setFillForegroundColor(headerColor);

                    final String customHeaderOrientation = serializationContext.get("licensedata.header.[" + key + "].orientation");
                    if ("up".equalsIgnoreCase(customHeaderOrientation)) {
                        customHeaderStyle.setRotation((short) -90);
                        sheet.setColumnWidth(cellNum, 20 * 42);
                        row.setHeight((short) (170 * 20));
                        customHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
                        customHeaderStyle.setVerticalAlignment(VerticalAlignment.TOP);
                    }

                    final Integer customHeaderWidth = serializationContext.get("licensedata.header.[" + key + "].width");
                    if (customHeaderWidth != null) {
                        sheet.setColumnWidth(cellNum, customHeaderWidth * 42);
                    }

                    cell.setCellStyle(customHeaderStyle);
                } else {
                    cell.setCellStyle(headerStyle);
                }
            }
            cell.setCellValue(new XSSFRichTextString(key));

            cellNum++;
        }

        int numCol = cellNum;

        for (LicenseData cpd : inventory.getLicenseData()) {
            row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : ordered) {
                final XSSFCell cell = row.createCell(cellNum++);
                cell.setCellValue(new XSSFRichTextString(cpd.get(key)));

                if (isAssetId(key)) {
                    cell.setCellStyle(centeredCellStyle);
                }

                final Boolean centered = serializationContext.get("licensedata.column.[" + key + "].centered");
                if (centered != null && centered) {
                    cell.setCellStyle(centeredCellStyle);
                }
            }
        }
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, numCol - 1));
    }


    private void writeAssetMetaData(Inventory inventory, XSSFWorkbook workbook) {
        if (isEmpty(inventory.getAssetMetaData())) return;

        final XSSFSheet sheet = workbook.createSheet("Assets");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        int rowNum = 0;

        final XSSFRow headerRow = sheet.createRow(rowNum++);

        final XSSFCellStyle headerStyle = createHeaderStyle(workbook);
        final XSSFCellStyle assetSourceHeaderStyle = createAssetSourceHeaderStyle(workbook);
        final XSSFCellStyle assetConfigHeaderStyle = createAssetConfigHeaderStyle(workbook);
        final XSSFCellStyle centeredCellStyle = createCenteredStyle(workbook);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (AssetMetaData amd : inventory.getAssetMetaData()) {
            attributes.addAll(amd.getAttributes());
        }

        // remove core attributes
        final List<String> finalOrder = deriveOrder(attributes, AssetMetaData.CORE_ATTRIBUTES);

        int cellNum = 0;
        for (final String key : finalOrder) {
            final XSSFCell cell = headerRow.createCell(cellNum);
            if (key.startsWith("SRC-")) {
                cell.setCellStyle(assetSourceHeaderStyle);
                // number of pixel times magic number
                sheet.setColumnWidth(cellNum, 20 * 42);
                headerRow.setHeight((short) (170 * 20));
            } else if (key.startsWith("config_")) {
                cell.setCellStyle(assetConfigHeaderStyle);
                // number of pixel times magic number
                sheet.setColumnWidth(cellNum, 40 * 42);
            } else {
                cell.setCellStyle(headerStyle);
            }
            cell.setCellValue(new XSSFRichTextString(key));

            cellNum++;
        }

        for (AssetMetaData cpd : inventory.getAssetMetaData()) {
            XSSFRow row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                final XSSFCell cell = row.createCell(cellNum);
                cell.setCellValue(new XSSFRichTextString(cpd.get(key)));

                if (key.startsWith("SRC-")) {
                    cell.setCellStyle(centeredCellStyle);
                }
                cellNum++;
            }
        }
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, finalOrder.size() - 1));
    }

    private void writeReportData(Inventory inventory, XSSFWorkbook workbook) {
        if (isEmpty(inventory.getReportData())) return;

        final XSSFSheet sheet = workbook.createSheet("Report");
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);

        int rowNum = 0;

        final XSSFRow headerRow = sheet.createRow(rowNum++);

        final XSSFCellStyle headerStyle = createHeaderStyle(workbook);
        final XSSFCellStyle assetHeaderStyle = createAssetHeaderStyle(workbook);
        final XSSFCellStyle centeredCellStyle = createCenteredStyle(workbook);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (ReportData rd : inventory.getReportData()) {
            attributes.addAll(rd.getAttributes());
        }

        // remove core attributes
        final List<String> finalOrder = deriveOrder(attributes, ReportData.CORE_ATTRIBUTES);

        int cellNum = 0;
        for (final String key : finalOrder) {
            final XSSFCell cell = headerRow.createCell(cellNum);
            if (isAssetId(key)) {
                cell.setCellStyle(assetHeaderStyle);
                // number of pixel times magic number
                sheet.setColumnWidth(cellNum, 20 * 42);
                headerRow.setHeight((short) (170 * 20));
            } else {
                cell.setCellStyle(headerStyle);
            }
            cell.setCellValue(new XSSFRichTextString(key));

            cellNum++;
        }

        for (ReportData rd : inventory.getReportData()) {
            final XSSFRow row = sheet.createRow(rowNum++);
            cellNum = 0;
            for (String key : finalOrder) {
                final XSSFCell cell = row.createCell(cellNum);
                cell.setCellValue(new XSSFRichTextString(rd.get(key)));

                if (isAssetId(key)) {
                    cell.setCellStyle(centeredCellStyle);
                }
                cellNum++;
            }
        }
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, finalOrder.size() - 1));
    }

    private static List<String> deriveOrder(Set<String> attributes, ArrayList<String> coreAttributes) {
        coreAttributes.forEach(attributes::remove);

        // produce ordered attribute list
        final List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        // compose core attributes and the ordered (remaining) list
        final List<String> finalOrder = new ArrayList<>(coreAttributes);
        finalOrder.addAll(ordered);
        return finalOrder;
    }

}
