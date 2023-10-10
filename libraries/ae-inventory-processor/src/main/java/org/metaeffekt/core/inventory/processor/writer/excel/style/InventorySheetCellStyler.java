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
package org.metaeffekt.core.inventory.processor.writer.excel.style;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class InventorySheetCellStyler {
    public boolean isApplicable(Cell cell, Sheet sheet, Row row, String fullColumnHeader, String splitColumnHeader) {
        return isApplicable(cell, sheet, row, fullColumnHeader, splitColumnHeader, null, null);
    }

    public void applyStyle(Cell cell, Sheet sheet, Row row, String fullColumnHeader, String splitColumnHeader) {
        applyStyle(cell, sheet, row, fullColumnHeader, splitColumnHeader, null, null);
    }

    public abstract boolean isApplicable(Cell cell, Sheet sheet, Row row, String fullColumnHeader, String splitColumnHeader, String fullCellContent, String splitCellContent);

    public abstract void applyStyle(Cell cell, Sheet sheet, Row row, String fullColumnHeader, String splitColumnHeader, String fullCellContent, String splitCellContent);

    public static InventorySheetCellStyler createStyler(Predicate<InventoryCellStylerContext> isApplicableLambda, Consumer<InventoryCellStylerContext> applyStyleLambda) {
        return new InventorySheetCellStyler() {
            @Override
            public boolean isApplicable(Cell cell, Sheet sheet, Row row, String fullColumnHeader, String splitColumnHeader, String fullCellContent, String splitCellContent) {
                return isApplicableLambda.test(new InventoryCellStylerContext(cell, sheet, row, fullColumnHeader, splitColumnHeader, fullCellContent, splitCellContent));
            }

            @Override
            public void applyStyle(Cell cell, Sheet sheet, Row row, String fullColumnHeader, String splitColumnHeader, String fullCellContent, String splitCellContent) {
                applyStyleLambda.accept(new InventoryCellStylerContext(cell, sheet, row, fullColumnHeader, splitColumnHeader, fullCellContent, splitCellContent));
            }
        };
    }

    public static class InventoryCellStylerContext {
        private final Cell cell;
        private final Sheet sheet;
        private final Row row;
        private final String fullColumnHeader;
        private final String splitColumnHeader;
        private final String fullCellContent;
        private final String splitCellContent;

        public InventoryCellStylerContext(Cell cell, Sheet sheet, Row row, String fullColumnHeader, String splitColumnHeader, String fullCellContent, String splitCellContent) {
            this.cell = cell;
            this.sheet = sheet;
            this.row = row;
            this.fullColumnHeader = fullColumnHeader;
            this.splitColumnHeader = splitColumnHeader;
            this.fullCellContent = fullCellContent;
            this.splitCellContent = splitCellContent;
        }

        public boolean hasCellContent() {
            return fullCellContent != null || splitCellContent != null;
        }

        public Cell getCell() {
            return cell;
        }

        public Row getRow() {
            return row;
        }

        public Sheet getSheet() {
            return sheet;
        }

        public Workbook getWorkbook() {
            return sheet.getWorkbook();
        }

        public String getFullColumnHeader() {
            return fullColumnHeader;
        }

        public String getSplitColumnHeader() {
            return splitColumnHeader;
        }

        public String getFullCellContent() {
            return fullCellContent;
        }

        public String getSplitCellContent() {
            return splitCellContent;
        }

        public int getColumnIndex() {
            return cell.getColumnIndex();
        }

        public int getRowIndex() {
            return cell.getRowIndex();
        }

        public boolean isSplitColumn() {
            return fullColumnHeader != null && !fullColumnHeader.equals(splitColumnHeader);
        }

        public boolean isHeaderEither(AbstractModelBase.Attribute... attributes) {
            for (AbstractModelBase.Attribute attribute : attributes) {
                if (fullColumnHeader != null && fullColumnHeader.equals(attribute.getKey())) {
                    return true;
                }
            }
            return false;
        }
    }
}
