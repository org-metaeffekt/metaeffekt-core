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
package org.metaeffekt.core.inventory;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class InventoryUtilsTest {

    @Test
    public void testTokenizeLicense() {
        assertTokenizeCycleMatchesInput(
                "BSD 2-Clause License (copyright holder variant), " +
                "BSD 3-Clause License (copyright holder variant), " +
                "BSD 3-Clause License (variant 001), " +
                "BSD 4.3 TAHOE License, " +
                "Cygwin Exception (GPL 3.0+), " +
                "Digital Mars Zlib License, " +
                "Spencer License (4-Clause), " +
                "TCL License (terms only), " +
                "Unicode DFS License, " +
                "zlib License",
                false, false);

        assertTokenizeCycleMatchesInput(
                "Cygwin Exception (GPL 3.0+)",
                false, false);

        assertTokenizeCycleMatchesInput(
                "BSD 2-Clause License (copyright holder variant), " +
                "BSD 3-Clause License (copyright holder variant), " +
                "BSD 4-Clause License (copyright holder variant), " +
                "Cygwin Exception (GPL 3.0+), " +
                "GNU Free Documentation License 1.2 (with GCC Runtime Library exception 3.1), " +
                "zlib License",
                false, true);

        assertTokenizedCycleMatches(
                "BSD 2-Clause License (copyright holder variant), " +
                "BSD 3-Clause License (copyright holder variant), " +
                "BSD 4-Clause License (copyright holder variant), " +
                "Cygwin Exception (GPL 3.0+), " +
                "GNU Free Documentation License 1.2 (with GCC Runtime Library exception 3.1), " +
                "zlib License",
                "BSD 2-Clause License (copyright holder variant), " +
                "BSD 3-Clause License (copyright holder variant), " +
                "BSD 4-Clause License (copyright holder variant), " +
                "Cygwin Exception (GPL 3.0+), " +
                "GNU Free Documentation License 1.2 (with GCC Runtime Library exception 3.1), " +
                "zlib License",
                false, true);

        assertTokenizedCycleMatches(
                "BSD 2-Clause License (copyright holder variant), " +
                "BSD 3-Clause License (copyright holder variant), " +
                "BSD 4-Clause License (copyright holder variant), " +
                "Cygwin Exception (GPL 3.0+), " +
                "GNU Free Documentation License 1.2 (with GCC Runtime Library exception 3.1), " +
                "zlib License",
                "BSD 2-Clause License (copyright holder variant), " +
                "BSD 3-Clause License (copyright holder variant), " +
                "BSD 4-Clause License (copyright holder variant), " +
                "Cygwin Exception (GPL 3.0+), " +
                "GNU Free Documentation License 1.2 (with GCC Runtime Library exception 3.1), " +
                "zlib License",
                false, false);
    }

    private void assertTokenizedCycleMatches(String input, String expected, boolean reorder, boolean commaSeparatorOnly) {
        final List<String> licenses = InventoryUtils.tokenizeLicense(input, reorder, false);
        Assertions.assertThat(InventoryUtils.joinLicenses(licenses)).isEqualTo(expected);
    }

    private void assertTokenizeCycleMatchesInput(String licenseExpression, boolean reorder, boolean commaSeparatorOnly) {
        final List<String> licenses = InventoryUtils.tokenizeLicense(licenseExpression, reorder, commaSeparatorOnly);
        Assertions.assertThat(InventoryUtils.joinLicenses(licenses)).isEqualTo(licenseExpression);
    }




}