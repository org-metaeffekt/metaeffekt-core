package org.metaeffekt.core.itest.javatests;

import org.metaeffekt.core.itest.common.Preparer;
import org.metaeffekt.core.itest.genericTests.CheckInvariants;
import org.metaeffekt.core.itest.inventory.Analysis;
import org.metaeffekt.core.inventory.processor.model.Inventory;

public abstract class TestBasicInvariants {

    static protected Preparer preparer;

    private Inventory inventory;

    private Analysis analysis;

    public Inventory getInventory() throws Exception {
        if (inventory == null) {
            this.inventory = preparer.getInventory();
            System.out.println(inventory);
        }
        return inventory;
    }

    public Analysis getAnalysis() {
        try {
            if (analysis == null) {
                this.analysis = new Analysis(getInventory());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return analysis;
    }

    public Analysis getAnalysisAfterInvariants() {
        CheckInvariants.assertInvariants(getAnalysis());
        return analysis;
    }
}
