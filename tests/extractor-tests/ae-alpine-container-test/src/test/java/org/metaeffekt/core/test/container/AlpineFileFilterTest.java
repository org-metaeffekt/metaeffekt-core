package org.metaeffekt.core.test.container;

import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractorUtil;
import org.metaeffekt.core.test.container.validation.AbstractContainerFileFilterTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AlpineFileFilterTest extends AbstractContainerFileFilterTest {

    @Test
    public void filterFileList() throws IOException {
        File analysisDir = new File("target/analysis");

        List<String> excludePatterns = new ArrayList<>();
        excludePatterns.add("/sys/devices/**/*");
        excludePatterns.add("/var/cache/ldconfig");
        excludePatterns.add("/var/log/**/*");
        excludePatterns.add("/lib/apk/**/*");
        excludePatterns.add("/sys/**/*");
        excludePatterns.add("/proc/**/*");
        excludePatterns.add("/root/.*");
        excludePatterns.add("/etc/**/*");
        excludePatterns.add("/.dockerenv");
        excludePatterns.add("/usr/lib/locale/locale-archive");

        assertFilesFiltered(analysisDir, excludePatterns, 0);
    }

}
