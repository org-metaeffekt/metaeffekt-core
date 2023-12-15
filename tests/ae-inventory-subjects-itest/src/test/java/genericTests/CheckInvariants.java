package genericTests;

import inventory.InventoryScanner;
import static org.assertj.core.api.Assertions.assertThat;

public class CheckInvariants {

    public static void assertInvariants(InventoryScanner scanner){
        assertAtLeastOneArtifact(scanner);
        assertNoMissingTypes(scanner);
        assertNoErrors(scanner);

    }
    public static void assertNoErrors(InventoryScanner scanner) {
        scanner.selectAllArtifacts()
                .hasNoErrors();
    }

    public static void assertAtLeastOneArtifact(InventoryScanner scanner) {
        scanner.selectAllArtifacts()
                .hasSizeGreaterThan(1000);
    }

    public static void assertNoMissingTypes(InventoryScanner scanner) {
        scanner.selectAllArtifacts()
                .filter(artifact -> artifact.get("Type") == null)
                .as("Artifcatlist with missing type")
                .mustBeEmpty();
    }
}
