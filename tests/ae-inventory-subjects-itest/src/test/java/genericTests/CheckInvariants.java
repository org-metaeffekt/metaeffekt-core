package genericTests;

import inventory.InventoryScanner;

import static inventory.dsl.predicates.Not.not;
import static inventory.dsl.predicates.attributes.Exists.exists;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;

public class CheckInvariants {

    public static void assertInvariants(InventoryScanner scanner){
        assertAtLeastOneArtifact(scanner);
        //assertNoMissingTypes(scanner);
        assertNoErrors(scanner);

    }
    public static void assertNoErrors(InventoryScanner scanner) {
        scanner.select(exists("Errors"))
                .mustBeEmpty();
    }

    public static void assertAtLeastOneArtifact(InventoryScanner scanner) {
        scanner.select()
                .hasSizeGreaterThan(0);
    }

    public static void assertNoMissingTypes(InventoryScanner scanner) {
        scanner.select(not(exists(TYPE)))
                .mustBeEmpty();
    }
}
