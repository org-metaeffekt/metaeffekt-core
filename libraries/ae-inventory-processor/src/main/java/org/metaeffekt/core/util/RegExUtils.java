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
package org.metaeffekt.core.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Common utils for dealing with regular expressions.
 */
public class RegExUtils {

    /**
     * Replaces the given regular expression with in the text by the specified replacement.
     *
     * This implementation applies the regular expression / replacement as often as a change is observed.
     *
     * @param text The text to apply the regular expression to.
     * @param regex The regular expression.
     * @param replacement The replacement string.
     *
     * @return The replaced text. If the regular expression does not match
     */
    public static String replaceAll(String text, String regex, String replacement) {
        Set<String> previousVersions = new HashSet<>();
        do {
            previousVersions.add(text);
            text = text.replaceAll(regex, replacement);
        } while (!previousVersions.contains(text));

        return text;
    }

    /**
     * Replaces markdown constructs to html. The function is limited but covers the currently required cases.
     *
     * @param text The text with markup.
     *
     * @return Html formatted fragments.
     */
    public static String simpleMarkdownToHtml(String text) {
        text = insertListStructure(text);
        text = applySimpleMarkdown(text);
        return text;
    }

    private static String insertListStructure(String text) {
        // bullet
        text = replaceAll(text, "(?m)^\\s*[\\-\\*-]{1} ([^\\s]{1}.*)$", "<l1b><li>$1</li></l1b>");
        text = replaceAll(text, "(?m)^\\s*[\\-\\*-]{2} ([^\\s]{1}.*)$", "<l2b><li>$1</li></l2b></li></l1b>");
        text = replaceAll(text, "(?m)^\\s*[\\-\\*-]{3} ([^\\s]{1}.*)$", "<l3b><li>$1</li></l3b></li></l2b></li></l1b>");

        // numbered
        text = replaceAll(text, "(?m)^\\s*[\\+]{1} ([^\\s]{1}.*)$", "<l1n><li>$1</li></l1n>");
        text = replaceAll(text, "(?m)^\\s*[\\+]{2} ([^\\s]{1}.*)$", "<l2n><li>$1</li></l2n></li></l1n>");
        text = replaceAll(text, "(?m)^\\s*[\\+]{3} ([^\\s]{1}.*)$", "<l3n><li>$1</li></l3n></li></l2n></li></l1n>");

        text = replaceAll(text, "<\\/l2.>\\s*<\\/li>\\s*<\\/l1.>\\s*(<l3.>)", "$1");
        text = replaceAll(text, "<\\/l1.>\\s*(<l2.>)", "$1");
        text = replaceAll(text, "<\\/li>\\s*(<l2.>)", "$1");
        text = replaceAll(text, "<\\/l2.>\\s*(<l3.>)", "$1");
        text = replaceAll(text, "<\\/li>\\s*(<l3.>)", "$1");

        // eliminate tags on the same level
        text = replaceAll(text, "<\\/l1.>\\s*<l1.>", "");
        text = replaceAll(text, "<\\/l2.>\\s*<l2.>", "");
        text = replaceAll(text, "<\\/l3.>\\s*<l3.>", "");

        // eliminate tags on the same level (bullet)
        text = replaceAll(text, "<l1b>", "<ul>");
        text = replaceAll(text, "<\\/l1b>", "</ul>");
        text = replaceAll(text, "<l2b>", "<ul>");
        text = replaceAll(text, "<\\/l2b>", "</ul>");
        text = replaceAll(text, "<l3b>", "<ul>");
        text = replaceAll(text, "<\\/l3b>", "</ul>");

        // TEMP-WORKAROUND: eliminate tags on the same level (ordered; workaround: alos bullet)
        final String bulletOpenTag = "<ul>";
        final String bulletCloseTag = "</ul>";
        text = replaceAll(text, "<l1n>", bulletOpenTag);
        text = replaceAll(text, "<\\/l1n>", bulletCloseTag);
        text = replaceAll(text, "<l2n>", bulletOpenTag);
        text = replaceAll(text, "<\\/l2n>", bulletCloseTag);
        text = replaceAll(text, "<l3n>", bulletOpenTag);
        text = replaceAll(text, "<\\/l3n>", bulletCloseTag);

        return text;
    }

    private static String applySimpleMarkdown(String text) {
        // ease boundary conditions
        text = " " + text + " ";

        final String prefixGroup = "([\\s\\.,;:_<>#\\+\\*]+)";
        final String contextGroup = "(.*?)";
        final String suffixGroup = prefixGroup;

        // {{ }} to tt
        text = replaceAll(text, "\\{\\{(.*?)\\}\\}", "<tt>$1</tt>");

        // special case italics
        text = replaceAll(text, prefixGroup + "\\*\\*" + contextGroup + "\\*\\*" + suffixGroup, "$1<i>$2</i>$3");

        // italics
        text = replaceAll(text, prefixGroup + "_" + contextGroup + "_" + suffixGroup, "$1<i>$2</i>$3");
        text = replaceAll(text, "\\{_\\}" + contextGroup + "_" + suffixGroup, "<i>$1</i>$2");
        text = replaceAll(text, "\\{_\\}" + contextGroup + "\\{_\\}", "<i>$1</i>");

        // bold
        text = replaceAll(text, prefixGroup + "\\*" + contextGroup + "\\*" + suffixGroup, "$1<b>$2</b>$3");
        text = replaceAll(text, "\\{\\*\\}" + contextGroup + "\\*" + suffixGroup, "<b>$1</b>$2");
        text = replaceAll(text, "\\{\\*\\}" + contextGroup + "\\{\\*\\}", "<b>$1</b>");

        // underline
        text = replaceAll(text, prefixGroup + "\\+" + contextGroup + "\\+" + suffixGroup, "$1<u>$2</u>$3");
        text = replaceAll(text, "\\{\\+\\}" + contextGroup + "\\+" + suffixGroup, "<u>$1</u>$2");
        text = replaceAll(text, "\\{\\+\\}" + contextGroup + "\\{\\+\\}", "<u>$1</u>");

        // strikethrough
        text = replaceAll(text, prefixGroup + "\\-" + contextGroup + "\\-" + suffixGroup, "$1<s>$2</s>$3");
        text = replaceAll(text, "\\{\\-\\}" + contextGroup + "\\-" + suffixGroup, "<s>$1</s>$2");
        text = replaceAll(text, "\\{\\-\\}" + contextGroup + "\\{\\-\\}", "<s>$1</s>");

        return text.substring(1, text.length() -1);
    }

}
