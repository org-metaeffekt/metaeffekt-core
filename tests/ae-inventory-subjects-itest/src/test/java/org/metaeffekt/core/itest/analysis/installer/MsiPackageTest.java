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
package org.metaeffekt.core.itest.analysis.installer;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.metaeffekt.core.itest.common.predicates.IdMismatchesVersion.ID_MISMATCHING_VERSION;

public class MsiPackageTest extends AbstractCompositionAnalysisTest {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://www.exemsi.com/downloads/packages/SumatraPDF/SumatraPDF-2.1.1-install.msi")
                .setSha256Hash("3d7c02cd3825fd481d1907aae2e3d5505f28b2f19db48efd3ed41f26354d73a6")
                .setName(MsiPackageTest.class.getName());
    }

    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(testSetup.clear());

    }

    @Ignore
    @Test
    public void analyse() throws Exception {
        Assert.assertTrue(testSetup.rebuildInventory());
    }

    //TODO
    @Ignore
    @Test
    public void versionMismatch() {
        getAnalysisAfterInvariantCheck()
                .selectArtifacts(ID_MISMATCHING_VERSION)
                .logList("Type")
                .assertEmpty();
    }

}
