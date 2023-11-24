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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.base.DescriptionParagraph</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaDescriptionParagraph {

    private final String header;
    private final String content;

    public AeaaDescriptionParagraph(String header, String content) {
        this.header = StringUtils.isEmpty(header) ? null : header;
        this.content = StringUtils.isEmpty(content) ? null : content;
    }

    public String getHeader() {
        return header;
    }

    public String getContent() {
        return content;
    }

    public boolean isEmpty() {
        return this.header == null && this.content == null;
    }

    @Override
    public String toString() {
        return toMarkdownString(1);
    }

    public String toMarkdownString(int header) {
        final StringBuilder headerBuilder = new StringBuilder();

        final boolean headerExists = StringUtils.isNotBlank(this.header);
        final boolean contentExists = StringUtils.isNotBlank(this.content);

        if (headerExists) {
            for (int i = 0; i < header; i++) {
                headerBuilder.append("#");
            }
        }

        headerBuilder
                .append(headerExists ? " " : "")
                .append(headerExists ? this.header : "")
                .append(headerExists && contentExists ? "\n" : "")
                .append(contentExists ? this.content : "");

        return headerBuilder.toString();
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("header", this.header)
                .put("content", this.content);
    }

    public static AeaaDescriptionParagraph fromTitleAndContent(String title, String content) {
        return new AeaaDescriptionParagraph(title, content);
    }

    public static AeaaDescriptionParagraph fromTitle(String title) {
        return new AeaaDescriptionParagraph(title, null);
    }

    public static AeaaDescriptionParagraph fromContent(String content) {
        return new AeaaDescriptionParagraph(null, content);
    }

    public static AeaaDescriptionParagraph fromJson(JSONObject json) {
        return new AeaaDescriptionParagraph(
                json.optString("header", null),
                json.optString("content", null)
        );
    }

    public static List<AeaaDescriptionParagraph> fromJson(JSONArray json) {
        final List<AeaaDescriptionParagraph> paragraphs = new ArrayList<>();

        if (json == null) return paragraphs;

        for (int i = 0; i < json.length(); i++) {
            paragraphs.add(fromJson(json.optJSONObject(i)));
        }

        return paragraphs;
    }
}
