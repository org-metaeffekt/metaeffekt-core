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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ProcessConfigurationTest {

    @Test
    public void processConfigurationTest() {
        final DemoConfiguration demoConfiguration = new DemoConfiguration();
        Map<String, Object> properties = demoConfiguration.getProperties();

        Assertions.assertTrue(properties.size() >= 4);
        Assertions.assertFalse(properties.containsKey("ignoreProperty"));

        demoConfiguration.setProperties(properties);

        final List<DemoConfiguration.CustomAdvisoryTypeIdentifier> advisoryTypeIdentifiers = demoConfiguration.getAdvisoryTypes();
        Assertions.assertEquals(DemoConfiguration.OSV_GENERIC_IDENTIFIER, advisoryTypeIdentifiers.get(0));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> advisoryTypeIdentifiers.add(DemoConfiguration.OSV_GENERIC_IDENTIFIER));

        List<DemoConfiguration.CustomAdvisoryTypeIdentifier> advisoryTypes = new ArrayList<>(advisoryTypeIdentifiers);
        advisoryTypes.add(DemoConfiguration.CSAF_GENERIC_IDENTIFIER);
        demoConfiguration.setAdvisoryTypes(advisoryTypes);

        Assertions.assertEquals(2, demoConfiguration.getAdvisoryTypes().size());

        properties = demoConfiguration.getProperties();
        properties.remove("advisoryTypes");
        properties.put("Deprecated Field Name", new ArrayList<>());

        demoConfiguration.setProperties(properties);

        advisoryTypes = demoConfiguration.getAdvisoryTypes();
        Assertions.assertEquals(0, advisoryTypes.size());
    }
}
