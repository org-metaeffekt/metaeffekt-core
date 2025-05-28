/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.report.configuration;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.security.cvss.processor.CvssSelectionResult;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class CspLoaderTest {
    private final static File RESOURCE_DIR = new File("src/test/resources/security-policy/CspLoaderTest");

    @Test
    public void initialTest() throws IOException {
        final CspLoader loader = new CspLoader();
        loader.getPolicyFiles().add(new File(RESOURCE_DIR, "initialTest/file-a.json"));
        loader.getPolicyFiles().add(new File(RESOURCE_DIR, "initialTest/file-b.json"));

        loader.setActiveIds(Arrays.asList("config d", "config a"));
        final CentralSecurityPolicyConfiguration resultPolicy1 = loader.loadConfiguration();

        Assert.assertEquals(Collections.singletonList("notice"), resultPolicy1.getIncludeAdvisoryTypes());
        Assert.assertEquals(5, resultPolicy1.getIncludeScoreThreshold(), 0.01);
        Assert.assertEquals(7.8, resultPolicy1.getInsignificantThreshold(), 0.01);
        Assert.assertEquals(Collections.singletonList(CvssSelectionResult.CvssScoreVersionSelectionPolicy.HIGHEST), resultPolicy1.getCvssVersionSelectionPolicy());
        Assert.assertEquals(CentralSecurityPolicyConfiguration.JsonSchemaValidationErrorsHandling.LENIENT, resultPolicy1.getJsonSchemaValidationErrorsHandling());

        loader.setActiveIds(Arrays.asList("config a", "config d"));
        final CentralSecurityPolicyConfiguration resultPolicy2 = loader.loadConfiguration();

        Assert.assertEquals(Collections.singletonList("alert"), resultPolicy2.getIncludeAdvisoryTypes());
        Assert.assertEquals(5, resultPolicy2.getIncludeScoreThreshold(), 0.01);
        Assert.assertEquals(7.8, resultPolicy2.getInsignificantThreshold(), 0.01);
        Assert.assertEquals(Collections.singletonList(CvssSelectionResult.CvssScoreVersionSelectionPolicy.HIGHEST), resultPolicy2.getCvssVersionSelectionPolicy());
        Assert.assertEquals(CentralSecurityPolicyConfiguration.JsonSchemaValidationErrorsHandling.LENIENT, resultPolicy2.getJsonSchemaValidationErrorsHandling());
    }


    @Test
    public void invalidKeysTest() {
        Assert.assertThrows(RuntimeException.class, () -> {
            final CspLoader loader = new CspLoader();
            loader.getPolicyFiles().add(new File(RESOURCE_DIR, "invalidTest/keys-1.json"));
            loader.loadConfiguration();
        });
        Assert.assertThrows(RuntimeException.class, () -> {
            final CspLoader loader = new CspLoader();
            loader.getPolicyFiles().add(new File(RESOURCE_DIR, "invalidTest/keys-2.json"));
            loader.loadConfiguration();
        });
        Assert.assertThrows(RuntimeException.class, () -> {
            final CspLoader loader = new CspLoader();
            loader.getPolicyFiles().add(new File(RESOURCE_DIR, "invalidTest/keys-3.json"));
            loader.loadConfiguration();
        });
    }
}