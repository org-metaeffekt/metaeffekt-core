package org.metaeffekt.core.itest.javatests;

import org.metaeffekt.core.itest.common.UrlPreparer;
import org.metaeffekt.core.itest.inventory.dsl.predicates.attributes.Exists;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.inventory.dsl.predicates.IdMissmatchesVersion;
import org.metaeffekt.core.itest.inventory.dsl.predicates.Not;
import org.metaeffekt.core.itest.inventory.dsl.predicates.TrivialPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.itest.inventory.dsl.predicates.attributes.Exists.withAttribute;
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
                .selectArtifacts(Not.not(Exists.withAttribute(TYPE)))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void noErrorsExist() {
        getAnalysisAfterInvariants()
                .selectArtifacts(Exists.withAttribute("Errors"))
                .assertEmpty();
    }

    //TODO
    @Ignore
    @Test
    public void versionMissmatch() {
        getAnalysis()
                .selectArtifacts(Exists.withAttribute(VERSION))
                .assertNotEmpty()
                .logArtifactList()
                .assertEmpty(IdMissmatchesVersion.idMismatchingVersion);
    }

    @Ignore
    @Test
    public void trivialPredicates() {
        getAnalysis()
                .selectArtifacts()
                .assertEmpty(TrivialPredicates.trivialReturnNoElements)
                .assertNotEmpty(TrivialPredicates.trivialReturnAllElements)
                .logArtifactListWithAllAtributes()
                .logInfo()
                .logInfo("Typed List:")
                .filter(Exists.withAttribute(TYPE))
                .as("Artifact has Type")
                .assertNotEmpty()
                .logArtifactListWithAllAtributes();

    }

    @Ignore
    @Test
    public void namedLists() {
        getAnalysis()
                .selectArtifacts()
                .logInfo("Typed List:")
                .filter(Exists.withAttribute(TYPE)).as("Artifact has Type")
                .assertNotEmpty()
                .logArtifactListWithAllAtributes();

    }

}
