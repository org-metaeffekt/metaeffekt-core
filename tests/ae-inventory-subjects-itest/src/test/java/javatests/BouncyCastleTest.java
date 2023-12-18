package javatests;

import common.JarPreparator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static inventory.dsl.predicates.IdMissmatchesVersion.idMissmatchesVersion;

public class BouncyCastleTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparator = new JarPreparator()
                .setSource("https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/1.77/bcprov-jdk18on-1.77.jar")
                .setName(BouncyCastleTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(preparator.clear());

    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(preparator.rebuildInventory());
    }

    //TODO
    @Ignore
    @Test
    public void versionMissmatch() {
        getScannerAfterInvariants()
                .select(idMissmatchesVersion)
                .mustBeEmpty();
    }


}
