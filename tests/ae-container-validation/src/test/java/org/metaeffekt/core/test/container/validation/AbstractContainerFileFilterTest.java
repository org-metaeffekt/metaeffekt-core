package org.metaeffekt.core.test.container.validation;

import org.junit.Assert;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class AbstractContainerFileFilterTest {

    private final Logger LOG = LoggerFactory.getLogger(AbstractContainerFileFilterTest.class);

    protected void assertFilesFiltered(File analysisDir, List<String> excludePatterns, int expectedRemainingFileArtifacts) throws IOException {
        Collection<String> fileList = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);
        Assert.assertEquals("Check filters in POM to exclude artifacts or adapt expectation.", expectedRemainingFileArtifacts, fileList.size());
    }

    protected void assertCommonFileAttributes(Artifact artifact) {
        Set<String> paths = artifact.getRootPaths();
        assertTrue("No artifact root path is set for file artifact " + artifact.getId(), paths != null && paths.size() == 1);
    }

}
