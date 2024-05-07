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

import java.util.Map;

public class ReportContext {

    private String id;
    private String title;
    private String context;

    public ReportContext() {
    }

    public ReportContext(String id, String title, String context) {
        this.id = id;
        this.title = title;
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
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
