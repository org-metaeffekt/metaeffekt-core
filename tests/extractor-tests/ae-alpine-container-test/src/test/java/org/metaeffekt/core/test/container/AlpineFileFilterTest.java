package org.metaeffekt.core.test.container;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.report.DependenciesDitaReport;
import org.metaeffekt.core.test.container.validation.AbstractContainerFileFilterTest;
import org.metaeffekt.core.test.container.validation.AbstractContainerValidationTest;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

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
