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
package org.metaeffekt.core.inventory.processor.report;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class PreFormattedEscapeUtils {

    private final Map<String, String> symbols = new HashMap<>();
    private final Map<String, String> characters = new HashMap<>();

    // escaped Pair.of(start, end) --> unescaped Pair.of(start, end)
    // e.g. Pair.of("&lt;li&gt;", "&lt;/li&gt;") --> Pair.of("<li>", "</li>")
    private final Map<Pair<String, String>, Pair<String, String>> balancedTags = new HashMap<>();

    public PreFormattedEscapeUtils() {
        putEscaped(symbols, "&copy;");

        putEscaped(characters, "&lt;");
        putEscaped(characters, "&gt;");

        putBalancedEscaped(balancedTags, "<lq>", "</lq>");

        putBalancedEscaped(balancedTags, "<p>", "</p>");
        putBalancedEscaped(balancedTags, "<p>", "<p/>");

        putBalancedEscaped(balancedTags, "<ol>", "</ol>");
        putBalancedEscaped(balancedTags, "<ul>", "</ul>");
        putBalancedEscaped(balancedTags, "<li>", "</li>");

        putBalancedEscaped(balancedTags, "<i>", "</i>");
        putBalancedEscaped(balancedTags, "<b>", "</b>");
        putBalancedEscaped(balancedTags, "<codeph>", "</codeph>");

        putBalancedEscaped(balancedTags, "<p><b>", "</b></p>");
        putBalancedEscapedAndRedefine(balancedTags, "<p><b>", "</b></p>", "<h3>", "</h3>");
        putBalancedEscapedAndRedefine(balancedTags, "<p><b>", "</b></p>", "<h2>", "</h2>");
        putBalancedEscapedAndRedefine(balancedTags, "<p><b>", "</b></p>", "<h1>", "</h1>");
    }

    private void putEscaped(Map<String, String> map, String value) {
        map.put(value, StringEscapeUtils.escapeXml(value));
    }

    private void putBalancedEscaped(Map<Pair<String, String>, Pair<String, String>> map, String startTag, String endTag) {
        map.put(Pair.of(StringEscapeUtils.escapeXml(startTag), StringEscapeUtils.escapeXml(endTag)), Pair.of(startTag, endTag));
    }

    private void putBalancedEscapedAndRedefine(Map<Pair<String, String>, Pair<String, String>> map, String replacementStartTag, String replacementEndTag, String startTag, String endTag) {
        map.put(Pair.of(StringEscapeUtils.escapeXml(startTag), StringEscapeUtils.escapeXml(endTag)), Pair.of(replacementStartTag, replacementEndTag));
    }

    public String xml(String string) {
        if (string == null) return null;

        // escape all xml
        String escaped = StringEscapeUtils.escapeXml(string);

        // unescape in certain cases

        // pass 1: balanced start and end tags
        for (Map.Entry<Pair<String, String>, Pair<String, String>> entry : balancedTags.entrySet()) {
            if (isBalanced(escaped, entry.getKey().getLeft(), entry.getKey().getRight())) {
                escaped = applyUnescape(escaped, entry.getKey().getLeft(), entry.getValue().getLeft());
                escaped = applyUnescape(escaped, entry.getKey().getRight(), entry.getValue().getRight());
            }
        }

        // pass 2: symbols
        for (Map.Entry<String, String> entry : symbols.entrySet()) {
            escaped = applyUnescape(escaped, entry.getValue(), entry.getKey());
        }

        // pass 3: chars
        for (Map.Entry<String, String> entry : characters.entrySet()) {
            escaped = applyUnescape(escaped, entry.getValue(), entry.getKey());
        }

        return escaped;
    }

    private boolean isBalanced(String string, String startTag, String endTag) {
        if (string == null) return false;
        if (!string.contains(startTag)) return false;
        if (!string.contains(endTag)) return false;

        int weight = 0;

        for (int i = 0; i < string.length(); i++) {
            if (string.startsWith(startTag, i)) {
                weight++;
                i += startTag.length() - 1;
            } else if (string.startsWith(endTag, i)) {
                weight--;
                i += endTag.length() - 1;
            }
            if (weight < 0) {
                return false;
            }
        }

        return weight == 0;
    }

    private String applyUnescape(String string, String find, String replace) {
        return string.replace(find, replace);
    }

    /**
     * Replaces the <code>@</code> character with <code>%40</code>.
     *
     * @param string The string to be processed.
     * @return The processed string.
     * @deprecated Method should either be renamed or removed.
     */
    @Deprecated
    public Object purl(String string) {
        return string.replace("@", "%40");
    }
}
