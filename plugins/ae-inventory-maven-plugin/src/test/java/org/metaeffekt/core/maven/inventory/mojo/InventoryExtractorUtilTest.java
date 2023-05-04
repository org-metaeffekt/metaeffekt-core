/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.maven.inventory.mojo;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.maven.inventory.extractor.InventoryExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InventoryExtractorUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryExtractorUtilTest.class);

    @Test
    public void testFileFilter001() throws IOException {
        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-001");
        List<String> excludesPattern = new ArrayList<>();
        Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludesPattern);

        // Prereq: symlink /bin -> /usr/bin + bash packages covers /bin/bash
        // Result: /usr/bin/bash is filtered, /bin/bash/ was never in file list
        Assert.assertFalse(filteredFiles.contains("/bin/bash"));
        Assert.assertFalse(filteredFiles.contains("/usr/bin/bash"));

        Assert.assertEquals(38241, filteredFiles.size());
    }

    @Test
    public void testFileFilter001_withExcludePattern() throws IOException {
        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-001");
        List<String> excludesPattern = new ArrayList<>();
        excludesPattern.add("/bin/apt");
        Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludesPattern);

        // Prereq: symlink /bin -> /usr/bin + /bin/apt covered by exclude pattern
        // Result: /usr/bin/apt is filtered, /bin/apt/ was never in file list
        Assert.assertFalse(filteredFiles.contains("/bin/apt"));

        Assert.assertEquals(38241, filteredFiles.size());
    }

    @Test
    public void testFileFilter001_withExcludePattern_Variant() throws IOException {
        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-001");
        List<String> excludesPattern = new ArrayList<>();
        excludesPattern.add("/bin/**/*");
        Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludesPattern);

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

        // this is a target mapped by a symlink (both files are removed; one applying the excludes filters; the second
        // applying both symlinks and exclude filters)
        excludePatterns.add("/var/lib/pam/auth");

        Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);

        LOG.debug("Filtered files size: {}", filteredFiles.size());
        if (LOG.isDebugEnabled()) {
            filteredFiles.forEach(LOG::debug);
        }

        Assert.assertEquals(42, filteredFiles.size());
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

        Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);

        LOG.debug("Filtered files size: {}", filteredFiles.size());
        filteredFiles.forEach(LOG::debug);

        Assert.assertEquals(3, filteredFiles.size());
    }

    @Test
    public void testFileFilter004() throws IOException {
        List<String> excludePatterns = new ArrayList<>();

        File analysisDir = new File("src/test/resources/InventoryExtractorUtilTest/analysis-004");

        {
            Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);
            LOG.debug("Filtered files size: {}", filteredFiles.size());
            filteredFiles.forEach(LOG::info);
            Assert.assertEquals(3, filteredFiles.size());
        }

        excludePatterns.add("/**/file-ba");

        {
            Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);
            LOG.debug("Filtered files size: {}", filteredFiles.size());
            filteredFiles.forEach(LOG::info);
            Assert.assertEquals(2, filteredFiles.size());
        }

        // exclude symlink (target)
        excludePatterns.add("/symlink-folder-bb/**/*");

        {
            Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);
            LOG.debug("Filtered files size: {}", filteredFiles.size());
            filteredFiles.forEach(LOG::info);

            Assert.assertEquals(1, filteredFiles.size());
        }

        // exclude symlink (file-level)
        excludePatterns.add("/simlink-file-bba");
        excludePatterns.remove("/symlink-folder-bb");

        {
            Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);
            LOG.debug("Filtered files size: {}", filteredFiles.size());
            filteredFiles.forEach(LOG::info);

            Assert.assertEquals(1, filteredFiles.size());
        }

    }

    @Ignore
    @Test
    public void testFileFilter_external_001() throws IOException {
        File analysisDir = new File("<path-to-analysis-folder>");
        int expectedResultSize = 1741;

        List<String> excludePatterns = new ArrayList<>();

        excludePatterns.add("/var/cache/ldconfig/**/*");
        excludePatterns.add("/var/log/**/*");
        excludePatterns.add("/var/lib/dpkg/**/*");
        excludePatterns.add("/sys/**/*");
        excludePatterns.add("/proc/**/*");
        excludePatterns.add("/root/.*");
        excludePatterns.add("/etc/**/*");
        excludePatterns.add("/.dockerenv");
        excludePatterns.add("/usr/lib/locale/locale-archive");
        excludePatterns.add("/**/*.pyc");

        excludePatterns.addAll(Arrays.asList(
                "/home/**/.local/share/containers/storage/overlay/**/*",
                "/proc/**/*",
                "/sys/devices/**/*",
                "/sys/fs/**/*",
                "/sys/kernel/**/*",
                "/var/data/db_data/**/*"));

        Collection<String> filteredFiles = InventoryExtractorUtil.filterFileList(analysisDir, excludePatterns);

        LOG.debug("Filtered files size: {}", filteredFiles.size());
        filteredFiles.forEach(LOG::debug);

        Assert.assertEquals(expectedResultSize, filteredFiles.size());
    }

}
