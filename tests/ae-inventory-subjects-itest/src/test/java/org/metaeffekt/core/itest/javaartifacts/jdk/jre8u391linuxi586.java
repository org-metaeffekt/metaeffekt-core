package org.metaeffekt.core.itest.javaartifacts.jdk;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.common.download.UrlPreparer;
import org.metaeffekt.core.itest.javaartifacts.TestBasicInvariants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class jre8u391linuxi586 extends TestBasicInvariants {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    //TODO specify file url schema for static / proxied ressources if needed

    @BeforeClass
    public static void prepare() {
        preparer = new UrlPreparer()
                .setSource("file:///home/tobi/Downloads/jre-8u391-linux-i586.tar.gz")
                .setName(jre8u391linuxi586.class.getName());
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

}
