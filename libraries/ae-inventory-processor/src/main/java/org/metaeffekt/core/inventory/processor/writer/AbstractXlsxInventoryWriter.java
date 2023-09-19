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

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.util.*;

public abstract class AbstractXlsxInventoryWriter extends AbstractInventoryWriter {

    /**
     * Excel 97 limits the maximum cell content length to <code>32767</code> characters. To ensure that the contents are
     * safe, 7 is subtracted from that value to set the max length to <code>32760</code>.
     */
    public final static int MAX_CELL_LENGTH = SpreadsheetVersion.EXCEL97.getMaxTextLength() - 7;

    /**
     * Contains self-managed palette of colors. XSSF api is strange.
     */
    private final Map<String, XSSFColor> colorPalette = new HashMap<>();

    private final DefaultIndexedColorMap indexedColorMap = new DefaultIndexedColorMap();

    protected boolean isEmpty(Collection<?> collection) {
        if (collection == null) return true;
        return collection.isEmpty();
    }

    protected int reinsert(int insertIndex, String key, List<String> orderedAttributesList, Set<String> attributesSet) {
        if (attributesSet.contains(key)) {
            orderedAttributesList.remove(key);
            orderedAttributesList.add(Math.min(insertIndex, orderedAttributesList.size()), key);
            insertIndex++;
        }
        return insertIndex;
    }

    protected CellStyle createDefaultHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "153,204,255");

        final Font headerFont = workbook.createFont();
        headerFont.setColor(Font.COLOR_NORMAL);

        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        return headerStyle;
    }

    protected CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        return createDefaultHeaderStyle(workbook);
    }

    protected CellStyle createAssetSourceHeaderStyle(SXSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "155,192,0"));
    }

    protected CellStyle createAssetConfigHeaderStyle(SXSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "219,219,219"));
    }

    protected CellStyle createAssetHeaderStyle(SXSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "255,192,0"));
    }

    protected CellStyle createRotatedCellStyle(SXSSFWorkbook workbook, XSSFColor headerColor) {
        final CellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setRotation((short) -90);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected CellStyle createCenteredStyle(SXSSFWorkbook workbook) {
        final CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    protected CellStyle createWarnHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "244,176,132");
        return createRotatedCellStyle(workbook, headerColor);
    }

    protected CellStyle createErrorHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "244,176,132");
        final CellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected XSSFColor resolveColor(SXSSFWorkbook workbook, String rgb) {
        final String[] rgbSplit = rgb.trim().split(", ?");
        final int red = Short.parseShort(rgbSplit[0]);
        final int green = Short.parseShort(rgbSplit[1]);
        final int blue = Short.parseShort(rgbSplit[2]);

        XSSFColor color = colorPalette.get(rgb);
        if (color == null) {
            // create color and register in map
            color = new XSSFColor(new java.awt.Color(red, green, blue), indexedColorMap);

            // add to map
            colorPalette.put(rgb, color);
        }
        return color;
    }

}
