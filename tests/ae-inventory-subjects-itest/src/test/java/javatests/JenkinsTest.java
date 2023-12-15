package javatests;

import common.JarPreparator;
import common.Preparator;
import org.junit.*;
import inventory.InventoryScanner;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static genericTests.CheckInvariants.*;
import static org.assertj.core.api.Assertions.assertThat;

public class JenkinsTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static Preparator preparator;
    
    private Inventory inventory;
    private InventoryScanner scanner;

    @BeforeClass
    public static void prepare() {
        preparator = new JarPreparator()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setName(JenkinsTest.class.getName());
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

    @Test
    public void artifactExists() throws Exception{
        assertAtLeastOneArtifact(scanner);
    }

    @Test
    public void noErrors() throws Exception {
        assertNoErrors(scanner);
    }

    @Test
    public void checkBasics() throws Exception {
        scanner.selectAllArtifacts()
                .hasSizeGreaterThan(30)
                .hasNoErrors()
        ;
    }


}
