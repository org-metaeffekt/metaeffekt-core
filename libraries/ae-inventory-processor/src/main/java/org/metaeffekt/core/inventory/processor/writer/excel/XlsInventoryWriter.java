/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.writer.excel;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.reader.AbstractInventoryReader;
import org.metaeffekt.core.inventory.processor.writer.excel.style.InventorySheetCellStyler;
import org.metaeffekt.core.inventory.processor.writer.excel.style.XlsHSSFInventorySheetCellStylers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class XlsInventoryWriter extends AbstractXlsInventoryWriter {

    public void writeInventory(Inventory inventory, File file) throws IOException {
        final HSSFWorkbook workbook = new HSSFWorkbook();
        final XlsHSSFInventorySheetCellStylers stylers = new XlsHSSFInventorySheetCellStylers(workbook);

        writeArtifacts(inventory, workbook, stylers);
        writeAssetMetaData(inventory, workbook, stylers);
        writeNotices(inventory, workbook, stylers);

        writeComponentPatterns(inventory, workbook, stylers);
        writeLicenseData(inventory, workbook, stylers);
        writeReportData(inventory, workbook, stylers);

        writeVulnerabilities(inventory, workbook, stylers);
        writeAdvisoryMetaData(inventory, workbook, stylers);

        writeInventoryInfo(inventory, workbook, stylers);

        final FileOutputStream out = new FileOutputStream(file);
        try {
            workbook.write(out);
        } finally {
            out.flush();
            out.close();
        }
    }

    private void writeArtifacts(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        // the artifact sheet is only written, if:
        // - there are artifacts
        // - there is no other information in the inventory
        //   this has to be done, since a xls without sheets is regarded damaged by Excel
        if (inventory.hasInformationOtherThanArtifacts() && inventory.getArtifacts().isEmpty()) {
            return;
        }

        final HSSFSheet sheet = createAMDSheet(workbook, AbstractInventoryReader.WORKSHEET_NAME_ARTIFACT_DATA);

        final HSSFRow headerRow = sheet.createRow(0);

        // create columns for key / value map content
        final List<String> ordered = InventorySerializationContext.initializeArtifactSerializationContext(inventory);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[]{
                stylers.headerStyleColumnNameAssetId,
                stylers.headerStyleColumnNameIncompleteMatch,
                stylers.headerStyleColumnNameErrors,
                stylers.createArtifactHeaderCellStyler(inventory.getSerializationContext()),
                stylers.headerStyleBinaryArtifact,
                stylers.headerStyleSourceArtifact,
                stylers.headerStyleSourceArchive,
                stylers.headerStyleDescriptor,
                stylers.headerStyleDefault,
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[]{
                stylers.contentStyleColumnNameAssetId,
                stylers.createArtifactsCellStyler(inventory.getSerializationContext()),
                stylers.contentStyleColumnNameIncompleteMatch,
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getArtifacts(), ordered,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, 65000, 0, columnCount - 1));
    }

    private void writeComponentPatterns(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        if (isEmpty(inventory.getComponentPatternData())) return;

        final HSSFSheet sheet = createAMDSheet(workbook, "Component Patterns");

        final HSSFRow headerRow = sheet.createRow(0);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (ComponentPatternData cpd : inventory.getComponentPatternData()) {
            attributes.addAll(cpd.getAttributes());
        }

        ComponentPatternData.CORE_ATTRIBUTES.forEach(attributes::remove);

        final List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        final List<String> finalOrder = new ArrayList<>(ComponentPatternData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[] {
                stylers.headerStyleColumnNameAssetId,
                stylers.headerStyleDefault
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[] {
                stylers.contentStyleColumnNameAssetId
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getComponentPatternData(), finalOrder,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columnCount - 1));
    }


    private void writeNotices(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        if (isEmpty(inventory.getLicenseMetaData())) return;

        final HSSFSheet sheet = createAMDSheet(workbook, "License Notices");

        final HSSFRow headerRow = sheet.createRow(0);

        // create columns for key / value map content
        final Set<String> attributes = new LinkedHashSet<>(LicenseMetaData.CORE_ATTRIBUTES);
        final Set<String> orderedOtherAttributes = new TreeSet<>();
        for (LicenseMetaData licenseMetaData : inventory.getLicenseMetaData()) {
            orderedOtherAttributes.addAll(licenseMetaData.getAttributes());
        }
        attributes.addAll(orderedOtherAttributes);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[]{
                stylers.headerStyleDefault
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[]{
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getLicenseMetaData(), attributes,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, 65000, 0, columnCount - 1));

    }

    private void writeVulnerabilities(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        inventory.getVulnerabilityMetaDataContexts().stream()
                .filter(StringUtils::isNotEmpty)
                .forEach(context -> writeVulnerabilities(inventory, workbook, context, stylers));
    }

    private void writeVulnerabilities(Inventory inventory, HSSFWorkbook workbook, String assessmentContext, XlsHSSFInventorySheetCellStylers stylers) {
        if (isEmpty(inventory.getVulnerabilityMetaData(assessmentContext))) return;

        final HSSFSheet sheet = createAMDSheet(workbook, assessmentContextToSheetName(assessmentContext));

        final HSSFRow headerRow = sheet.createRow(0);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (VulnerabilityMetaData vmd : inventory.getVulnerabilityMetaData(assessmentContext)) {
            attributes.addAll(vmd.getAttributes());
        }

        VulnerabilityMetaData.CORE_ATTRIBUTES.forEach(attributes::remove);

        final List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        final List<String> finalOrder = new ArrayList<>(VulnerabilityMetaData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[]{
                stylers.headerStyleDefault,
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[]{
                stylers.contentStyleCvssScoresDoubleValue,
                stylers.contentStyleUrlValue,
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getVulnerabilityMetaData(assessmentContext), finalOrder,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columnCount - 1));
    }

    private void writeAdvisoryMetaData(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        if (isEmpty(inventory.getAdvisoryMetaData())) return;

        final HSSFSheet sheet = createAMDSheet(workbook, AbstractInventoryReader.WORKSHEET_NAME_ADVISORY_DATA);

        final HSSFRow headerRow = sheet.createRow(0);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (AdvisoryMetaData am : inventory.getAdvisoryMetaData()) {
            attributes.addAll(am.getAttributes());
        }

        AdvisoryMetaData.CORE_ATTRIBUTES.forEach(attributes::remove);

        final List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        final List<String> finalOrder = new ArrayList<>(AdvisoryMetaData.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[]{
                stylers.headerStyleDefault,
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[]{
                stylers.contentStyleUrlValue,
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getAdvisoryMetaData(), finalOrder,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columnCount - 1));
    }

    private void writeInventoryInfo(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        if (isEmpty(inventory.getInventoryInfo())) return;

        final HSSFSheet sheet = createAMDSheet(workbook, "Info");

        final HSSFRow headerRow = sheet.createRow(0);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (InventoryInfo info : inventory.getInventoryInfo()) {
            attributes.addAll(info.getAttributes());
        }

        InventoryInfo.CORE_ATTRIBUTES.forEach(attributes::remove);

        final List<String> ordered = new ArrayList<>(attributes);
        Collections.sort(ordered);

        final List<String> finalOrder = new ArrayList<>(InventoryInfo.CORE_ATTRIBUTES);
        finalOrder.addAll(ordered);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[]{
                stylers.headerStyleDefault,
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[]{
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getInventoryInfo(), finalOrder,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columnCount - 1));
    }

    private void writeLicenseData(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        if (isEmpty(inventory.getLicenseData())) return;

        final HSSFSheet sheet = createAMDSheet(workbook, "Licenses");

        final HSSFRow headerRow = sheet.createRow(0);

        final InventorySerializationContext serializationContext = inventory.getSerializationContext();

        final List<String> ordered = InventorySerializationContext.initializeLicenseDataSerializationContext(inventory);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[]{
                stylers.headerStyleColumnNameAssetId,
                stylers.headerStyleColumnNameMarker,
                stylers.headerStyleColumnNameClassification,
                stylers.createLicensesHeaderCellStyler(serializationContext),
                stylers.headerStyleDefault,
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[]{
                stylers.contentStyleColumnNameAssetId,
                stylers.contentStyleColumnNameMarkerCentered,
                stylers.createLicensesCellStyler(serializationContext)
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getLicenseData(), ordered,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columnCount - 1));
    }

    private void writeAssetMetaData(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        if (isEmpty(inventory.getAssetMetaData())) return;

        final HSSFSheet sheet = createAMDSheet(workbook, "Assets");

        final HSSFRow headerRow = sheet.createRow(0);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (AssetMetaData amd : inventory.getAssetMetaData()) {
            attributes.addAll(amd.getAttributes());
        }

        // remove core attributes
        final List<String> finalOrder = deriveOrder(attributes, AssetMetaData.CORE_ATTRIBUTES);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[] {
                stylers.headerStyleColumnNameAssetId,
                stylers.headerStyleColumnNameSrcAssetSource,
                stylers.headerStyleColumnNameAssetConfig,
                stylers.headerStyleDefault,
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[] {
                stylers.contentStyleColumnNameSrcCentered,
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getAssetMetaData(), finalOrder,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columnCount - 1));
    }

    private void writeReportData(Inventory inventory, HSSFWorkbook workbook, XlsHSSFInventorySheetCellStylers stylers) {
        if (isEmpty(inventory.getReportData())) return;

        final HSSFSheet sheet = createAMDSheet(workbook, "Report");

        final HSSFRow headerRow = sheet.createRow(0);

        // create columns for key / value map content
        final Set<String> attributes = new HashSet<>();
        for (ReportData rd : inventory.getReportData()) {
            attributes.addAll(rd.getAttributes());
        }

        // remove core attributes
        final List<String> finalOrder = deriveOrder(attributes, ReportData.CORE_ATTRIBUTES);

        final InventorySheetCellStyler[] headerCellStylers = new InventorySheetCellStyler[]{
                stylers.headerStyleColumnNameAssetId,
                stylers.headerStyleDefault,
        };

        final InventorySheetCellStyler[] dataCellStylers = new InventorySheetCellStyler[]{
                stylers.contentStyleColumnNameAssetId,
        };

        final int columnCount = super.populateSheetWithModelData(
                inventory.getReportData(), finalOrder,
                headerRow::createCell, sheet::createRow,
                headerCellStylers, dataCellStylers);

        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, columnCount - 1));
    }

    private HSSFSheet createAMDSheet(HSSFWorkbook workbook, String sheetname) {
        final HSSFSheet sheet = workbook.createSheet(sheetname);
        sheet.createFreezePane(0, 1);
        sheet.setDefaultColumnWidth(20);
        return sheet;
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
