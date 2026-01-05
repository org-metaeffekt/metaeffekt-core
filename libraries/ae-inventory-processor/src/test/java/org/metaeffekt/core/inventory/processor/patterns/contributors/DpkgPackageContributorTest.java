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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DpkgPackageContributorTest {
    DpkgPackageContributor cpc = new DpkgPackageContributor();

    // FIXME: move test resources to component-pattern-contributor; apply conventions
    File cpcTestBaseDir = new File("src/test/resources/test-cpc-dpkg");

    @Test
    public void applies() {
        assertTrue(cpc.applies("var/lib/dpkg/status"));
    }

    private boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    @Test
    public void contribute() {

        if (isWindows()) return;

        List<ComponentPatternData> list = cpc.contribute(cpcTestBaseDir,
                "var/lib/dpkg/status", "6f72a28d5456b9a7f1af6f25f029afe9");

        boolean foundArchlessPackage = false;
        boolean foundArchPackage = false;
        for (ComponentPatternData pattern : list) {
            // all should be valid.
            assertTrue(pattern.isValid());
            assertEquals("6f72a28d5456b9a7f1af6f25f029afe9",
                    pattern.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM));
            assertEquals("1.2.3-4.5",
                    pattern.get(ComponentPatternData.Attribute.COMPONENT_VERSION));

            // check that we found both archless and arch test packages
            if ("test-package-archless".equals(pattern.get(ComponentPatternData.Attribute.COMPONENT_NAME))) {
                foundArchlessPackage = true;
            }
            if ("test-package-arch".equals(pattern.get(ComponentPatternData.Attribute.COMPONENT_NAME))) {
                foundArchPackage = true;
            }
        }

        assertTrue(foundArchlessPackage);
        assertTrue(foundArchPackage);
    }

    @Test
    @Ignore("Requires special resources and inspection.")
    public void contributeTestOnFileSystemDump() {
        List<ComponentPatternData> list = cpc.contribute(
                new File("/tmp/youthful_williamson"),
                "var/lib/dpkg/status", "dummy"
        );

        for (ComponentPatternData pattern : list) {
            System.out.println("- " + pattern.get(ComponentPatternData.Attribute.COMPONENT_NAME));
            for (ComponentPatternData.Attribute attr : ComponentPatternData.Attribute.values()) {
                System.out.println("    " + attr.name() + ": " + pattern.get(attr));
            }
            System.out.println("    - isValid: " + pattern.isValid());
        }
    }
}
