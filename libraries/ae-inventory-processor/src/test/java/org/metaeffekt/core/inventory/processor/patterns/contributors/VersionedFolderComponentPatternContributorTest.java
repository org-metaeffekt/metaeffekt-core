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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.metaeffekt.core.inventory.processor.patterns.contributors.VersionedFolderComponentPatternContributor.*;

class VersionedFolderComponentPatternContributorTest {

    @Test
    public void test() {
        String samples = """
            test/freetype_2.13.3/dsds
            test/freetype_2.13.3+dfsg-1+deb13u1/dsds
            test/bzip2_1.0.8-6/dsds
            test/expat_2.8.3-1~deb13u1/dsds
            test/fontconfig_2.15.0-2.3/dsds
            test/fonts-dejavu_2.37-8/dsds
            test/freetype_2.13.3+dfsg-1+deb13u1/dsds
            test/gcc-14_14.2.0-19/dsds
            test/glib2.0_2.84.4-3~deb13u5/dsds
            test/graphite2_1.3.14-2+deb13u1/dsds
            test/harfbuzz_10.2.0-1+deb13u1/dsds
            test/lcms2_2.16-2+deb13u2/dsds
            test/libjpeg-turbo_1:2.1.5-4/dsds
            test/libpng1.6_1.6.48-1+deb13u5/dsds
            test/libxcrypt_1:4.4.38-1/dsds
            test/pcre2_10.46-1~deb13u2/dsds
            test/temurin-21-jre_21.0.12.1.0+1-0/dsds
            test/util-linux_2.41.5-0+deb13u1/dsds
            test/zlib_1:1.3.dfsg+really1.3.1-1/dsds
            test/somename-1:4.2.8p15+dfsg-1/dsds
            test/somename-1:4.2.8p15+dfsg-2~1.2.2+dfsg1-4build1/dsds
            test/somename_1:4.2.8p15+dfsg-2~1.2.2+dfsg1-4build2/dsds
            test/somename-1:4.2.8p15+dfsg-2/dsds
            test/somename-1:4.2.8p15+dfsg-3/dsds""";

        Arrays.stream(samples.split("\n")).forEach(s -> {
            boolean matches = FOLDER_VERSION_PATTERN.matcher(s).matches();
            Assertions.assertTrue(matches, s + " does not match expected pattern");
        });
    }

}