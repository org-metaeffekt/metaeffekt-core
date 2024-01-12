package org.metaeffekt.core.itest.common;

public class Testconfig {

    private static final String downloadFolder = "target/.test/downloads/";
    private static final String scanFolder = "target/.test/scan/";
    private static final String inventoryFolder = "target/.test/inventory/";

    public static String getDownloadFolder() {
        return downloadFolder;
    }

    public static String getScanFolder() {
        return scanFolder;
    }

    public static String getInventoryFolder() {
        return inventoryFolder;
    }
}
