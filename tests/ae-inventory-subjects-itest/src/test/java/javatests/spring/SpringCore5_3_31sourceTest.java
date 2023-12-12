package javatests.spring;

import common.JarPreparator;
import common.Preparator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringCore5_3_31sourceTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static Preparator preparator;

    @BeforeClass
    public static void prepare(){
        preparator = new JarPreparator()
                .setSource("https://repo1.maven.org/maven2/org/springframework/spring-core/5.3.31/spring-core-5.3.31-sources.jar")
                .setName(SpringCore5_3_31sourceTest.class.getName());
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
