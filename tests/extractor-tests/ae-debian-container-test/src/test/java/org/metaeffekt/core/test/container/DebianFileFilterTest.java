package org.metaeffekt.core.test.container;

import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.extractor.InventoryExtractor;
import org.metaeffekt.core.test.container.validation.AbstractContainerFileFilterTest;
import org.metaeffekt.core.util.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertNull;

public class DebianFileFilterTest extends AbstractContainerFileFilterTest {

    @Test
    public void filterFileList() throws IOException {
        File analysisDir = new File("target/analysis");

        List<String> excludePatterns = new ArrayList<>();
        excludePatterns.add("/sys/devices/**/*");
        excludePatterns.add("/var/cache/ldconfig/**/*");
        excludePatterns.add("/var/log/**/*");
        excludePatterns.add("/var/lib/dpkg/**/*");
        excludePatterns.add("/sys/**/*");
        excludePatterns.add("/proc/**/*");
        excludePatterns.add("/root/.*");
        excludePatterns.add("/etc/**/*");
        excludePatterns.add("/.dockerenv");
        excludePatterns.add("/usr/lib/locale/locale-archive");

        assertFilesFiltered(analysisDir, excludePatterns, 44);
    }

}
