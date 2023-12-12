package common;

public class Testconfig {

    private static final String downloadFolder = ".test/downloads/";
    private static final String scanFolder = ".test/scan/";
    private static final String inventoryFolder = ".test/inventory/";

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
