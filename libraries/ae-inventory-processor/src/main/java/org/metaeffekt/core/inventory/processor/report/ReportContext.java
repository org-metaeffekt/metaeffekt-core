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

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Getter
@Setter
public class ReportContext {

    /**
     * Used in generated output to differentiate template results for different reports.
     */
    private String id;

    /**
     * Used in generated output to differentiate section and chapters (used for prefixing captions)
     */
    // FIXME: consider renaming to something like captionPrefix
    private String title;

    /**
     * Used for providing more context to table captions; Table x (context)
     */
    // FIXME: consider renaming to something like tableCaptionContext
    private String context;

    /**
     * Fields used for BOM topic generation
     */
    private String reportInventoryName;
    private String reportInventoryVersion;

    public ReportContext(String id, String title, String context) {
        this.id = id;
        this.title = title;
        this.context = context;
    }

    /**
     * Combines a title from topic and a prepend string.
     *
     * @param topic The topic name.
     * @param prepend The string to prepend.
     *
     * @return The title string.
     */
    public String combinedTitle(String topic, boolean prepend) {
        if (title == null || StringUtils.isEmpty(title)) {
            return topic;
        } else {
            return prepend ? title + " " + topic : topic + " " + title;
        }
    }

    public String inContextOf() {
        if (context == null || StringUtils.isEmpty(context)) {
            return "";
        } else {
            return " (" + context + ")";
        }
    }

    public String map(String key, Map<String, String> map, String defaultValue) {
        if (key == null) return defaultValue;
        final String value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public String map(String key, Map<String, String> map) {
        return map(key, map, null);
    }

}
