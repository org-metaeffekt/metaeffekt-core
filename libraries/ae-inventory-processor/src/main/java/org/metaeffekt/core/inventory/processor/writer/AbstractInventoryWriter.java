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
package org.metaeffekt.core.inventory.processor.writer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellBase;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.writer.excel.style.InventorySheetCellStyler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT;
import static org.metaeffekt.core.inventory.processor.writer.InventoryWriter.SINGLE_VULNERABILITY_ASSESSMENT_WORKSHEET;
import static org.metaeffekt.core.inventory.processor.writer.InventoryWriter.VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX;

public abstract class AbstractInventoryWriter {

    public abstract void writeInventory(Inventory inventory, File file) throws IOException;

    public String assessmentContextToSheetName(String assessmentContext) {
        if (VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT.equals(assessmentContext)) {
            return SINGLE_VULNERABILITY_ASSESSMENT_WORKSHEET;
        } else {
            return VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX + assessmentContext;
        }
    }

    protected int reinsert(int insertIndex, String key, List<String> orderedAttributesList, Set<String> attributesSet) {
        if (attributesSet.contains(key)) {
            orderedAttributesList.remove(key);
            orderedAttributesList.add(Math.min(insertIndex, orderedAttributesList.size()), key);
            insertIndex++;
        }
        return insertIndex;
    }

    // FIXME: must be moved more centrally; InventoryUtils?
    protected static boolean isAssetId(String key) {
        return key.startsWith("CID-") ||
                key.startsWith("AID-") ||
                key.startsWith("EID-") ||
                key.startsWith("IID-") ||
                key.startsWith("EAID-");
    }

    /**
     * Indicates into how many columns a column should be split based on strings in that column that are too long to be stored in their cells.
     *
     * @param orderedColumnHeaders the list of column headers
     * @param models               the models
     * @param maxCellLength        the maximum cell length
     *
     * @param <T> Generic collection type.
     *
     * @return the map of column headers and how many columns they span
     */
    protected <T extends AbstractModelBase> LinkedHashMap<String, Integer> splitIntoMultipleColumns(Collection<String> orderedColumnHeaders, Collection<T> models, int maxCellLength) {
        final LinkedHashMap<String, Integer> result = new LinkedHashMap<>();

        for (String columnHeader : orderedColumnHeaders) {
            int maxColumnCount = 1;
            for (T model : models) {
                final String value = model.get(columnHeader);
                if (value != null && value.length() > maxCellLength) {
                    final int columnCount = (int) Math.ceil((double) value.length() / maxCellLength);
                    maxColumnCount = Math.max(maxColumnCount, columnCount);
                }
            }
            result.put(columnHeader, maxColumnCount);
        }

        return result;
    }

    /**
     * Creates column header names based on how many times they repeat using the following pattern (example: <code>Vulnerability</code> with 4 columns):<br>
     * <code>Vulnerability</code><br>
     * <code>Vulnerability (split-1)</code><br>
     * <code>Vulnerability (split-2)</code><br>
     * <code>Vulnerability (split-3)</code>
     *
     * @param splitColumnHeaders the map of column headers and how many columns they span
     * @return a map of the unmodified column headers with their split counterparts
     */
    protected LinkedHashMap<String, List<String>> deriveEffectiveSplitColumnHeaders(LinkedHashMap<String, Integer> splitColumnHeaders) {
        final LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();

        for (String columnHeader : splitColumnHeaders.keySet()) {
            final List<String> columnHeaders = new ArrayList<>();
            final int columnCount = splitColumnHeaders.get(columnHeader);
            if (columnCount > 1) {
                for (int i = 0; i < columnCount; i++) {
                    if (i == 0) {
                        columnHeaders.add(columnHeader);
                    } else {
                        columnHeaders.add(columnHeader + " (split-" + i + ")");
                    }
                }
            } else {
                columnHeaders.add(columnHeader);
            }
            result.put(columnHeader, columnHeaders);
        }

        return result;
    }

    protected <AMB extends AbstractModelBase, CR extends Row, HC extends CellBase> int populateSheetWithModelData(
            int maxCellLength,
            Collection<AMB> models, Collection<String> columnHeaders,
            Function<Integer, HC> headerCellSupplier, Function<Integer, CR> contentRowSupplier, Function<String, RichTextString> richTextInitializer,
            InventorySheetCellStyler[] headerCellStyler, InventorySheetCellStyler[] dataCellStyler) {

        final LinkedHashMap<String, Integer> columnHeaderSplitCount = this.splitIntoMultipleColumns(columnHeaders, models, maxCellLength);
        final LinkedHashMap<String, List<String>> columnHeadersWithSplitColumnInformation = this.deriveEffectiveSplitColumnHeaders(columnHeaderSplitCount);

        // populate header row
        {
            int cellNum = 0;
            for (Map.Entry<String, List<String>> columnHeaderGroup : columnHeadersWithSplitColumnInformation.entrySet()) {
                final String originalColumnName = columnHeaderGroup.getKey();
                final List<String> splitColumnNames = columnHeaderGroup.getValue();

                for (String splitColumnName : splitColumnNames) {
                    final HC headerCell = headerCellSupplier.apply(cellNum);
                    headerCell.setCellValue(richTextInitializer.apply(splitColumnName));

                    for (InventorySheetCellStyler styler : headerCellStyler) {
                        if (styler.isApplicable(headerCell, headerCell.getSheet(), headerCell.getRow(), originalColumnName, splitColumnName)) {
                            styler.applyStyle(headerCell, headerCell.getSheet(), headerCell.getRow(), originalColumnName, splitColumnName);
                            break;
                        }
                    }

                    cellNum++;
                }
            }
        }

        // populate data rows
        {
            int rowNum = 1;
            for (AMB model : models) {
                int cellNum = 0;
                final CR dataRow = contentRowSupplier.apply(rowNum);

                for (Map.Entry<String, List<String>> columnHeaderGroup : columnHeadersWithSplitColumnInformation.entrySet()) {
                    final String originalColumnName = columnHeaderGroup.getKey();
                    final List<String> splitColumnNames = columnHeaderGroup.getValue();
                    final String columnValue = model == null ? null : model.get(originalColumnName);

                    if (columnValue == null) {
                        cellNum += splitColumnNames.size();
                        continue;
                    }

                    if (splitColumnNames.size() == 1) {
                        // directly set the value if only one column is needed
                        final Cell dataCell = dataRow.createCell(cellNum);
                        dataCell.setCellValue(richTextInitializer.apply(columnValue));

                        for (InventorySheetCellStyler styler : dataCellStyler) {
                            if (styler.isApplicable(dataCell, dataRow.getSheet(), dataRow, originalColumnName, originalColumnName, columnValue, columnValue)) {
                                styler.applyStyle(dataCell, dataRow.getSheet(), dataRow, originalColumnName, originalColumnName, columnValue, columnValue);
                                break;
                            }
                        }

                        cellNum++;
                    } else {
                        for (int splitIndex = 0; splitIndex < splitColumnNames.size(); splitIndex++) {
                            final String splitColumnName = splitColumnNames.get(splitIndex);
                            final int startIndex = splitIndex * maxCellLength;
                            final int endIndex = Math.min((splitIndex + 1) * maxCellLength, columnValue.length());

                            String splitColumnValue = null;
                            if (startIndex < columnValue.length()) {
                                splitColumnValue = columnValue.substring(startIndex, endIndex);
                            }

                            final Cell dataCell = dataRow.createCell(cellNum);
                            dataCell.setCellValue(richTextInitializer.apply(splitColumnValue));

                            for (InventorySheetCellStyler styler : dataCellStyler) {
                                if (styler.isApplicable(dataCell, dataRow.getSheet(), dataRow, originalColumnName, splitColumnName, columnValue, splitColumnValue)) {
                                    styler.applyStyle(dataCell, dataRow.getSheet(), dataRow, originalColumnName, splitColumnName, columnValue, splitColumnValue);
                                    break;
                                }
                            }

                            cellNum++;
                        }
                    }
                }
                rowNum++;
            }
        }

        return columnHeadersWithSplitColumnInformation.values().stream().mapToInt(List::size).sum();
    }

    protected List<String> determineOrder(Inventory inventory, Supplier<List<? extends AbstractModelBase>> supplier,
                        String columnKeyList, List<String> coreAttributes, List<String> orderedAttributes) {

        // create columns for key / value map content
        final Set<String> attributeSet = new HashSet<>(coreAttributes);
        for (final AbstractModelBase obj : supplier.get()) {
            attributeSet.addAll(obj.getAttributes());
        }

        final List<String> contextColumnList = inventory.getSerializationContext().get(columnKeyList);
        if (contextColumnList != null) {
            attributeSet.addAll(contextColumnList);
        }

        // impose context or default order
        final List<String> ordered = new ArrayList<>(attributeSet);
        Collections.sort(ordered);
        int insertIndex = 0;
        if (contextColumnList != null) {
            for (String key : contextColumnList) {
                insertIndex = reinsert(insertIndex, key, ordered, attributeSet);
            }
        } else {
            for (String key : orderedAttributes) {
                insertIndex = reinsert(insertIndex, key, ordered, attributeSet);
            }
        }
        return ordered;
    }

}
