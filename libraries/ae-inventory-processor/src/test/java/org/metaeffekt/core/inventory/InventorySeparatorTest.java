package org.metaeffekt.core.inventory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.InventorySeparator;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InventorySeparatorTest {

    private static Inventory inventory;
    private static List<Inventory> separatedInventories;

    @BeforeClass
    public static void setUp() throws Exception {
        inventory = new InventoryReader().readInventory(new File("src/test/resources/separator/keycloak-25.0.4.xlsx"));
        separatedInventories = InventorySeparator.separate(inventory);

        int inventoryCounter = 0;

        for (Inventory inventoryItem : separatedInventories) {
            File outputFile = new File("target/test-classes/separator/separated-" + inventoryCounter + ".xlsx");
            FileUtils.forceMkdirParent(outputFile);
            new InventoryWriter().writeInventory(inventoryItem, outputFile);
            inventoryCounter++;
        }
    }

    @Test
    public void testAssetSheet() {
        Inventory separatedInventory1 = separatedInventories.get(0);
        Inventory separatedInventory2 = separatedInventories.get(1);

        List<AssetMetaData> assetsOfSeparatedInventory1 = separatedInventory1.getAssetMetaData();
        List<AssetMetaData> assetsOfSeparatedInventory2 = separatedInventory2.getAssetMetaData();

        assertThat(assetsOfSeparatedInventory1.size()).isEqualTo(1);
        assertThat(assetsOfSeparatedInventory2.size()).isEqualTo(1);

        assertThat(assetsOfSeparatedInventory1.get(0).get(AssetMetaData.Attribute.ASSET_ID)).isEqualTo("CID-8ddfbf9408625337e69608156e0a075016b427b672d796cd694450222f88ac23");
        assertThat(assetsOfSeparatedInventory2.get(0).get(AssetMetaData.Attribute.ASSET_ID)).isEqualTo("AID-keycloak-admin-cli-25.0.4.jar-e66e096cb88f1b4542bb5776918bf048");
    }

    @Test
    public void testArtifactSheet() {
        Inventory separatedInventory1 = separatedInventories.get(0);
        Inventory separatedInventory2 = separatedInventories.get(1);

        List<Artifact> artifactsOfSeparatedInventory1 = separatedInventory1.getArtifacts();
        List<Artifact> artifactsOfSeparatedInventory2 = separatedInventory2.getArtifacts();

        assertThat(artifactsOfSeparatedInventory1.size()).isEqualTo(449);
        assertThat(artifactsOfSeparatedInventory2.size()).isEqualTo(11);

        assertThat(artifactsOfSeparatedInventory1.get(0).get("AID-keycloak-admin-cli-25.0.4.jar-e66e096cb88f1b4542bb5776918bf048")).isNull();
        assertThat(artifactsOfSeparatedInventory2.get(0).get("CID-8ddfbf9408625337e69608156e0a075016b427b672d796cd694450222f88ac23")).isNull();
    }
}
