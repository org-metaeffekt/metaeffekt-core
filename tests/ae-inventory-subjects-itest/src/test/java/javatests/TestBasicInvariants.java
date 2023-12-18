package javatests;

import common.Preparator;
import genericTests.CheckInvariants;
import inventory.InventoryScanner;
import org.metaeffekt.core.inventory.processor.model.Inventory;

public abstract class TestBasicInvariants {

    static protected Preparator preparator;

    private Inventory inventory;

    private InventoryScanner scanner;

    public Inventory getInventory() throws Exception {
        if (inventory == null) {
            System.out.println("getInventory");
            this.inventory = preparator.getInventory();
            System.out.println(inventory);
        }
        return inventory;
    }

    public InventoryScanner getScanner() {
        try {
            if (scanner == null) {
                System.out.println("getScanner");
                this.scanner = new InventoryScanner(getInventory());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return scanner;
    }

    public InventoryScanner getScannerAfterInvariants() {
        CheckInvariants.assertInvariants(getScanner());
        return scanner;

    }
}
