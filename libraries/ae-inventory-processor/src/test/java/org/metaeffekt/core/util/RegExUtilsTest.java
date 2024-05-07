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
import org.junit.Test;

import static org.metaeffekt.core.util.RegExUtils.replaceAll;

public class RegExUtilsTest {

    @Test
    public void testMarkdownToHtml() {
        Assertions.assertThat("xxyxx".replaceAll("xyx", "y")).isEqualTo("xyx");
        Assertions.assertThat(replaceAll("xxyxx", "xyx", "y")).isEqualTo("y");
    }
}