package javatests;

import common.UrlPreparer;
import inventory.dsl.predicates.attributes.Exists;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static inventory.dsl.predicates.IdMissmatchesVersion.idMismatchingVersion;
import static inventory.dsl.predicates.Not.not;
import static inventory.dsl.predicates.TrivialPredicates.trivialReturnAllElements;
import static inventory.dsl.predicates.TrivialPredicates.trivialReturnNoElements;
import static inventory.dsl.predicates.attributes.Exists.withAttribute;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;

public class JenkinsTest extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setName(JenkinsTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(preparer.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(preparer.rebuildInventory());
    }


    //TODO
    @Ignore
    @Test
    public void typesMustBeSetPredicate() {
        getAnalysis()
                .selectArtifacts(not(Exists.withAttribute(TYPE)))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void noErrorsExist() {
        getAnalysisAfterInvariants()
                .selectArtifacts(withAttribute("Errors"))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void versionMissmatch() {
        getAnalysis()
                .selectArtifacts(withAttribute(VERSION))
                .assertNotEmpty()
                .logArtifactList()
                .assertEmpty(idMismatchingVersion);
    }

    @Ignore
    @Test
    public void trivialPredicates() {
        getAnalysis()
                .selectArtifacts(not(withAttribute(CHECKSUM)))
                .assertEmpty(trivialReturnNoElements)
                .assertNotEmpty(trivialReturnAllElements)
                .logArtifactListWithAllAtributes()
                .logInfo()
                .logInfo("Typed List:")
                .filter(withAttribute(TYPE)).as("Artifact has Type")
                .assertNotEmpty()
                .logArtifactListWithAllAtributes();
    }

}
