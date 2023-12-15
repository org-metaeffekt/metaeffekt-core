package javatests;

import common.JarPreparator;
import common.Preparator;
import inventory.InventoryScanner;
import org.junit.*;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static genericTests.CheckInvariants.*;
import static inventory.dsl.ArtifactPredicate.IdMissmatchesVersion;

public class BouncyCastle {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static Preparator preparator;
    
    private Inventory inventory;
    private InventoryScanner scanner;

    @BeforeClass
    public static void prepare() {
        preparator = new JarPreparator()
                .setSource("https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/1.77/bcprov-jdk18on-1.77.jar")
                .setName(BouncyCastle.class.getName());
    }
    
    @Before
    public void prepareInventory() throws Exception{
        this.inventory = preparator.getInventory();
        this.scanner = new InventoryScanner(inventory);
    }


    @Ignore
    @Test
    public void clear() throws Exception{
        Assert.assertTrue(preparator.clear());

    }
    @Ignore
    @Test
    public void inventorize() throws Exception{
        Assert.assertTrue(preparator.rebuildInventory());
        //
        // erst prerequisits und danach den Test
        // Keine Folgefehler
        //
    }
    @Test
    public void checkInvariants() throws Exception {
        assertInvariants(scanner);
    }

    @Test public void versionMissmatch(){
        scanner.select(IdMissmatchesVersion)
                .mustBeEmpty();
    }


}
