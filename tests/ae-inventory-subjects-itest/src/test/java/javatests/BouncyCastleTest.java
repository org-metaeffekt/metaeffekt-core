package javatests;

import common.UrlPreparer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static inventory.dsl.predicates.IdMissmatchesVersion.idMismatchingVersion;

public class BouncyCastleTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/1.77/bcprov-jdk18on-1.77.jar")
                .setName(BouncyCastleTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(preparer.clear());

    }

    @Ignore
    @Test
    public void analyse() throws Exception {
        Assert.assertTrue(preparer.rebuildInventory());
    }

    //TODO
    @Ignore
    @Test
    public void versionMismatch() {
        getAnalysisAfterInvariants()
                .selectArtifacts(idMismatchingVersion)
                .logArtifactList("Type")
                .assertEmpty();
    }


}
