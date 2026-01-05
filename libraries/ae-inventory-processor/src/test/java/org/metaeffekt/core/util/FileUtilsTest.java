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
package org.metaeffekt.core.util;

import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.metaeffekt.core.util.FileUtils.toAbsoluteOrReferencePath;

public class FileUtilsTest {

    @Test
    public void normalizeLinuxPathTest() {
        assertEquals("test", FileUtils.normalizePathToLinux("test/."));
        assertEquals(".", FileUtils.normalizePathToLinux("."));
        assertEquals(".", FileUtils.normalizePathToLinux("./"));
    }

    @Test
    public void canonicalizeLinuxPathTest() {
        assertEquals("test", FileUtils.canonicalizeLinuxPath("test"));
        assertEquals("test", FileUtils.canonicalizeLinuxPath("./test"));
        assertEquals("test", FileUtils.canonicalizeLinuxPath("././././test"));
        assertEquals("test", FileUtils.canonicalizeLinuxPath("././././test/../././test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("test/test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("test/./test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("test/././test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("test/./././test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test/./././test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test/./test/../././test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./////test/./test/../././test"));
        assertEquals("../test/test", FileUtils.canonicalizeLinuxPath("../test/./test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test//./test/../././test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test/./test//../././test"));
        assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test/./test/../././/test"));

        assertEquals("test/test/test", FileUtils.canonicalizeLinuxPath("./test/./test/test/../././/test"));
        assertEquals("/a/b", FileUtils.canonicalizeLinuxPath("/a/b/c/d/e/../../../"));
        assertEquals("/a/b", FileUtils.canonicalizeLinuxPath("/a/b/c/d/e/../../.."));

        assertEquals("test", FileUtils.canonicalizeLinuxPath("./a/../b/../c/../test"));
        assertEquals("/test", FileUtils.canonicalizeLinuxPath("/./a/../b/../c/../test"));
        assertEquals("/test", FileUtils.canonicalizeLinuxPath("/./a/.././b/.././c/.././test"));

    }

    @Test
    public void illegalPathBehaviorTest() {
        // edge cases that require a definition; better: throw exception
        assertThatIllegalStateException().isThrownBy(() -> FileUtils.canonicalizeLinuxPath("/../test"));
        assertThatIllegalStateException().isThrownBy(() -> FileUtils.canonicalizeLinuxPath("/./../test"));
        assertThatIllegalStateException().isThrownBy(() -> FileUtils.canonicalizeLinuxPath("/.././test"));
        assertThatIllegalStateException().isThrownBy(() -> FileUtils.canonicalizeLinuxPath("/./././.././test"));
        assertThatIllegalStateException().isThrownBy(() -> FileUtils.canonicalizeLinuxPath("/././././test/../.."));
        assertThatIllegalStateException().isThrownBy(() -> FileUtils.canonicalizeLinuxPath("/././././test/../../a"));

        // these normalizations should throw an exception; illegal/undefined path; please also not the OS-specific treatment
        assertThat(Paths.get("/../test").normalize().toString()).isEqualTo(File.separator + "test");
        assertThat(Paths.get("/.././test").normalize().toString()).isEqualTo(File.separator + "test");
        assertThat(Paths.get("/./../test").normalize().toString()).isEqualTo(File.separator + "test");
        assertThat(Paths.get("/./././.././test").normalize().toString()).isEqualTo(File.separator + "test");
        assertThat(Paths.get("/././././test/../..").normalize().toString()).isEqualTo(File.separator);
        assertThat(Paths.get("/././././test/../../a").normalize().toString()).isEqualTo(File.separator + "a");
    }

    @Test
    public void match() {
        String pattern = "**/md_to_pdf/**/*,/**/cache/**/md_to_pdf/**/*,/**/cache/**/md_to_pdf.*,**/md-to-pdf/**/*,**/md-to-pdf-*/**/*,/**/cache/**/md-to-pdf/**/*,/**/cache/**/md-to-pdf-*/**/*,/**/cache/**/md-to-pdf.*,**/md_to_pdf.gemspec";
        String path = "3.2.0/cache/bundler/git/md-to-pdf-db8a51cb2d2f39298e3259fa5c06fe96d67fec0b/objects/d4/4d0b959ab06938fe21bcb1150f5c2c2c05308a";
        assertThat(FileUtils.matches(pattern, path)).isEqualTo(true);
    }

    @Test
    public void pathMatching001() {
        assertThat(FileUtils.matches("**/*", "hello/world/test")).isTrue();
        assertThat(FileUtils.matches("**/*", "/hello/world/test")).isTrue();
        assertThat(FileUtils.matches("**/*", "C:/hello/world/test")).isTrue();
        assertThat(FileUtils.matches("/**/*", "/hello/world/test")).isTrue();
        assertThat(FileUtils.matches("/**/*", "C:/hello/world/test")).isTrue();
        assertThat(FileUtils.matches("/**/*", "C:/hello/world/test")).isTrue();
        assertThat(FileUtils.matches("C:/**/*", "C:/hello/world/test")).isTrue();

        assertThat(FileUtils.matches("a/b/**/*", "a/b/c")).isTrue();
        assertThat(FileUtils.matches("a/b/**/*", "a/b/c/d")).isTrue();
        assertThat(FileUtils.matches("**/a/b/**/*", "f/e/a/b/c/d")).isTrue();
        assertThat(FileUtils.matches("**/a/b/**/*", "f/e/a/b/c/d")).isTrue();

        assertThat(FileUtils.matches("**/a/b", "f/e/a/b")).isTrue();
        assertThat(FileUtils.matches("**/a/b", "f/e/a/c")).isFalse();
    }

    @Test
    public void testToAbsoluteOrExtendedFile() {
        assertThat(toAbsoluteOrReferencePath("/absolute/path","/test").getPath())
                .isEqualTo("/absolute/path");

        assertThat(toAbsoluteOrReferencePath("relative/path","/test").getPath())
                .isEqualTo("/test/relative/path");

        assertThat(toAbsoluteOrReferencePath("relative/path","test/x/..////").getPath())
                .isEqualTo("test/relative/path");

        assertThat(toAbsoluteOrReferencePath("C:/absolute/path","c:/test/x").getPath())
                .isEqualTo("C:/absolute/path");

        assertThat(toAbsoluteOrReferencePath("C:\\absolute\\path","D:\\test\\x").getPath())
                .isEqualTo("C:/absolute/path");

        // the colon maybe escaped; at the moment we cannot handle this properly
        assertThat(toAbsoluteOrReferencePath("c:test","basedir").getPath())
                .isEqualTo("basedir/c:test");

    }

}