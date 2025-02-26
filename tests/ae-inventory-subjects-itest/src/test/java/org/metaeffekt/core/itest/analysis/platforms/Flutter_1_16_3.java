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
package org.metaeffekt.core.itest.analysis.platforms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.itest.common.fluent.ArtifactList;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.PATH_IN_ASSET;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class Flutter_1_16_3 extends AbstractCompositionAnalysisTest{

    @BeforeClass
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://github.com/flutter/flutter/archive/refs/tags/v1.16.3.zip")
                .setSha256Hash("1c965bee13fe7be931ecb9298031e2c0cf1760788047c00ffb3f37c74b9a73d2")
                .setName(Flutter_1_16_3.class.getName());
    }


    @Ignore
    @Test
    public void clear() throws Exception {
        Assert.assertTrue(testSetup.clear());
    }

    @Ignore
    @Test
    public void inventorize() throws Exception {
        Assert.assertTrue(testSetup.rebuildInventory());
    }

    @Test
    public void assertContent() throws Exception {
        ArtifactList artifactList = getAnalysisAfterInvariantCheck()
                .selectArtifacts()
                .filter(a -> a.getVersion() != null);

        artifactList.logListWithAllAttributes();

        artifactList.with(attributeValue(ID, "snippets-0.1.0"),
                    attributeValue(VERSION, "0.1.0"),
                    attributeValue(ROOT_PATHS, "[v1.16.3.zip]/flutter-1.16.3/dev/snippets"),
                    attributeValue(PATH_IN_ASSET, "[v1.16.3.zip]/flutter-1.16.3/dev/snippets"),
                    attributeValue(PURL, "pkg:pub/snippets@0.1.0"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "ios_add2app_flutter-1.0.0+1"),
                    attributeValue(VERSION, "1.0.0+1"),
                    attributeValue(ROOT_PATHS, "[v1.16.3.zip]/flutter-1.16.3/dev/integration_tests/ios_add2app/flutterapp"),
                    attributeValue(PATH_IN_ASSET, "[v1.16.3.zip]/flutter-1.16.3/dev/integration_tests/ios_add2app/flutterapp"),
                    attributeValue(PURL, "pkg:pub/ios_add2app_flutter@1.0.0+1"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "splash_screen_load_rotate-1.0.0+1"),
                    attributeValue(VERSION, "1.0.0+1"),
                    attributeValue(ROOT_PATHS, "[v1.16.3.zip]/flutter-1.16.3/dev/integration_tests/android_splash_screens/splash_screen_load_rotate"),
                    attributeValue(PATH_IN_ASSET, "[v1.16.3.zip]/flutter-1.16.3/dev/integration_tests/android_splash_screens/splash_screen_load_rotate"),
                    attributeValue(PURL, "pkg:pub/splash_screen_load_rotate@1.0.0+1"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "vitool-0.0.1"),
                    attributeValue(VERSION, "0.0.1"),
                    attributeValue(ROOT_PATHS, "[v1.16.3.zip]/flutter-1.16.3/dev/tools/vitool"),
                    attributeValue(PATH_IN_ASSET, "[v1.16.3.zip]/flutter-1.16.3/dev/tools/vitool"),
                    attributeValue(PURL, "pkg:pub/vitool@0.0.1"))
                .assertNotEmpty();

        artifactList.with(attributeValue(ID, "image_list-1.0.0+1"),
                    attributeValue(VERSION, "1.0.0+1"),
                    attributeValue(ROOT_PATHS, "[v1.16.3.zip]/flutter-1.16.3/examples/image_list"),
                    attributeValue(PATH_IN_ASSET, "[v1.16.3.zip]/flutter-1.16.3/examples/image_list"),
                    attributeValue(PURL, "pkg:pub/image_list@1.0.0+1"))
                .assertNotEmpty();
    }

}
