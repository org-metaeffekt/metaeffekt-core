package org.metaeffekt.core.itest.javaartifacts;

import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.itest.inventory.Analysis;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public interface AnalysisTemplate {

    default Analysis getTemplate(String templatepath) throws IOException {
        URL templateurl = this.getClass().getResource(templatepath);
        File file = new File(templateurl.getFile());
        Inventory template = InventoryUtils.readInventory(file, "*.xls");
        Analysis analysis = new Analysis(template, templatepath);
        return analysis;
    }
}
