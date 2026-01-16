/*
 * Copyright 2009-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.inventory.processor.configuration;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeIdentifier;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.store.AeaaAdvisoryTypeStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ProcessConfigurationTest {

    @Test
    public void processConfigurationTest() {
        DemoConfiguration demoConfiguration = new DemoConfiguration();
        Map<String, Object> properties = demoConfiguration.getProperties();

        Assert.assertTrue(properties.size() >= 4);
        Assert.assertFalse(properties.containsKey("ignoreProperty"));

        demoConfiguration.setProperties(properties);

        List<AeaaAdvisoryTypeIdentifier<?>> advisoryTypeIdentifiers = demoConfiguration.getAdvisoryTypes();
        Assert.assertEquals(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER, advisoryTypeIdentifiers.get(0));
        Assert.assertThrows(UnsupportedOperationException.class, () -> advisoryTypeIdentifiers.add(AeaaAdvisoryTypeStore.OSV_GENERIC_IDENTIFIER));

        List<AeaaAdvisoryTypeIdentifier<?>> advisoryTypes = new ArrayList<>(advisoryTypeIdentifiers);
        advisoryTypes.add(AeaaAdvisoryTypeStore.CSAF_GENERIC_IDENTIFIER);
        demoConfiguration.setAdvisoryTypes(advisoryTypes);

        Assert.assertEquals(2, demoConfiguration.getAdvisoryTypes().size());

        properties = demoConfiguration.getProperties();
        properties.remove("advisoryTypes");
        properties.put("Deprecated Field Name", new ArrayList<>());

        demoConfiguration.setProperties(properties);

        advisoryTypes = demoConfiguration.getAdvisoryTypes();
        Assert.assertEquals(0, advisoryTypes.size());
    }
}
