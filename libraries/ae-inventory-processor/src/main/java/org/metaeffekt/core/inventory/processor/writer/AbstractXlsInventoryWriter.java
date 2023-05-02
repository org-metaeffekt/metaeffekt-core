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

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.util.*;

public class AbstractXlsInventoryWriter {

    /**
     * Excel 97 limits the maximum cell content length to <code>32767</code> characters. To ensure that the contents are
     * safe, 7 is subtracted from that value to set the max length to <code>32760</code>.
     */
    public final static int MAX_CELL_LENGTH = SpreadsheetVersion.EXCEL97.getMaxTextLength() - 7;

    /**
     * Contains self-managed palette of colors. HSSF api is strange.
     */
    private final Map<String, HSSFColor> colorPalette = new HashMap<>();

    protected boolean isEmpty(Collection<?> collection) {
        if (collection == null) return true;
        return collection.isEmpty();
    }

    protected HSSFCellStyle createDefaultHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "153,204,255");

        final Font headerFont = workbook.createFont();
        headerFont.setColor(Font.COLOR_NORMAL);

        final HSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        return headerStyle;
    }

    protected HSSFCellStyle createHeaderStyle(HSSFWorkbook workbook) {
        return createDefaultHeaderStyle(workbook);
    }

    protected HSSFCellStyle createAssetSourceHeaderStyle(HSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "155,192,0"));
    }

    protected HSSFCellStyle createAssetConfigHeaderStyle(HSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "219,219,219"));
    }

    protected HSSFCellStyle createAssetHeaderStyle(HSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "255,192,0"));
    }

    protected HSSFCellStyle createRotatedCellStyle(HSSFWorkbook workbook, HSSFColor headerColor) {
        final HSSFCellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor.getIndex());
        cellStyle.setRotation((short) -90);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected HSSFCellStyle createCenteredStyle(HSSFWorkbook workbook) {
        final HSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    protected HSSFCellStyle createWarnHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "244,176,132");
        return createRotatedCellStyle(workbook, headerColor);
    }

    protected HSSFCellStyle createErrorHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "244,176,132");
        final HSSFCellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor.getIndex());
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected HSSFColor resolveColor(HSSFWorkbook workbook, String rgb) {
        final HSSFPalette palette = workbook.getCustomPalette();
        final String[] rgbSplit = rgb.trim().split(", ?");
        final byte red = (byte) Short.parseShort(rgbSplit[0]);
        final byte green = (byte) Short.parseShort(rgbSplit[1]);
        final byte blue = (byte) Short.parseShort(rgbSplit[2]);

        HSSFColor color = colorPalette.get(rgb);
        if (color == null) {
            // the index needs to start with 7 (8 being an offset that is used by HSSFPalette
            final int index = colorPalette.size() + 8;

            try {
                // adjust color
                palette.setColorAtIndex((short) index, red, green, blue);

                // access color using index
                color = palette.getColor(index);

            } catch (RuntimeException e) {
                // fallback to similar color (since palette is limited)
                color = palette.findSimilarColor(red, green, blue);
            }

            // add to map
            colorPalette.put(rgb, color);
        }
        return color;
    }

}
