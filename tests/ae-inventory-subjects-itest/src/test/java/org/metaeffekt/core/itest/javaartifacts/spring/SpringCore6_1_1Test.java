package org.metaeffekt.core.itest.javaartifacts.spring;

import org.apache.tools.ant.taskdefs.optional.windows.Attrib;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.metaeffekt.core.itest.javaartifacts.TestBasicInvariants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.inventory.dsl.predicates.attributes.AttributeValue.attributeValue;

public class SpringCore6_1_1Test extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("https://repo1.maven.org/maven2/org/springframework/spring-core/6.1.1/spring-core-6.1.1.jar")
                .setName(SpringCore6_1_1Test.class.getName());
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

    @Test
    public void first() throws Exception {
        getAnalysisAfterInvariants()
                .selectArtifacts()
                .logArtifactListWithAllAtributes()
                .with(attributeValue(ID, "spring-core-6.1.1.jar"),
                        attributeValue(CHECKSUM, "7a787700b8de9fc9034ffdc070517f51"),
                        attributeValue(VERSION, "6.1.1"),
                        attributeValue("Hash (SHA-256)", "a2ef6992edc54d3380ba95c56d86d1baf64afb0eda9296518be21a483318d93f"),
                        attributeValue(PROJECTS,"spring-core-6.1.1.jar"),
                        attributeValue("Path in Asset", "spring-core-6.1.1.jar")
                )
                .assertNotEmpty();
    }
}
