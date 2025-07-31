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
package org.metaeffekt.core.inventory.processor.report;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class DitaToolkitTest {

    @Test
    public void testDitaToolkitChecksum() throws IOException {
        File checksumFileParent = new File("/private/var/folders/2k/hgt151lj7jx6p_hn2z6gx9vm0000gr/T/dita/d1fb341e588a32c603fd21d8203d1ba4");
        if (checksumFileParent.exists()) {
            assertThat(Files.lines(Objects.requireNonNull(checksumFileParent.listFiles())[0].toPath())
                    .anyMatch(l -> l.equals("84264f308b7a89a2d115788d8ac39670"))).isTrue();
        }
    }
}
