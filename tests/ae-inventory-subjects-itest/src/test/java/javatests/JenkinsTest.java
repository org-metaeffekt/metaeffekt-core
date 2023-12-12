package javatests;

import common.JarPreparator;
import common.Preparator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static Preparator preparator;

    @BeforeClass
    public static void prepare() throws Exception{
        preparator = new JarPreparator()
                .setSource("https://ftp.halifax.rwth-aachen.de/jenkins/war-stable/2.426.1/jenkins.war")
                .setName(JenkinsTest.class.getName());
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

    }
    @Test
    public void first() throws Exception{
        LOG.info(preparator.getInventory().toString());

    }
}
