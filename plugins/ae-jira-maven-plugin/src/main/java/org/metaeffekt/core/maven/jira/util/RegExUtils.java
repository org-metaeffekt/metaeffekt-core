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
package org.metaeffekt.core.maven.jira.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Common utils for dealing with regular expressions.
 */
public class RegExUtils {

    public static final String DITA_NEWLINE = "<lines><line>&nbsp;</line></lines>";

    public static final String DITA_NEWLINE_REGEXP = DITA_NEWLINE;

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
     * Replaces markdown constructs to DITA. The function is limited but covers the currently required cases.
     *
     * @param text The text with markup.
     *
     * @return DITA formatted fragments.
     */
    public static String simpleMarkdownToDita(String text) {
        text = preserveFormatting(text);
        text = eliminateUnsupportedConstructs(text);
        text = applySimpleMarkdown(text);
        text = enhanceFormatting(text);
        text = insertListStructure(text);
        text = manageNewlines(text);
        text = adjustXmlTags(text);
        return text;
    }

    public static String eliminateCodeBlocks(String ditaFragment, String replacement) {
        ditaFragment = replaceAll(ditaFragment, "(?m)<codeblock[^>]*>[.\\s\\S]*?</codeblock>", "<codeblock>" + replacement + "</codeblock>");
        return ditaFragment;
    }

    private static String manageNewlines(String text) {
        // mark newlines as single carriage return
        String newText = replaceAll(text,"\r*\n", DITA_NEWLINE);

        // eliminate wrongly introduced <lines/> in codeblocks
        newText = replaceAll(newText, "(?m)(<codeblock[^>]*>[.\\s\\S]*?)(" + DITA_NEWLINE_REGEXP + ")([.\\s\\S]*?</codeblock>)", "$1\r\n$3");

        // collapse subsequent newlines into one
        newText = replaceAll(newText, DITA_NEWLINE_REGEXP + DITA_NEWLINE_REGEXP, DITA_NEWLINE);

        return newText;
    }

    private static String eliminateUnsupportedConstructs(String text) {
        text = text.replaceAll("(?m)^\\-\\-\\-*$", "\n");
        text = text.replaceAll("(?m)^\\- \\- \\-( \\-)*$", "\n");
        text = text.replaceAll("!image.*?!", "&lt;image not displayed&gt;");
        text = text.replaceAll("\\{color:.*?}", "");
        text = text.replaceAll("\\{color}", "");
        return text;
    }

    private static String enhanceFormatting(String text) {
        text = text.replaceAll("\\:\\*", ":\n*");
        text = text.replaceAll("\\.\\*", ":\n*");
        text = text.replaceAll("\\:\\+", ":\n+ ");
        text = text.replaceAll("\\.\\+", ":\n+ ");
        text = text.replaceAll("\\:\\- ", ":\n- ");
        text = text.replaceAll("\\.<b>", ". <b>");
        return text;
    }

    private static String preserveFormatting(String text) {
        text = replaceAll(text,
                "(?m)\\{noformat}(\\s*[.\\s\\S]*?)[\\s]*\\{noformat}",
                "<codeblock>$1</codeblock>");
        text = replaceAll(text,
                "(?m)\\{code}(\\s*[.\\s\\S]*?)[\\s]*\\{code}",
                "<codeblock>$1</codeblock>");
        text = replaceAll(text,
                "(?m)\\{code:(.*)}(\\s*[.\\s\\S]*?)[\\s]*\\{code}",
                "<codeblock otherprops=\"lang($1)\">$2</codeblock>");
        return text;
    }

    private static Set<String> structuralTags = new HashSet<>();

    private static Set<String> nonStructuralTags = new HashSet<>();

    static {
        structuralTags.add("ul");
        structuralTags.add("ol");
        structuralTags.add("li");
        structuralTags.add("p");
        structuralTags.add("div");
        structuralTags.add("span");
        structuralTags.add("table");
        structuralTags.add("td");
        structuralTags.add("th");
        structuralTags.add("thead");
        structuralTags.add("q");

        nonStructuralTags.add("b");
        nonStructuralTags.add("s");
        nonStructuralTags.add("st");
        nonStructuralTags.add("strong");
        nonStructuralTags.add("i");
        nonStructuralTags.add("tt");
        nonStructuralTags.add("u");
    }

    private static class TagMatch {

        String tag = null;

        int index = -1;

        boolean isClosing = false;

        int endIndex = -1;

        @Override
        public String toString() {
            return "TagMatch{" +
                    "tag='" + tag + '\'' +
                    ", index=" + index +
                    ", isClosing=" + isClosing +
                    ", endIndex=" + endIndex +
                    '}';
        }
    }

    public static String adjustXmlTags(String text) {
        // observed tags are managed in a stack
        Stack<TagMatch> tagStack = new Stack<>();

        // while moving throught the text string we operated with an offset
        int offset = 0;

        // variable having the current tag match
        TagMatch tagMatch;

        do {
            // peek last, if exists; otherwise null
            final TagMatch previousTag = tagStack.isEmpty() ? null: tagStack.peek();

            // find next tag match stating from current offset; null in case no tag was found
            tagMatch = matchNextTag(text, offset);

            if (tagMatch != null) {

                if (tagMatch.isClosing) {

                    // detected unexpected closing tag
                    if (previousTag == null || !tagMatch.tag.equalsIgnoreCase(previousTag.tag)) {
                        if (structuralTags.contains(tagMatch.tag)) {
                            // eliminate superfluous structural tag
                            text = text.substring(0, tagMatch.index) +
                                    text.substring(tagMatch.endIndex);

                            // don't push the tag; just continue; offset is the same
                            continue;
                        } else {
                            // handle non-structural tags
                            if (previousTag != null) {
                                // insert non-structural tag matching previous
                                text = text.substring(0, tagMatch.index) +
                                        "</" + previousTag.tag + ">" +
                                        text.substring(tagMatch.index);

                                // don't push the tag; just continue; offset is the same
                                continue;
                            } else {
                                // eliminate non-structural tag
                                text = text.substring(0, tagMatch.index) +
                                        text.substring(tagMatch.endIndex);

                                // don't push the tag; just continue; offset is the same
                                continue;
                            }
                        }
                    }

                    // closing tag closes open tag; ok; pop and continue with adjusted offest
                    if (previousTag != null && tagMatch.tag.equalsIgnoreCase(previousTag.tag)) {
                        // pop previous (as it was closed)
                        tagStack.pop();
                        offset = tagMatch.endIndex;
                    } else {
                        // otherwise push next tag
                        tagStack.push(tagMatch);
                        offset = tagMatch.endIndex;
                    }
                } else {
                    tagStack.push(tagMatch);
                    offset = tagMatch.endIndex;
                }
            }
        } while (tagMatch != null);

        if (!tagStack.isEmpty()) {
            // Unclosed items on the tagStack; empty stack and appending closing tags
            final StringBuilder sb = new StringBuilder();
            while (!tagStack.isEmpty()) {
                tagMatch = tagStack.pop();
                text += "</" + tagMatch.tag + ">";
            }
            text += sb.toString();
        }

        return text;
    }

    private static TagMatch matchNextTag(String fullText, int startIndex) {
        String text = fullText.substring(startIndex);
        TagMatch tagMatch = new TagMatch();
        for (String tag : structuralTags) {
            matchNextOpeningTag(text, tag, tagMatch);
            matchNextClosingTag(text, tag, tagMatch);
        }
        for (String tag : nonStructuralTags) {
            matchNextOpeningTag(text, tag, tagMatch);
            matchNextClosingTag(text, tag, tagMatch);
        }

        if (tagMatch.index == -1) {
            return null;
        }

        tagMatch.index += startIndex;
        tagMatch.endIndex += startIndex;

        return tagMatch;
    }

    private static void matchNextOpeningTag(String text, String tag, TagMatch tagMatch) {
        matchNextTag(tag, tagMatch, text.indexOf("<" + tag + ">"), false);
    }

    private static void matchNextClosingTag(String text, String tag, TagMatch tagMatch) {
        matchNextTag(tag, tagMatch, text.indexOf("</" + tag + ">"), true);
    }

    private static void matchNextTag(String tag, TagMatch tagMatch, int i, boolean isClosing) {
        if (i != -1 && (tagMatch.index == -1 || i < tagMatch.index)) {
            tagMatch.tag = tag;
            tagMatch.index = i;
            tagMatch.isClosing = isClosing;
            tagMatch.endIndex = i + tag.length() + 2 + (isClosing ? 1 : 0);
        }
    }

    private static String insertListStructure(String text) {
        // NOTE: supports three levels of embedding; inserts additional structures to mimic a hierarchy

        // normalize returns and newlines; use newlines only
        text = replaceAll(text, "\r*\n", "XXX_NEWLINE_XXX");
        text = replaceAll(text, "\r", "XXX_NEWLINE_XXX");
        text = replaceAll(text, "\n", "XXX_NEWLINE_XXX");
        text = replaceAll(text, "XXX_NEWLINE_XXXXXX_NEWLINE_XXX", "XXX_NEWLINE_XXXXXX_BREAKING_XXX");
        text = replaceAll(text, "XXX_NEWLINE_XXX", "\n");

        // differentiate bullet items (there may be new lines within one list item)
        text = replaceAll(text, "[\n]+(\\s*[\\-\\*\\+]+ )", "XXX_BREAKING_XXX$1");
        text = replaceAll(text, "[\n]+", "XXX_NONEBREAKING_XXX");

        text = replaceAll(text, "XXX_BREAKING_XXX", "\n");

        // bullet
        text = replaceAll(text, "(?m)^\\s*[\\-\\*]{1}[\\s]+([^\\s^-]{1}.*)$", "<l1b><li>$1</li></l1b>");
        text = replaceAll(text, "(?m)^\\s*[\\-\\*]{2}[\\s]+([^\\s^-]{1}.*)$", "<l2b><li>$1</li></l2b></li></l1b>");
        text = replaceAll(text, "(?m)^\\s*[\\-\\*]{3}[\\s]+([^\\s^-]{1}.*)$", "<l3b><li>$1</li></l3b></li></l2b></li></l1b>");

        // numbered
        text = replaceAll(text, "(?m)^\\s*[\\+]{1}[\\s]+([^\\s^+]{1}.*)$", "<l1n><li>$1</li></l1n>");
        text = replaceAll(text, "(?m)^\\s*[\\+]{2}[\\s]+([^\\s^+]{1}.*)$", "<l2n><li>$1</li></l2n></li></l1n>");
        text = replaceAll(text, "(?m)^\\s*[\\+]{3}[\\s]+([^\\s^+]{1}.*)$", "<l3n><li>$1</li></l3n></li></l2n></li></l1n>");

        // enclose embedded (change tag order at start of substructure)
        text = replaceAll(text, "<\\/l2.>\\s*<\\/li>\\s*<\\/l1.>\\s*(<l3.>)", "$1");
        text = replaceAll(text, "<\\/l1.>\\s*?(<l2.>)", "$1");
        text = replaceAll(text, "<\\/li>\\s*?(<l2.>)", "$1");
        text = replaceAll(text, "<\\/l2.>\\s*?(<l3.>)", "$1");
        text = replaceAll(text, "<\\/li>\\s*?(<l3.>)", "$1");

        // eliminate tags on the same level
        text = replaceAll(text, "<\\/l3n>\\s*?<l3n>", "");
        text = replaceAll(text, "<\\/l3b>\\s*?<l3b>", "");
        text = replaceAll(text, "<\\/l2b>\\s*?<l2b>", "");
        text = replaceAll(text, "<\\/l2n>\\s*?<l2n>", "");
        text = replaceAll(text, "<\\/l1n>\\s*?<l1n>", "");
        text = replaceAll(text, "<\\/l1b>\\s*?<l1b>", "");

        // replace placeholder by real tags (unordered)
        final String unorderedOpenTag = "<ul>";
        final String unorderedCloseTag = "</ul>";
        text = replaceAll(text, "<l1b>", unorderedOpenTag);
        text = replaceAll(text, "<\\/l1b>", unorderedCloseTag);
        text = replaceAll(text, "<l2b>", unorderedOpenTag);
        text = replaceAll(text, "<\\/l2b>", unorderedCloseTag);
        text = replaceAll(text, "<l3b>", unorderedOpenTag);
        text = replaceAll(text, "<\\/l3b>", unorderedCloseTag);

        // eliminate tags on the same level (ordered)
        final String orderedOpenTag = "<ol>";
        final String orderedCloseTag = "</ol>";
        text = replaceAll(text, "<l1n>", orderedOpenTag);
        text = replaceAll(text, "<\\/l1n>", orderedCloseTag);
        text = replaceAll(text, "<l2n>", orderedOpenTag);
        text = replaceAll(text, "<\\/l2n>", orderedCloseTag);
        text = replaceAll(text, "<l3n>", orderedOpenTag);
        text = replaceAll(text, "<\\/l3n>", orderedCloseTag);

        text = replaceAll(text, "XXX_NONEBREAKING_XXX", "\n");

        return text;
    }

    private static String applySimpleMarkdown(String text) {
        // ease boundary conditions
        text = " " + text + " ";

        final String prefixGroup = "([\\s\\.,;:_<>#\\-\\+\\*]+)";
        final String contextGroup = "(\\S.*?\\S)";
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
        text = replaceAll(text, prefixGroup + "\\-" + contextGroup + "\\-" + suffixGroup, "$1<line-through>$2</line-through>$3");
        text = replaceAll(text, "\\{\\-\\}" + contextGroup + "\\-" + suffixGroup, "<line-through>$1</line-through>$2");
        text = replaceAll(text, "\\{\\-\\}" + contextGroup + "\\{\\-\\}", "<line-through>$1</line-through>");

        // eliminate introduced boundaries for further replacements
        text = text.substring(1, text.length() - 1);

        // headings
        text = replaceAll(text, "(?m)^\\s*h1\\. (.*)", "\n\n<b><u>$1</u></b>\n\n");
        text = replaceAll(text, "(?m)^\\s*h2\\. (.*)", "\n\n<b>$1</b>\n\n");
        text = replaceAll(text, "(?m)^\\s*h3\\. (.*)", "\n\n<u>$1</u>\n\n");
        text = replaceAll(text, "(?m)^\\s*h4\\. (.*)", "\n\n<i>$1</i>\n\n");
        text = replaceAll(text, "(?m)^\\s*h5\\. (.*)", "\n\n$1\n\n");

        return text;
    }

    public static String ditaToHtml(String ditaFragment) {
        String htmlFragment = ditaFragment;

        htmlFragment = htmlFragment.replaceAll("<line-through>", "<s>");
        htmlFragment = htmlFragment.replaceAll("</line-through>", "</s>");

        htmlFragment = htmlFragment.replaceAll(DITA_NEWLINE, "<br/>");

        htmlFragment = htmlFragment.replaceAll("<codeblock>", "<pre>");
        htmlFragment = htmlFragment.replaceAll("<codeblock ", "<pre ");
        htmlFragment = htmlFragment.replaceAll("</codeblock>", "</pre>");

        return htmlFragment;
    }

}
