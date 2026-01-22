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

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaInventoryAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.InventorySerializationContext.*;
import static org.metaeffekt.core.inventory.processor.writer.InventoryWriter.VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX;

public abstract class AbstractInventoryReader {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractInventoryReader.class);

    public static final String WORKSHEET_NAME_ARTIFACT_DATA = "Artifacts";
    public static final String WORKSHEET_NAME_INVENTORY_INFO = "Info";
    public static final String WORKSHEET_NAME_REPORT_DATA = "Report";
    public static final String WORKSHEET_NAME_ASSET_DATA = "Assets";
    public static final String WORKSHEET_NAME_COMPONENT_PATTERN_DATA = "Component Patterns";
    public static final String WORKSHEET_NAME_VULNERABILITY_DATA = "Vulnerabilities";
    public static final String WORKSHEET_NAME_LICENSE_NOTICES_DATA = "License Notices";
    public static final String WORKSHEET_NAME_LICENSE_DATA = "Licenses";
    public static final String WORKSHEET_NAME_ADVISORY_DATA = "Security Advisories";
    public static final String WORKSHEET_NAME_ADVISORY_ALTERNATIVE_1_DATA = "Advisories";
    public static final String WORKSHEET_NAME_ADVISORY_ALTERNATIVE_2_DATA = "Cert";

    public static final String WORKSHEET_NAME_THREAT_DATA = "Threats";
    public static final String WORKSHEET_NAME_WEAKNESS_DATA = "Weaknesses";
    public static final String WORKSHEET_NAME_ATTACK_PATTERN_DATA = "Attack Patterns";

    private final DataFormatter baseCellDataFormatter = new DataFormatter();
    private final DecimalFormat numericCellDataFormatter = new DecimalFormat();

    {
        // use US locale to ensure dot as decimal separator
        numericCellDataFormatter.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        numericCellDataFormatter.setGroupingUsed(false);
    }

    private static final Pattern SPLIT_COLUMN_PATTERN = Pattern.compile("(.*) \\(split-\\d+\\)");

    public Inventory readInventory(File file) throws IOException {
        final FileInputStream myInput = new FileInputStream(file);
        try {
            return readInventory(myInput);
        } catch (RuntimeException e) {
            throw new IllegalStateException(String.format("Cannot read inventory file [%s].", file.getAbsoluteFile()), e);
        } finally {
            myInput.close();
        }
    }

    public abstract Inventory readInventory(InputStream in) throws IOException;

    private void updateArtifacts(Inventory inventory, InventorySerializationContext serializationContext) {
        final List<String> attributeList = serializationContext.get(CONTEXT_ARTIFACT_DATA_COLUMN_LIST);
        final List<Artifact> artifacts = inventory.getArtifacts();

        // apply modification operations
        resolveRename(artifacts, attributeList, "Component / Group", Artifact.Attribute.COMPONENT.getKey());
    }

    private void updateLicenseMetaData(Inventory inventory, InventorySerializationContext serializationContext) {
        final List<String> attributeList = serializationContext.get(CONTEXT_LICENSE_DATA_COLUMN_LIST);
        final List<LicenseMetaData> lmdList = inventory.getLicenseMetaData();

        // apply modification operations
        resolveRename(lmdList, attributeList, "Text", LicenseMetaData.Attribute.NOTICE.getKey());
    }

    private void updateAssetMetaData(Inventory inventory, InventorySerializationContext serializationContext) {
        final List<String> attributeList = serializationContext.get(CONTEXT_ASSET_DATA_COLUMN_LIST);
        final List<AssetMetaData> lmdList = inventory.getAssetMetaData();

        // apply modification operations
        resolveRename(lmdList, attributeList, "Role", AssetMetaData.Attribute.AUDIENCE.getKey());
    }

    private void updateVulnerabilityMetaData(Inventory inventory, InventorySerializationContext serializationContext) {
        final List<String> attributeList = serializationContext.get(CONTEXT_VULNERABILITY_DATA_COLUMN_LIST);
        for (String assessmentContext : inventory.getVulnerabilityMetaDataContexts()) {
            final List<VulnerabilityMetaData> vmdList = inventory.getVulnerabilityMetaData(assessmentContext);

            mergeContent(vmdList, attributeList, "Referenced Content IDs", AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS);
            mergeContent(vmdList, attributeList, "Referenced Content Ids", AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS);

            resolveRename(vmdList, attributeList, "Url", VulnerabilityMetaData.Attribute.URL.getKey());
        }
    }

    private void updateAdvisoryMetaData(Inventory inventory, InventorySerializationContext serializationContext) {
        final List<String> attributeList = serializationContext.get(CONTEXT_ADVISORY_DATA_COLUMN_LIST);
        final List<AdvisoryMetaData> list = inventory.getAdvisoryMetaData();

        // apply modification operations
        resolveRename(list, attributeList, "Url", AdvisoryMetaData.Attribute.URL.getKey());
    }

    private void mergeContent(List<? extends AbstractModelBase> objects, List<String> attributeList, String oldKey, AbstractModelBase.Attribute attribute) {
        objects.forEach(a -> mapContent(a, oldKey, attribute));
        if (attributeList != null) {
            if (!attributeList.contains(attribute.getKey())) {
                int i = attributeList.indexOf(oldKey);
                if (i != -1) {
                    attributeList.set(i, attribute.getKey());
                }
            }

            attributeList.remove(oldKey);
        }
    }

    protected void update(VulnerabilityMetaData vulnerabilityMetaData) {
        // compensate rename of attributes
        mapContent(vulnerabilityMetaData, "Referenced Content IDs", AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS);
        mapContent(vulnerabilityMetaData, "Referenced Content Ids", AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS);

        resolveRename(vulnerabilityMetaData, "Url", VulnerabilityMetaData.Attribute.URL.getKey());
    }

    protected void resolveRename(List<? extends AbstractModelBase> objects, List<String> attributeList, String oldKey, String newKey) {
        objects.forEach(a -> resolveRename(a, oldKey, newKey));

        // replace the name in the attribute list
        if (attributeList != null) {
            int i = attributeList.indexOf(oldKey);
            if (i != -1) {
                attributeList.set(i, newKey);
            }
        }
    }

    protected void resolveRename(AbstractModelBase object, String oldKey, String newKey) {
        final String oldKeyValue = object.get(oldKey);
        final String newKeyValue = object.get(newKey);

        if (StringUtils.isEmpty(newKeyValue)) {
            object.set(newKey, oldKeyValue);
            object.set(oldKey, null);
        }
    }

    private void mapContent(final AbstractModelBase object,
                            final String originalKey, final AbstractModelBase.Attribute updatedAttribute) {

        // read the original content
        final String originalKeyContent = object.get(originalKey);

        // check if there is original content
        if (StringUtils.isNotBlank(originalKeyContent)) {

            // read updated attribute content
            final String updatedAttributeContent = object.get(updatedAttribute);

            if (!StringUtils.isNotBlank(updatedAttributeContent)) {
                // original content available; no updated content: transfer and delete
                object.set(updatedAttribute, originalKeyContent);
                object.set(originalKey, null);
            } else {
                // original content and updated content available
                if (!originalKeyContent.equals(updatedAttributeContent)) {

                    // warn in case the values differ
                    LOG.warn("Vulnerability metadata inconsistent: " +
                                    "[{}] shows different content in attributes [{}] and [{}]. Please consolidate to [{}].",
                            object.get(VulnerabilityMetaData.Attribute.NAME), originalKey,
                            updatedAttribute.getKey(), updatedAttribute.getKey());
                }

                // values are identical or we warned the consumer: remove the obsolete original key value
                object.set(originalKey, null);
            }
        }
    }

    protected void applyModificationsForCompatibility(Inventory inventory) {
        final InventorySerializationContext serializationContext = inventory.getSerializationContext();

        updateArtifacts(inventory, serializationContext);
        updateAssetMetaData(inventory, serializationContext);
        updateLicenseMetaData(inventory, serializationContext);
        updateVulnerabilityMetaData(inventory, serializationContext);
        updateAdvisoryMetaData(inventory, serializationContext);
    }

    protected static class ParsingContext {

        public ParsingContext() {
            this.columns = new ArrayList<>();
            this.columnsMap = new HashMap<>();
        }

        final List<String> columns;
        final Map<Integer, String> columnsMap;
    }

    public String sheetNameToAssessmentContext(String sheetName) {
        // FIXME: also rename to assessment
        if ("Vulnerabilities".equals(sheetName)) {
            return VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT;
        } else if (sheetName.startsWith(VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX)) {
            return sheetName.substring(VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX.length());
        }
        return sheetName;
    }


    protected <T extends AbstractModelBase> T readRow(Row row, T modelBase, ParsingContext parsingContext) {
        final Map<Integer, String> map = parsingContext.columnsMap;

        for (Map.Entry<Integer, String> column : map.entrySet()) {
            final String columnName = column.getValue();
            final Cell cell = row.getCell(column.getKey());

            if (cell == null) {
                continue;
            }

            final String cellValue = formatDataCellValue(cell);
            final String effectiveCellValue, effectiveCellName;

            if (isSplitColumn(columnName)) {
                effectiveCellName = getSplitColumnName(columnName);
                effectiveCellValue = modelBase.get(effectiveCellName) != null ? modelBase.get(effectiveCellName) + cellValue : cellValue;
            } else {
                effectiveCellName = columnName;
                effectiveCellValue = cellValue;
            }

            modelBase.set(effectiveCellName, effectiveCellValue);
        }

        for (String key : modelBase.getAttributes()) {
            final String value = modelBase.get(key);
            if (value != null) {
                modelBase.set(key, value.trim());
            }
        }

        return modelBase;
    }

    private boolean isSplitColumn(String columnName) {
        if (StringUtils.isEmpty(columnName)) {
            return false;
        }

        return columnName.endsWith(")") && SPLIT_COLUMN_PATTERN.matcher(columnName).matches();
    }

    private String getSplitColumnName(String columnName) {
        if (StringUtils.isEmpty(columnName)) {
            return null;
        }

        final Matcher matcher = SPLIT_COLUMN_PATTERN.matcher(columnName);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public String formatDataCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            // format decimal numbers to ensure dot as decimal separator
            return numericCellDataFormatter.format(cell.getNumericCellValue());
        } else {
            return baseCellDataFormatter.formatCellValue(cell);
        }
    }

    protected void populateSerializationContextHeaders(Inventory inventory, Sheet sheet, String contextKey, ParsingContext pc) {
        final List<String> headerList = pc.columns;
        final List<String> filteredHeaderList = headerList.stream().filter(col -> !this.isSplitColumn(col)).collect(Collectors.toList());

        final InventorySerializationContext serializationContext = inventory.getSerializationContext();
        serializationContext.put(contextKey + ".columnlist", filteredHeaderList);
        for (int i = 0; i < headerList.size(); i++) {
            if (!filteredHeaderList.contains(headerList.get(i))) continue;
            final int width = sheet.getColumnWidth(i);
            serializationContext.put(contextKey + ".column[" + i + "].width", width);
        }
    }

}
