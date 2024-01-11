package org.metaeffekt.core.itest.common;

import org.apache.commons.io.FileUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public abstract class AbstractPreparer implements Preparer {

    private final Logger LOG = LoggerFactory.getLogger(AbstractPreparer.class);

    protected String url;
    String name;
    private Inventory inventory;
    private String myDir = "";

    private String referenceInventory = "";

    public String getDownloadFolder() {
        return Testconfig.getDownloadFolder() + myDir;
    }

    public String getScanFolder() {
        return Testconfig.getScanFolder() + myDir;
    }

    public String getInventoryFolder() {
        return Testconfig.getInventoryFolder() + myDir;
    }

    public Preparer setSource(String url) {
        this.url = url;
        return this;
    }

    @Override
    public Preparer setName(String testname) {
        this.name = testname;
        this.myDir = name.replace(".", "/") + "/";
        return this;
    }

    @Override
    public boolean clear() throws Exception {
        FileUtils.deleteDirectory(new File(getInventoryFolder()));
        FileUtils.deleteDirectory(new File(getDownloadFolder()));
        FileUtils.deleteDirectory(new File(getScanFolder()));
        return true;
    }

    @Override
    public boolean loadInventory() throws Exception {
        if (inventory == null) {
            setInventory(new InventoryReader().readInventory(new File(getInventoryFolder() + "scan-inventory.ser")));
        }
        return inventory != null;
    }

    @Override
    public boolean rebuildInventory() throws Exception {
        return load(false) &&
                inventorize(true);
    }

    @Override
    public Inventory getInventory() throws Exception {
        load(false);
        inventorize(false);
        loadInventory();
        return inventory;
    }

    private void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory readReferenceInventory() throws Exception {
        if (referenceInventory.isEmpty()) return new Inventory();
        URL url = this.getClass().getResource("/" + referenceInventory);
        if (url == null) {
            LOG.error("reference inventory not found: " + referenceInventory);
            return new Inventory();
        }
        LOG.info("Loading reference inventory: " + referenceInventory);
        File file = new File(url.getFile());
        return InventoryUtils.readInventory(file, "*.xls");
    }

    @Override
    public Preparer setReferenceInventory(String referenceinventory) {
        this.referenceInventory = referenceinventory;
        return this;
    }
}
