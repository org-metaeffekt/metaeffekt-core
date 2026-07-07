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
package org.metaeffekt.core.test.container.validation;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractContainerFileFilterTest {

    private final Logger LOG = LoggerFactory.getLogger(AbstractContainerFileFilterTest.class);

    protected void assertFilesFiltered(File analysisDir, List<String> excludePatterns, int expectedRemainingFileArtifacts) throws IOException {
        Collection<String> fileList = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);
        assertEquals(expectedRemainingFileArtifacts, fileList.size(), "Check filters in POM to exclude artifacts or adapt expectation.");
    }

    protected void assertCommonFileAttributes(Artifact artifact) {
        Set<String> paths = artifact.getRootPaths();
        assertTrue(paths != null && paths.size() == 1, "No artifact root path is set for file artifact " + artifact.getId());
    }

}
