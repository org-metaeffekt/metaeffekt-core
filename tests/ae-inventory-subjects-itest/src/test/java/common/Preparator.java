package common;

import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.IOException;
import java.net.MalformedURLException;

public interface Preparator {

    Preparator setName(String testname);

    Preparator setSource(String source);

    boolean clear() throws Exception;

    boolean download(boolean overwrite) throws Exception;

    boolean inventorize(boolean overwrite) throws Exception;

    boolean loadInventory() throws Exception;

    boolean rebuildInventory() throws Exception;

    Inventory getInventory() throws Exception;

    Inventory readReferenceInventory() throws Exception;
}
