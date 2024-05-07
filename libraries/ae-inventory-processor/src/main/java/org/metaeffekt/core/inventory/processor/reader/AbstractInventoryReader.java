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
package org.metaeffekt.core.inventory.processor.reader;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
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

    protected void update(Artifact artifact) {
        resolveRename(artifact, "Component / Group", Artifact.Attribute.COMPONENT.getKey());
    }

    protected void update(LicenseMetaData licenseMetaData) {
        resolveRename(licenseMetaData, "Text", LicenseMetaData.Attribute.NOTICE.getKey());
    }

    protected void resolveRename(AbstractModelBase item, String oldKey, String newKey) {
        final String oldKeyValue = item.get(oldKey);
        final String newKeyValue = item.get(newKey);

        if (org.apache.commons.lang3.StringUtils.isEmpty(newKeyValue)) {
            item.set(newKey, oldKeyValue);
        }
    }

    protected void update(VulnerabilityMetaData vulnerabilityMetaData) {
        // compensate rename of attributes
        mapContent(vulnerabilityMetaData, "Referenced Content IDs", AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS);
        mapContent(vulnerabilityMetaData, "Referenced Content Ids", AeaaInventoryAttribute.VULNERABILITY_REFERENCED_CONTENT_IDS);
    }

    private void mapContent(final VulnerabilityMetaData vulnerabilityMetaData,
                            final String originalKey, final AbstractModelBase.Attribute updatedAttribute) {

        // read the original content
        final String originalKeyContent = vulnerabilityMetaData.get(originalKey);

        // check if there is original content
        if (StringUtils.isNotBlank(originalKeyContent)) {

            // read updated attribute content
            final String updatedAttributeContent = vulnerabilityMetaData.get(updatedAttribute);

            if (!StringUtils.isNotBlank(updatedAttributeContent)) {
                // original content available; no updated content: transfer and delete
                vulnerabilityMetaData.set(updatedAttribute, originalKeyContent);
                vulnerabilityMetaData.set(originalKey, null);
            } else {
                // original content and updated content available
                if (!originalKeyContent.equals(updatedAttributeContent)) {

                    // warn in case the values differ
                    LOG.warn("Vulnerability metadata inconsistent: " +
                                    "[{}] shows different content in attributes [{}] and [{}]. Please consolidate to [{}].",
                            vulnerabilityMetaData.get(VulnerabilityMetaData.Attribute.NAME), originalKey,
                            updatedAttribute.getKey(), updatedAttribute.getKey());
                }

                // values are identical or we warned the consumer: remove the obsolete original key value
                vulnerabilityMetaData.set(originalKey, null);
            }
        }
    }

    protected void applyModificationsForCompatibility(Inventory inventory) {
        inventory.getArtifacts().forEach(this::update);
        inventory.getLicenseMetaData().forEach(this::update);
        inventory.getVulnerabilityMetaDataContexts().forEach(
                context -> inventory.getVulnerabilityMetaData(context).forEach(this::update)
        );
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
}
