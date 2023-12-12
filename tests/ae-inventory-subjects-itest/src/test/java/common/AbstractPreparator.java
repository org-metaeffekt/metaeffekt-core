package common;

import org.apache.commons.io.FileUtils;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.sql.SQLOutput;

public abstract class AbstractPreparator implements Preparator {

    private final Logger LOG = LoggerFactory.getLogger(AbstractPreparator.class);

    String url;
    String name;
    private Inventory inventory;
    private String myDir = "";

    public String getDownloadFolder() {
        return Testconfig.getDownloadFolder() + myDir;
    }

    public String getScanFolder() {
        return Testconfig.getScanFolder() + myDir;
    }

    public String getInventoryFolder() {
        return Testconfig.getInventoryFolder() + myDir;
    }

    public Preparator setSource(String url) {
        this.url = url;
        return this;
    }

    @Override
    public Preparator setName(String testname) {
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
        download(false);
        return inventorize(true);
    }

    @Override
    public Inventory getInventory() throws Exception {
        download(false);
        inventorize(false);
        loadInventory();
        return inventory;
    }

    private void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory readReferenceInventory() throws Exception {
        URL url = this.getClass().getResource("/" + myDir + "referenceinventory/");
        if (url == null) {
            LOG.info("NO REFERENCE INVENTORY");
            return new Inventory();
        }
        File file = new File(url.getFile());
        return InventoryUtils.readInventory(file, "*.xls");
    }
}
