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

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.metaeffekt.core.util.RegExUtils.simpleMarkdownToHtml;

public class RegExUtilsTest {

    @Test
    public void testMarkdownToHtml() {

        Assertions.assertThat(simpleMarkdownToHtml(" _+*Test*+_")).isEqualTo(" <i><u><b>Test</b></u></i>");

        Assertions.assertThat(simpleMarkdownToHtml("*Test*")).isEqualTo("<b>Test</b>");
        Assertions.assertThat(simpleMarkdownToHtml("*Test*Test*")).isEqualTo("<b>Test*Test</b>");
        Assertions.assertThat(simpleMarkdownToHtml("_+*Test*+_")).isEqualTo("<i><u><b>Test</b></u></i>");
        Assertions.assertThat(simpleMarkdownToHtml("_+*_Test_*+_")).isEqualTo("<i><u><b><i>Test</i></b></u></i>");
        Assertions.assertThat(simpleMarkdownToHtml("*/Test/*")).isEqualTo("<b>/Test/</b>");
        Assertions.assertThat(simpleMarkdownToHtml("Test{_}x_")).isEqualTo("Test<i>x</i>");
        Assertions.assertThat(simpleMarkdownToHtml("Test{_}x{_}Suffix")).isEqualTo("Test<i>x</i>Suffix");
        Assertions.assertThat(simpleMarkdownToHtml("Test{*}x{*}Suffix")).isEqualTo("Test<b>x</b>Suffix");
        Assertions.assertThat(simpleMarkdownToHtml("**Test**")).isEqualTo("<i>Test</i>");
        Assertions.assertThat(simpleMarkdownToHtml("-Test-")).isEqualTo("<s>Test</s>");
        Assertions.assertThat(simpleMarkdownToHtml("{{Teletype Text}}")).isEqualTo("<tt>Teletype Text</tt>");

        Assertions.assertThat(simpleMarkdownToHtml("* Level-1\n** Level-2").replace("\n", "")).isEqualTo("<ul><li>Level-1<ul><li>Level-2</li></ul></li></ul>");
        Assertions.assertThat(simpleMarkdownToHtml("* Level-1\n** Level-2.1\n** Level-2.2").replace("\n", "")).isEqualTo("<ul><li>Level-1<ul><li>Level-2.1</li><li>Level-2.2</li></ul></li></ul>");
        Assertions.assertThat(simpleMarkdownToHtml("* Level-1.1\n** Level-2.1\n** Level-2.2\n* Level-1.2").replace("\n", "")).isEqualTo("<ul><li>Level-1.1<ul><li>Level-2.1</li><li>Level-2.2</li></ul></li><li>Level-1.2</li></ul>");

        Assertions.assertThat(simpleMarkdownToHtml("* Level-1\n** Level-2\n*** Level-3.1\n*** Level-3.2")).isEqualTo(
           ("<ul>" +
            "   <li>Level-1" +
            "       <ul>" +
            "           <li>Level-2" +
            "              <ul>" +
            "                   <li>Level-3.1</li>" +
            "                   <li>Level-3.2</li>" +
            "              </ul>" +
            "           </li>" +
            "       </ul>" +
            "   </li>" +
            "</ul>").replace(" ", ""));

        // all ul; incomplete support for ordered lists
        Assertions.assertThat(simpleMarkdownToHtml("+ Level-1\n++ Level-2\n+++ Level-3.1\n+++ Level-3.2")).isEqualTo(
            ("<ul>" +
            "   <li>Level-1" +
            "       <ul>" +
            "           <li>Level-2" +
            "              <ul>" +
            "                   <li>Level-3.1</li>" +
            "                   <li>Level-3.2</li>" +
            "              </ul>" +
            "           </li>" +
            "       </ul>" +
            "   </li>" +
            "</ul>").replace(" ", ""));

        /* incomplete support for ordered lists
        Assertions.assertThat(simpleMarkdownToHtml("+ Level-1\n++ Level-2\n+++ Level-3.1\n+++ Level-3.2")).isEqualTo(
           ("<ol>" +
            "   <li>Level-1" +
            "       <ol>" +
            "           <li>Level-2" +
            "              <ol>" +
            "                   <li>Level-3.1</li>" +
            "                   <li>Level-3.2</li>" +
            "              </ol>" +
            "           </li>" +
            "       </ol>" +
            "   </li>" +
            "</ol>").replace(" ", ""));
         */

        // all ul; incomplete support for ordered lists
        Assertions.assertThat(simpleMarkdownToHtml("* Level-1\n** Level-2\n+++ Level-3.1\n+++ Level-3.2")).isEqualTo(
           ("<ul>" +
            "   <li>Level-1" +
            "       <ul>" +
            "           <li>Level-2" +
            "              <ul>" +
            "                   <li>Level-3.1</li>" +
            "                   <li>Level-3.2</li>" +
            "              </ul>" +
            "           </li>" +
            "       </ul>" +
            "   </li>" +
            "</ul>").replace(" ", ""));

        /* incomplete support for ordered lists
        Assertions.assertThat(simpleMarkdownToHtml("* Level-1\n** Level-2\n+++ Level-3.1\n+++ Level-3.2")).isEqualTo(
           ("<ul>" +
            "   <li>Level-1" +
            "       <ul>" +
            "           <li>Level-2" +
            "              <ol>" +
            "                   <li>Level-3.1</li>" +
            "                   <li>Level-3.2</li>" +
            "              </ol>" +
            "           </li>" +
            "       </ul>" +
            "   </li>" +
            "</ul>").replace(" ", ""));
         */

    }
}