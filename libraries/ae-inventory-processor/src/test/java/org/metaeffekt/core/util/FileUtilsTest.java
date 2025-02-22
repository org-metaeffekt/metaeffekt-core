/*
 * Copyright 2009-2024 the original author or authors.
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

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import static org.metaeffekt.core.util.FileUtils.toAbsoluteOrReferencePath;

public class FileUtilsTest {

    @Test
    public void canonicializeLinuxPathTest() {
        Assert.assertEquals("test", FileUtils.canonicalizeLinuxPath("test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("test/test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("test/./test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("test/././test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("test/./././test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test/./././test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test/./test/../././test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./////test/./test/../././test"));
        Assert.assertEquals("../test/test", FileUtils.canonicalizeLinuxPath("../test/./test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test//./test/../././test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test/./test//../././test"));
        Assert.assertEquals("test/test", FileUtils.canonicalizeLinuxPath("./test/./test/../././/test"));
    }

    @Test
    public void match() {
        String pattern = "**/md_to_pdf/**/*,/**/cache/**/md_to_pdf/**/*,/**/cache/**/md_to_pdf.*,**/md-to-pdf/**/*,**/md-to-pdf-*/**/*,/**/cache/**/md-to-pdf/**/*,/**/cache/**/md-to-pdf-*/**/*,/**/cache/**/md-to-pdf.*,**/md_to_pdf.gemspec";
        String path = "3.2.0/cache/bundler/git/md-to-pdf-db8a51cb2d2f39298e3259fa5c06fe96d67fec0b/objects/d4/4d0b959ab06938fe21bcb1150f5c2c2c05308a";
        Assertions.assertThat(FileUtils.matches(pattern, path)).isEqualTo(true);
    }

    @Test
    public void pathMatching001() {
        Assertions.assertThat(FileUtils.matches("**/*", "hello/world/test")).isTrue();
        Assertions.assertThat(FileUtils.matches("**/*", "/hello/world/test")).isTrue();
        Assertions.assertThat(FileUtils.matches("**/*", "C:/hello/world/test")).isTrue();
        Assertions.assertThat(FileUtils.matches("/**/*", "/hello/world/test")).isTrue();
        Assertions.assertThat(FileUtils.matches("/**/*", "C:/hello/world/test")).isTrue();
        Assertions.assertThat(FileUtils.matches("/**/*", "C:/hello/world/test")).isTrue();
        Assertions.assertThat(FileUtils.matches("C:/**/*", "C:/hello/world/test")).isTrue();

        Assertions.assertThat(FileUtils.matches("a/b/**/*", "a/b/c")).isTrue();
        Assertions.assertThat(FileUtils.matches("a/b/**/*", "a/b/c/d")).isTrue();
        Assertions.assertThat(FileUtils.matches("**/a/b/**/*", "f/e/a/b/c/d")).isTrue();
        Assertions.assertThat(FileUtils.matches("**/a/b/**/*", "f/e/a/b/c/d")).isTrue();

        Assertions.assertThat(FileUtils.matches("**/a/b", "f/e/a/b")).isTrue();
        Assertions.assertThat(FileUtils.matches("**/a/b", "f/e/a/c")).isFalse();
    }

    @Test
    public void testToAbsoluteOrExtendedFile() {
        Assertions.assertThat(toAbsoluteOrReferencePath("/absolute/path","/test").getPath())
                .isEqualTo("/absolute/path");

        Assertions.assertThat(toAbsoluteOrReferencePath("relative/path","/test").getPath())
                .isEqualTo("/test/relative/path");

        Assertions.assertThat(toAbsoluteOrReferencePath("relative/path","test/x/..////").getPath())
                .isEqualTo("test/relative/path");
    }

}