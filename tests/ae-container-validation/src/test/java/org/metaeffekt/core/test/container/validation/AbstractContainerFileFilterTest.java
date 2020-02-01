package org.metaeffekt.core.test.container.validation;

import org.junit.Assert;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AbstractContainerFileFilterTest {

    private final Logger LOG = LoggerFactory.getLogger(AbstractContainerFileFilterTest.class);

    protected void assertFilesFiltered(File analysisDir, List<String> excludePatterns, int extectedRemainingFileArtifacts) throws IOException {
        List<String> fileList = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);
        fileList.stream().forEach(LOG::info);
        Assert.assertEquals("Check filters in POM to exclude artifacts or adapt expectation.", extectedRemainingFileArtifacts, fileList.size());
    }

}
