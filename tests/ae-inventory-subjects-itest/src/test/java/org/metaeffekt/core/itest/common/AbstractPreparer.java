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
        if(referenceInventory.isEmpty()) return new Inventory();
        URL url = this.getClass().getResource("/"+referenceInventory);
        if (url == null) {
            LOG.error("reference inventory not found: "+referenceInventory);
            return new Inventory();
        }
        LOG.info("Loading reference inventory: "+referenceInventory);
        File file = new File(url.getFile());
        return InventoryUtils.readInventory(file, "*.xls");
    }

    @Override
    public Preparer setReferenceInventory(String referenceinventory) {
        this.referenceInventory = referenceinventory;
        return this;
    }
}
