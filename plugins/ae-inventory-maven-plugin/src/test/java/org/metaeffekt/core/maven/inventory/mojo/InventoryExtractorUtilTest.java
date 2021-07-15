package org.metaeffekt.core.maven.inventory.mojo;

import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InventoryExtractorUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryExtractorUtilTest.class);

    @Test
    public void testFileFilter001() throws IOException {
        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-001");
        List<String> excludesPattern = new ArrayList<>();
        List<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludesPattern);

        // Prereq: symlink /bin -> /usr/bin + bash packages covers /bin/bash
        // Result: /usr/bin/bash is filtered, /bin/bash/ was never in file list
        Assert.assertFalse(filteredFiles.contains("/bin/bash"));
        Assert.assertFalse(filteredFiles.contains("/usr/bin/bash"));
    }

    @Test
    public void testFileFilter001_withExcludePattern() throws IOException {
        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-001");
        List<String> excludesPattern = new ArrayList<>();
        excludesPattern.add("/bin/apt");
        List<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludesPattern);

        // Prereq: symlink /bin -> /usr/bin + /bin/apt covered by exclude pattern
        // Result: /usr/bin/apt is filtered, /bin/apt/ was never in file list
        Assert.assertFalse(filteredFiles.contains("/bin/apt"));
    }

    @Test
    public void testFileFilter001_withExcludePattern_Variant() throws IOException {
        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-001");
        List<String> excludesPattern = new ArrayList<>();
        excludesPattern.add("/bin/**/*");
        List<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludesPattern);

        // Prereq: symlink /bin -> /usr/bin + /bin/apt covered by exclude pattern
        // Result: /usr/bin/apt is filtered, /bin/apt/ was never in file list
        Assert.assertFalse(filteredFiles.contains("/bin/apt"));
    }

    @Test
    public void testFileFilter002() throws IOException {
        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-002");
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

        List<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);

        LOG.debug("Filtered files size: {}", filteredFiles.size());
        filteredFiles.forEach(LOG::debug);

        Assert.assertEquals(44, filteredFiles.size());
    }

    @Test
    public void testFileFilter003() throws IOException {
        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-003");
        List<String> excludePatterns = new ArrayList<>();

        excludePatterns.add("/sys/devices/**/*");
        excludePatterns.add("/var/cache/apt/**/*");
        excludePatterns.add("/var/cache/debconf/**/*");
        excludePatterns.add("/var/cache/ldconfig/**/*");
        excludePatterns.add("/var/cache/ldconfig/**/*");
        excludePatterns.add("/var/lib/apt/**/*");
        excludePatterns.add("/var/lib/pam/**/*");
        excludePatterns.add("/var/lib/dpkg/**/*");
        excludePatterns.add("/var/lib/systemd/**/*.dsh-also");
        excludePatterns.add("/var/lib/systemd/**/*.timer");
        excludePatterns.add("/var/lib/ucf/**/*");
        excludePatterns.add("/var/log/**/*");
        excludePatterns.add("/sys/**/*");
        excludePatterns.add("/proc/**/*");
        excludePatterns.add("/root/.*");
        excludePatterns.add("/etc/**/*");
        excludePatterns.add("/.dockerenv");
        excludePatterns.add("/usr/lib/locale/locale-archive");
        excludePatterns.add("/usr/share/info/dir");

        List<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);

        LOG.debug("Filtered files size: {}", filteredFiles.size());
        filteredFiles.forEach(LOG::debug);

        Assert.assertEquals(1644, filteredFiles.size());
    }

}
