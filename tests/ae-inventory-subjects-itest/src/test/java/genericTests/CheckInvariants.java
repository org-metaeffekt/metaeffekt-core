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
        assertThat(scanner.getErrorlist()).as("Artifacts should not have errors after parsing.").isEmpty();
    }

    public static void assertAtLeastOneArtifact(InventoryScanner scanner) {
        assertThat(scanner.getArtifacts()).as("Artifactlist should not be empty").isNotEmpty();
    }

    public static void assertNoMissingTypes(InventoryScanner scanner) {
        assertThat(scanner.getMissingTypelist()).as("Artifactlist should have type identified").isEmpty();
    }
}
