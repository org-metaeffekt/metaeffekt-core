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

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class ReportUtils {

    public boolean isEmpty(String value) {
        return !StringUtils.isNotBlank(value);
    }

    public boolean notEmpty(String value) {
        return StringUtils.isNotBlank(value);
    }

    public String ratio(long part, long total) {
        return String.format("%.1f", ((double) part) / total);
    }

    public String percent(long part, long total) {
        if (total == 0) return "n/a";
        return String.format(Locale.GERMANY, "%.1f %%", (100d * part) / total);
    }

}
