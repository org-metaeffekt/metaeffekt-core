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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.base.Reference</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaReference {

    public final static Pattern REFERENCE_STRING_PATTERN = Pattern.compile("^([^:]+): ([^(]+) \\((.+)\\)$");

    private final String title;
    private final String url;
    private final Set<String> tags = new LinkedHashSet<>();

    public AeaaReference(String title, String url) {
        if (StringUtils.isEmpty(title) || title.equals("null")) this.title = null;
        else this.title = title;
        if (StringUtils.isEmpty(url) || url.equals("null")) this.url = null;
        else this.url = url;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public Set<String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return toMarkdownString();
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();

        json.put("title", title);
        json.put("url", url);
        if (!tags.isEmpty()) {
            json.put("tags", tags);
        }

        return json;
    }

    private String getTaggedTitle() {
        return title + (!tags.isEmpty() ? " (" + String.join(", ", tags) + ")" : "");
    }

    public String toMarkdownString() {
        return "[" + getTaggedTitle() + "](" + url + ")";
    }

    public String toDisplayString() {
        if (getTaggedTitle().equals("null")) {
            return url;
        }
        return getTaggedTitle() + " " + url;
    }

    public static AeaaReference fromJson(JSONObject json) {
        if (json == null) return null;
        final AeaaReference ref = new AeaaReference(
                ObjectUtils.firstNonNull(json.optString("title", null), json.optString("type", null)),
                ObjectUtils.firstNonNull(json.optString("url", null), json.optString("ref", null))
        );

        final JSONArray tags = json.optJSONArray("tags");
        if (tags != null) tags.forEach(tag -> ref.addTag(tag.toString()));

        return ref;
    }

    public static AeaaReference fromMap(Map<String, Object> map) {
        final String name = String.valueOf(map.getOrDefault("title", null));
        final String url = String.valueOf(map.getOrDefault("url", null));

        final AeaaReference ref = new AeaaReference(name, url);

        final Object tags = map.getOrDefault("tags", null);
        if (tags instanceof Collection) {
            for (String tag : ((Collection<String>) tags)) {
                ref.addTag(tag);
            }
        }

        return ref;
    }

    public static AeaaReference fromUrl(String url) {
        return new AeaaReference(
                null,
                url
        );
    }

    public static AeaaReference fromUrlAndTags(String url, String... tags) {
        final AeaaReference reference = new AeaaReference(
                null,
                url
        );
        for (String tag : tags) {
            reference.addTag(tag);
        }
        return reference;
    }

    public static AeaaReference fromTitle(String title) {
        return new AeaaReference(
                title,
                null
        );
    }

    public static AeaaReference fromTitleAndUrl(String title, String url) {
        return new AeaaReference(
                title,
                url
        );
    }

    public static List<AeaaReference> fromJsonArray(JSONArray json) {
        if (json == null) return new ArrayList<>();

        return IntStream.range(0, json.length())
                .mapToObj(i -> {
                    final JSONObject entry = json.optJSONObject(i);
                    if (entry == null) {
                        return handleStringCase(json.optString(i, null));
                    }
                    return fromJson(entry);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static AeaaReference handleStringCase(String str) {
        if (str != null) {
            final String[] parts = str.split(" \\(");
            if (parts.length == 2) {
                return new AeaaReference(parts[0], parts[1].substring(0, parts[1].length() - 1));
            } else {
                if (str.startsWith("http")) {
                    return new AeaaReference(null, str);
                } else {
                    return new AeaaReference(str, null);
                }
            }
        }
        return null;
    }

    public static List<AeaaReference> fromJsonArray(String jsonString) {
        if (jsonString == null || !jsonString.startsWith("[")) return new ArrayList<>();

        return fromJsonArray(new JSONArray(jsonString));
    }

    public static List<AeaaReference> mergeReferences(Collection<AeaaReference>... references) {
        final List<AeaaReference> merged = new ArrayList<>();

        for (Collection<AeaaReference> referenceSet : references) {
            for (AeaaReference reference : referenceSet) {
                final AeaaReference existing = merged.stream()
                        .filter(r -> r.getUrl() != null && r.getUrl().equals(reference.getUrl()))
                        .findFirst()
                        .orElse(null);
                if (existing != null) {
                    existing.getTags().addAll(reference.getTags());
                } else {
                    merged.add(reference);
                }
            }
        }

        return merged;
    }
}
