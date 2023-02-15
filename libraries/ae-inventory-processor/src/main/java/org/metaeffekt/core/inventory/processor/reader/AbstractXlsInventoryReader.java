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
package org.metaeffekt.core.inventory.processor.reader;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.metaeffekt.core.inventory.processor.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractXlsInventoryReader {

    public static final String WORKSHEET_NAME_ARTIFACT_DATA = "Artifacts";
    public static final String WORKSHEET_NAME_INVENTORY_INFO = "Info";
    public static final String WORKSHEET_NAME_REPORT_DATA = "Report";
    public static final String WORKSHEET_NAME_ASSET_DATA = "Assets";
    public static final String WORKSHEET_NAME_COMPONENT_PATTERN_DATA = "Component Patterns";
    public static final String WORKSHEET_NAME_VULNERABILITY_DATA = "Vulnerabilities";
    public static final String WORKSHEET_NAME_LICENSE_NOTICES_DATA = "License Notices";
    public static final String WORKSHEET_NAME_LICENSE_DATA = "Licenses";
    public static final String WORKSHEET_NAME_ADVISORY_DATA = "Advisories";

    public Inventory readInventory(File file) throws IOException {
        final FileInputStream myInput = new FileInputStream(file);
        try {
            return readInventory(myInput);
        } finally {
            myInput.close();
        }
    }

    public Inventory readInventory(InputStream in) throws IOException {
        final POIFSFileSystem fileSystem = new POIFSFileSystem(in);
        final HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);

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

    abstract void applyModificationsForCompatibility(Inventory inventory);

    protected void readArtifactMetaData(HSSFWorkbook workbook, Inventory inventory) {
        // FIXME: move column map into InventorySerializationContext; make implicit
        final Map<Integer, String> columnMap = new HashMap<>();
        final String worksheetName = WORKSHEET_NAME_ARTIFACT_DATA;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_ARTIFACT_DATA;

        HSSFSheet sheet = workbook.getSheet(worksheetName);

        // supporting alternative sheet names for backward compatibility
        if (sheet == null) sheet = workbook.getSheet("Artifact Inventory");

        if (sheet == null) return;

        final List<Artifact> artifacts = new ArrayList<>();
        inventory.setArtifacts(artifacts);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final Artifact artifact = readRow(row, new Artifact(), pc);
            if (artifact.isValid()) {
                artifacts.add(artifact);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }
    protected void readComponentPatternData(HSSFWorkbook workBook, Inventory inventory) {
        final Map<Integer, String> columnMap = new HashMap<>();
        final String worksheetName = WORKSHEET_NAME_COMPONENT_PATTERN_DATA;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_COMPONENT_PATTERN_DATA;

        final HSSFSheet sheet = workBook.getSheet(worksheetName);
        if (sheet == null) return;

        final List<ComponentPatternData> componentPatternData = new ArrayList<>();
        inventory.setComponentPatternData(componentPatternData);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final ComponentPatternData cpd = readRow(row, new ComponentPatternData(), pc);
            if (cpd.isValid()) {
                componentPatternData.add(cpd);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }

    protected void readVulnerabilityMetaData(HSSFWorkbook workbook, Inventory inventory) {
        final String worksheetName = WORKSHEET_NAME_VULNERABILITY_DATA;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_VULNERABILITY_DATA;

        final HSSFSheet sheet = workbook.getSheet(worksheetName);
        if (sheet == null) return;

        final List<VulnerabilityMetaData> vulnerabilityMetaData = new ArrayList<>();
        inventory.setVulnerabilityMetaData(vulnerabilityMetaData);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final VulnerabilityMetaData vmd = readRow(row, new VulnerabilityMetaData(), pc);
            if (vmd.isValid()) {
                vulnerabilityMetaData.add(vmd);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }

    protected void readCertMetaData(HSSFWorkbook workbook, Inventory inventory) {
        final Map<Integer, String> columnMap = new HashMap<>();
        final String worksheetName = WORKSHEET_NAME_ADVISORY_DATA;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_ADVISORY_DATA;

        HSSFSheet sheet = workbook.getSheet(worksheetName);

        // for backward compatibility
        if (sheet == null) sheet = workbook.getSheet("Cert");

        if (sheet == null) return;

        final List<CertMetaData> certMetadata = new ArrayList<>();
        inventory.setCertMetaData(certMetadata);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final CertMetaData cmd = readRow(row, new CertMetaData(), pc);
            if (cmd.isValid()) {
                certMetadata.add(cmd);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }

    protected void readInventoryInfo(HSSFWorkbook workbook, Inventory inventory) {
        final Map<Integer, String> columnMap = new HashMap<>();
        final String worksheetName = WORKSHEET_NAME_INVENTORY_INFO;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_ADVISORY_DATA;

        final HSSFSheet sheet = workbook.getSheet(worksheetName);
        if (sheet == null) return;

        final List<InventoryInfo> inventoryInfo = new ArrayList<>();
        inventory.setInventoryInfo(inventoryInfo);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final InventoryInfo info = readRow(row, new InventoryInfo(), pc);
            if (info.isValid()) {
                inventoryInfo.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }

    protected void readReportData(HSSFWorkbook workbook, Inventory inventory) {
        final Map<Integer, String> columnMap = new HashMap<>();
        final String worksheetName = WORKSHEET_NAME_REPORT_DATA;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_REPORT_DATA;

        final HSSFSheet sheet = workbook.getSheet(worksheetName);
        if (sheet == null) return;

        final List<ReportData> reportData = new ArrayList<>();
        inventory.setReportData(reportData);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final ReportData info = readRow(row, new ReportData(), pc);
            if (info.isValid()) {
                reportData.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }

    protected void readAssetMetaData(HSSFWorkbook workbook, Inventory inventory) {
        final Map<Integer, String> columnMap = new HashMap<>();
        final String worksheetName = WORKSHEET_NAME_ASSET_DATA;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_ASSET_DATA;

        final HSSFSheet sheet = workbook.getSheet(worksheetName);
        if (sheet == null) return;

        final List<AssetMetaData> assetMetaDataList = new ArrayList<>();
        inventory.setAssetMetaData(assetMetaDataList);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final AssetMetaData amd = readRow(row, new AssetMetaData(), pc);
            if (amd.isValid()) {
                assetMetaDataList.add(amd);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }

    protected void readLicenseMetaData(HSSFWorkbook workbook, Inventory inventory) {
        final Map<Integer, String> columnMap = new HashMap<>();
        final String worksheetName = WORKSHEET_NAME_LICENSE_NOTICES_DATA;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_LICENSE_NOTICE_DATA;

        HSSFSheet sheet = workbook.getSheet(worksheetName);

        // supporting alternative sheet names for backward compatibility
        if (sheet == null) sheet = workbook.getSheet("Obligation Notices");
        if (sheet == null) sheet = workbook.getSheet("Component Notices");

        if (sheet == null) return;

        final List<LicenseMetaData> licenseMetaDataList = new ArrayList<>();
        inventory.setLicenseMetaData(licenseMetaDataList);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final LicenseMetaData info = readRow(row, new LicenseMetaData(), pc);
            if (info.isValid()) {
                licenseMetaDataList.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }

    protected void readLicenseData(HSSFWorkbook workbook, Inventory inventory) {
        final Map<Integer, String> columnMap = new HashMap<>();
        final String worksheetName = WORKSHEET_NAME_LICENSE_DATA;
        final String contextKey = InventorySerializationContext.CONTEXT_KEY_LICENSE_DATA;

        final HSSFSheet sheet = workbook.getSheet(worksheetName);
        if (sheet == null) return;

        final List<LicenseData> licenseDataList = new ArrayList<>();
        inventory.setLicenseData(licenseDataList);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final LicenseData licenseData = readRow(row, new LicenseData(), pc);
            if (licenseData.isValid()) {
                licenseDataList.add(licenseData);
            }
        };

        parse(inventory, sheet, rowConsumer, contextKey);
    }

    final Function<HSSFRow, ParsingContext> headerConsumer = row -> parseColumns(row);

    private void parse(Inventory inventory, HSSFSheet sheet, BiConsumer<HSSFRow, ParsingContext> rowConsumer, String contextKey) {

        final Iterator<?> rows = sheet.rowIterator();
        if (rows.hasNext()) {
            // read header row
            final ParsingContext pc = headerConsumer.apply((HSSFRow) rows.next());

            // read content
            while (rows.hasNext()) {
                rowConsumer.accept((HSSFRow) rows.next(), pc);
            }

            // read formatting data
            final List<String> headerList = pc.columns;
            final InventorySerializationContext serializationContext = inventory.getSerializationContext();
            serializationContext.put(contextKey + ".columnlist", headerList);
            for (int i = 0; i < headerList.size(); i++) {
                int width = sheet.getColumnWidth(i);
                serializationContext.put(contextKey + ".column[" + i + "].width", width);
            }
        }
    }

    private static class ParsingContext {

        public ParsingContext() {
            this.columns = new ArrayList<>();
            this.columnsMap = new HashMap<>();
        }

        final List<String> columns;
        final Map<Integer, String> columnsMap;
    }

    protected ParsingContext parseColumns(HSSFRow row) {
        final ParsingContext parsingContainer = new ParsingContext();
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            final HSSFCell cell = row.getCell(i);
            if (cell != null) {
                final String value = cell.getStringCellValue();
                parsingContainer.columnsMap.put(i, value);
                parsingContainer.columns.add(value);
            }
        }
        return parsingContainer;
    }

    protected <T extends AbstractModelBase> T readRow(HSSFRow row, T modelBase, ParsingContext parsingContext) {
        final Map<Integer, String> map = parsingContext.columnsMap;
        for (int i = 0; i < map.size(); i++) {
            final String columnName = map.get(i).trim();
            final HSSFCell cell = row.getCell(i);
            final String value = cell != null ? cell.toString() : null;
            if (value != null) {
                modelBase.set(columnName, value.trim());
            }
        }
        return modelBase;
    }

}
