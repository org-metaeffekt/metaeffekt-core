/*
 * Copyright 2009-2026 the original author or authors.
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

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class XlsxInventoryReader extends AbstractInventoryReader {

    private static final Logger LOG = LoggerFactory.getLogger(XlsInventoryReader.class);

    @Override
    public Inventory readInventory(InputStream in) throws IOException {
        // strange workaround to allow reading large XSLX files.
        org.apache.poi.util.IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);

        // the XLSX reader check the compression ratio; we need to disable this for large XSLX files
        ZipSecureFile.setMinInflateRatio(0);

        final XSSFWorkbook workbook = new XSSFWorkbook(in);

        final Inventory inventory = new Inventory();

        readArtifactMetaData(workbook, inventory);
        readLicenseMetaData(workbook, inventory);
        readLicenseData(workbook, inventory);
        readComponentPatternData(workbook, inventory);
        readVulnerabilityMetaData(workbook, inventory);
        readCertMetaData(workbook, inventory);
        readInventoryInfo(workbook, inventory);
        readAssetMetaData(workbook, inventory);
        readReportData(workbook, inventory);

        applyModificationsForCompatibility(inventory);

        return inventory;
    }

    protected void readArtifactMetaData(XSSFWorkbook workbook, Inventory inventory) {
        XSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_ARTIFACT_DATA);

        // supporting alternative sheet names for backward compatibility
        if (sheet == null) sheet = workbook.getSheet("Artifact Inventory");

        if (sheet == null) return;

        final List<Artifact> artifacts = new ArrayList<>();
        inventory.setArtifacts(artifacts);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final Artifact artifact = super.readRow(row, new Artifact(), pc);
            if (artifact.isValid()) {
                artifacts.add(artifact);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_ARTIFACT_DATA);
    }

    protected void readComponentPatternData(XSSFWorkbook workBook, Inventory inventory) {
        final XSSFSheet sheet = workBook.getSheet(WORKSHEET_NAME_COMPONENT_PATTERN_DATA);
        if (sheet == null) return;

        final List<ComponentPatternData> componentPatternData = new ArrayList<>();
        inventory.setComponentPatternData(componentPatternData);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final ComponentPatternData cpd = super.readRow(row, new ComponentPatternData(), pc);
            if (cpd.isValid()) {
                componentPatternData.add(cpd);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_COMPONENT_PATTERN_DATA);
    }

    protected void readVulnerabilityMetaData(XSSFWorkbook workbook, Inventory inventory) {
        workbook.sheetIterator().forEachRemaining(sheet -> {
            if (sheet.getSheetName().startsWith(WORKSHEET_NAME_VULNERABILITY_DATA) ||
                    sheet.getSheetName().startsWith(InventoryWriter.VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX)) {
                readVulnerabilityMetaData(workbook, inventory, sheet.getSheetName());
            }
        });
    }

    protected void readVulnerabilityMetaData(XSSFWorkbook workbook, Inventory inventory, String sheetName) {
        final XSSFSheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            LOG.warn("Inventory sheet {} not found.", sheetName);
            return;
        }

        final String context = sheetNameToAssessmentContext(sheetName);

        final List<VulnerabilityMetaData> vulnerabilityMetaData = new ArrayList<>();
        inventory.setVulnerabilityMetaData(vulnerabilityMetaData, context);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final VulnerabilityMetaData vmd = super.readRow(row, new VulnerabilityMetaData(), pc);
            if (vmd.isValid()) {
                vulnerabilityMetaData.add(vmd);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_VULNERABILITY_DATA);
    }

    protected void readCertMetaData(XSSFWorkbook workbook, Inventory inventory) {
        XSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_ADVISORY_DATA);

        // for backward compatibility
        if (sheet == null) sheet = workbook.getSheet(WORKSHEET_NAME_ADVISORY_ALTERNATIVE_1_DATA);
        if (sheet == null) sheet = workbook.getSheet(WORKSHEET_NAME_ADVISORY_ALTERNATIVE_2_DATA);

        if (sheet == null) return;

        final List<AdvisoryMetaData> advisoryMetadata = new ArrayList<>();
        inventory.setAdvisoryMetaData(advisoryMetadata);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final AdvisoryMetaData amd = super.readRow(row, new AdvisoryMetaData(), pc);
            if (amd.isValid()) {
                advisoryMetadata.add(amd);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_ADVISORY_DATA);
    }

    protected void readInventoryInfo(XSSFWorkbook workbook, Inventory inventory) {
        final XSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_INVENTORY_INFO);
        if (sheet == null) return;

        final List<InventoryInfo> inventoryInfo = new ArrayList<>();
        inventory.setInventoryInfo(inventoryInfo);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final InventoryInfo info = super.readRow(row, new InventoryInfo(), pc);
            if (info.isValid()) {
                inventoryInfo.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_ADVISORY_DATA);
    }

    protected void readReportData(XSSFWorkbook workbook, Inventory inventory) {
        final XSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_REPORT_DATA);
        if (sheet == null) return;

        final List<ReportData> reportData = new ArrayList<>();
        inventory.setReportData(reportData);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final ReportData info = super.readRow(row, new ReportData(), pc);
            if (info.isValid()) {
                reportData.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_REPORT_DATA);
    }

    protected void readAssetMetaData(XSSFWorkbook workbook, Inventory inventory) {
        final XSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_ASSET_DATA);
        if (sheet == null) return;

        final List<AssetMetaData> assetMetaDataList = new ArrayList<>();
        inventory.setAssetMetaData(assetMetaDataList);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final AssetMetaData amd = super.readRow(row, new AssetMetaData(), pc);
            if (amd.isValid()) {
                assetMetaDataList.add(amd);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_ASSET_DATA);
    }

    protected void readLicenseMetaData(XSSFWorkbook workbook, Inventory inventory) {
        XSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_LICENSE_NOTICES_DATA);

        // supporting alternative sheet names for backward compatibility
        if (sheet == null) sheet = workbook.getSheet("Obligation Notices");
        if (sheet == null) sheet = workbook.getSheet("Component Notices");

        if (sheet == null) return;

        final List<LicenseMetaData> licenseMetaDataList = new ArrayList<>();
        inventory.setLicenseMetaData(licenseMetaDataList);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final LicenseMetaData info = super.readRow(row, new LicenseMetaData(), pc);
            if (info.isValid()) {
                licenseMetaDataList.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_LICENSE_NOTICE_DATA);
    }

    protected void readLicenseData(XSSFWorkbook workbook, Inventory inventory) {

        final XSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_LICENSE_DATA);
        if (sheet == null) return;

        final List<LicenseData> licenseDataList = new ArrayList<>();
        inventory.setLicenseData(licenseDataList);

        final BiConsumer<XSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final LicenseData licenseData = super.readRow(row, new LicenseData(), pc);
            if (licenseData.isValid()) {
                licenseDataList.add(licenseData);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_LICENSE_DATA);
    }

    private void parse(Inventory inventory, XSSFSheet sheet, BiConsumer<XSSFRow, ParsingContext> rowConsumer, String contextKey) {
        final Function<XSSFRow, ParsingContext> headerConsumer = this::parseColumns;

        final Iterator<?> rows = sheet.rowIterator();
        if (rows.hasNext()) {
            // read header row
            final ParsingContext pc = headerConsumer.apply((XSSFRow) rows.next());

            // read content
            while (rows.hasNext()) {
                rowConsumer.accept((XSSFRow) rows.next(), pc);
            }

            // read formatting data
            populateSerializationContextHeaders(inventory, sheet, contextKey, pc);
        }
    }

    protected ParsingContext parseColumns(XSSFRow row) {
        final ParsingContext parsingContainer = new ParsingContext();
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            final XSSFCell cell = row.getCell(i);
            if (cell != null) {
                final String value = cell.getStringCellValue();
                parsingContainer.columnsMap.put(i, value);
                parsingContainer.columns.add(value);
            }
        }
        return parsingContainer;
    }
}
