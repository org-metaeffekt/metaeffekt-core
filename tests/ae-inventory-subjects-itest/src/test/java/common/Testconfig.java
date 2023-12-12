package common;

public class Testconfig {

    private static  String downloadFolder = ".test/downloads/";
    private static  String scanFolder = ".test/scan/";
    private static  String inventoryFolder = ".test/inventory/";

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
