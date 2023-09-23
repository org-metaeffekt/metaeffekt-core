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
package org.metaeffekt.core.inventory.processor.writer.excel;

import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellBase;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;
import org.metaeffekt.core.inventory.processor.writer.AbstractInventoryWriter;
import org.metaeffekt.core.inventory.processor.writer.excel.style.InventorySheetCellStyler;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractXlsxInventoryWriter extends AbstractInventoryWriter {

    /**
     * Excel 97 limits the maximum cell content length to <code>32767</code> characters. To ensure that the contents are
     * safe, 7 is subtracted from that value to set the max length to <code>32760</code>.
     */
    public final static int MAX_CELL_LENGTH = SpreadsheetVersion.EXCEL97.getMaxTextLength() - 7;

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

    protected <AMB extends AbstractModelBase, CR extends SXSSFRow, HC extends CellBase> int populateSheetWithModelData(
            Collection<AMB> models, Collection<String> columnHeaders,
            Function<Integer, HC> headerCellSupplier, Function<Integer, CR> contentRowSupplier,
            InventorySheetCellStyler[] headerCellStyler, InventorySheetCellStyler[] dataCellStyler) {
        return super.populateSheetWithModelData(
                MAX_CELL_LENGTH,
                models, columnHeaders,
                headerCellSupplier, contentRowSupplier, HSSFRichTextString::new,
                headerCellStyler, dataCellStyler);
    }

}
