package org.metaeffekt.core.itest.common;

import org.metaeffekt.core.inventory.processor.model.Inventory;


public interface Preparer {

    Preparer setName(String testname);

    Preparer setSource(String source);

    boolean clear() throws Exception;

    boolean download(boolean overwrite) throws Exception;

    boolean inventorize(boolean overwrite) throws Exception;

    boolean loadInventory() throws Exception;

    boolean rebuildInventory() throws Exception;

    Inventory getInventory() throws Exception;

    Inventory readReferenceInventory() throws Exception;

    Preparer setReferenceInventory(String referenceinventory);
}
