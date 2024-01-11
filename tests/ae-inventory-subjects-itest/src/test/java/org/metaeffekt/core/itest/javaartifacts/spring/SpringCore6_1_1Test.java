package org.metaeffekt.core.itest.javaartifacts.spring;

import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.metaeffekt.core.itest.javaartifacts.TestBasicInvariants;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void clear() throws Exception{
        Assert.assertTrue(preparer.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception{
        Assert.assertTrue(preparer.rebuildInventory());
    }

    @Test
    public void first() throws Exception{
        LOG.info(preparer.getInventory().toString());
    }
}
