package javatests.spring;

import common.JarPreparator;
import javatests.TestBasicInvariants;
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
        preparator = new JarPreparator()
                .setSource("https://repo1.maven.org/maven2/org/springframework/spring-core/6.1.1/spring-core-6.1.1.jar")
                .setName(SpringCore6_1_1Test.class.getName());
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
