package org.metaeffekt.core.inventory.processor.configuration;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
public class ProcessConfigurationTest {


    @Test
    public void processConfigurationTest(){
        DemoConfiguration demoConfiguration = new DemoConfiguration();

        LinkedHashMap<String, Object> properties = demoConfiguration.getProperties();

        log.info(String.valueOf(properties));
        Assert.assertEquals(4, properties.size());

        demoConfiguration.setProperties(properties);

        List<AeaaAdvisoryTypeIdentifier<?>> advisoryTypeIdentifiers = demoConfiguration.getAdvisoryTypes();
        Assert.assertEquals(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER, advisoryTypeIdentifiers.get(0));
        Assert.assertThrows(UnsupportedOperationException.class, () -> advisoryTypeIdentifiers.add(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER));

        ArrayList<AeaaAdvisoryTypeIdentifier<?>> advisoryTypes = new ArrayList<>(advisoryTypeIdentifiers);
        advisoryTypes.add(AeaaAdvisoryTypeStore.CSAF_GENERIC_IDENTIFIER);
        demoConfiguration.setAdvisoryTypes(advisoryTypes);

        Assert.assertEquals(2, demoConfiguration.getAdvisoryTypes().size());

    }
}
