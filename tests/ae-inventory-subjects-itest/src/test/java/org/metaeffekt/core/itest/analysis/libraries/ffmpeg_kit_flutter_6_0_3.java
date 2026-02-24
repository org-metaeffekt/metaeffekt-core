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
package org.metaeffekt.core.itest.analysis.libraries;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.metaeffekt.core.itest.common.setup.AbstractCompositionAnalysisTest;
import org.metaeffekt.core.itest.common.setup.UrlBasedTestSetup;

import static org.metaeffekt.core.inventory.processor.model.Artifact.Attribute.*;
import static org.metaeffekt.core.itest.common.predicates.AttributeValue.attributeValue;

public class ffmpeg_kit_flutter_6_0_3 extends AbstractCompositionAnalysisTest {

    @BeforeAll
    public static void prepare() {
        testSetup = new UrlBasedTestSetup()
                .setSource("https://pub.dev/api/archives/ffmpeg_kit_flutter-6.0.3.tar.gz")
                .setSha256Hash("843aae41823ca94a0988d975b4b6cdc6948744b9b7e2707d81a3a9cd237b0100")
                .setName(ffmpeg_kit_flutter_6_0_3.class.getName());
    }


    @Disabled
    @Test
    public void clear() throws Exception {
        Assertions.assertTrue(testSetup.clear());
    }

    @Disabled
    @Test
    public void inventorize() throws Exception {
        Assertions.assertTrue(testSetup.rebuildInventory());
    }

    @Test
    public void assertContent() throws Exception {
        getAnalysisAfterInvariantCheck()
                .selectArtifacts()
                .logListWithAllAttributes()
                .with(attributeValue(ID, "ffmpeg_kit_flutter-6.0.3"),
                        attributeValue(VERSION, "6.0.3"),
                        attributeValue(ROOT_PATHS, "[ffmpeg_kit_flutter-6.0.3.tar.gz]/ffmpeg_kit_flutter-6.0.3.tar"),
                        attributeValue(PURL, "pkg:pub/ffmpeg_kit_flutter@6.0.3"),
                        attributeValue(COMPONENT_SOURCE_TYPE, "pub"))
                .assertNotEmpty();
    }

}
