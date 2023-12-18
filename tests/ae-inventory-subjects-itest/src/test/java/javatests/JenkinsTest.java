package javatests;

import common.JarPreparator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static inventory.dsl.predicates.IdMissmatchesVersion.idMissmatchesVersion;
import static inventory.dsl.predicates.Not.not;
import static inventory.dsl.predicates.attributes.Exists.exists;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.TYPE;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.VERSION;

public class JenkinsTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparator = new JarPreparator()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setName(JenkinsTest.class.getName());
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
    public void typesMustBeSetPredicate() {
        getScanner()
                .select(not(exists(TYPE)))
                .mustBeEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void noErrorsExist() {
        getScannerAfterInvariants()
                .select(exists(VERSION))
                .mustBeEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void versionMissmatch() {
        getScanner()
                .select(exists(VERSION))
                .hasSizeGreaterThan(0)
                .logArtifactList()
                .filter(idMissmatchesVersion)
                .mustBeEmpty();
    }

}
