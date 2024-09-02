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
package org.metaeffekt.core.maven.jira.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.generic.EscapeTool;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.io.IOException;

import static org.metaeffekt.core.maven.jira.util.RegExUtils.*;

public class RegExUtilsTest {

    @Test
    public void testDashesAndStrikeout() {
        Assertions.assertThat(simpleMarkdownToDita("AAA\r\n---\r\nBBB")).isEqualTo("AAA" + DITA_NEWLINE + "BBB");
        Assertions.assertThat(simpleMarkdownToDita("-Strikeout-")).isEqualTo("<line-through>Strikeout</line-through>");
        Assertions.assertThat(simpleMarkdownToDita("---")).isEqualTo(DITA_NEWLINE);
        Assertions.assertThat(simpleMarkdownToDita("-----")).isEqualTo(DITA_NEWLINE);
        Assertions.assertThat(simpleMarkdownToDita("- - - - -")).isEqualTo(DITA_NEWLINE);
        Assertions.assertThat(simpleMarkdownToDita("-Test-")).isEqualTo("<line-through>Test</line-through>");
    }

    @Test
    public void testSimpleFormatting() {
        Assertions.assertThat(simpleMarkdownToDita("*Test*")).isEqualTo("<b>Test</b>");
        Assertions.assertThat(simpleMarkdownToDita("*123*")).isEqualTo("<b>123</b>");
        Assertions.assertThat(simpleMarkdownToDita("+123+")).isEqualTo("<u>123</u>");
        Assertions.assertThat(simpleMarkdownToDita("A + B and D + E")).isEqualTo("A + B and D + E");
        Assertions.assertThat(simpleMarkdownToDita("A + B and +D + E+")).isEqualTo("A + B and <u>D + E</u>");
        Assertions.assertThat(simpleMarkdownToDita("*Test*Test*")).isEqualTo("<b>Test*Test</b>");
        Assertions.assertThat(simpleMarkdownToDita("_+*Test*+_")).isEqualTo("<i><u><b>Test</b></u></i>");
        Assertions.assertThat(simpleMarkdownToDita("_+*_Test_*+_")).isEqualTo("<i><u><b><i>Test</i></b></u></i>");
        Assertions.assertThat(simpleMarkdownToDita("*/Test/*")).isEqualTo("<b>/Test/</b>");

        // if required make context/purpose cleared
        Assertions.assertThat(simpleMarkdownToDita("**Test**")).isEqualTo("<i>Test</i>");
        Assertions.assertThat(simpleMarkdownToDita("{{Teletype Text}}")).isEqualTo("<tt>Teletype Text</tt>");

        Assertions.assertThat(simpleMarkdownToDita(" _+*Test*+_")).isEqualTo(" <i><u><b>Test</b></u></i>");
    }
    @Test
    public void testNestedLists() {

        Assertions.assertThat(simpleMarkdownToDita(
                        "* Level-1\n** Level-2\n*** Level-3.1\n*** Level-3.2")).
                isEqualTo(
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

        Assertions.assertThat(simpleMarkdownToDita("* Level-1.1\n** Level-2.1\n** Level-2.2\n* Level-1.2").replace("\n", "")).
                isEqualTo("<ul><li>Level-1.1<ul><li>Level-2.1</li><li>Level-2.2</li></ul></li><li>Level-1.2</li></ul>");

        Assertions.assertThat(simpleMarkdownToDita("* -*Tests may be affected*-").replace("\n", "")).
                isEqualTo("<ul><li><line-through><b>Tests may be affected</b></line-through></li></ul>");
        Assertions.assertThat(simpleMarkdownToDita("* Level-1\n** Level-2").replace("\n", "")).
                isEqualTo("<ul><li>Level-1<ul><li>Level-2</li></ul></li></ul>");
        Assertions.assertThat(simpleMarkdownToDita("* Level-1\n** Level-2.1\n** Level-2.2").replace("\n", "")).
                isEqualTo("<ul><li>Level-1<ul><li>Level-2.1</li><li>Level-2.2</li></ul></li></ul>");

        // test support for ordered lists
        Assertions.assertThat(simpleMarkdownToDita(
            "+ Level-1\n++ Level-2\n+++ Level-3.1\n+++ Level-3.2")).
                isEqualTo(
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
    }

    @Test
    public void testSequenceCorrections() {
        // the order of the markup may not be correct; simpleMarkdownToHtml is meant to correct this
        Assertions.assertThat(simpleMarkdownToDita("_+*Test_+*"))
                .isEqualTo("<i><u><b>Test</b></u></i>");
    }

    @Test
    public void testPreserveFormat() {
        // the oder of the markup may not be embedding; the conversion is meant to correct this
        Assertions.assertThat(simpleMarkdownToDita("{noformat}line1\nline2\nline3{noformat}"))
            .isEqualTo("<codeblock>line1\r\nline2\r\nline3</codeblock>");
        Assertions.assertThat(simpleMarkdownToDita("{noformat}line1\r\nline2\r\nline3{noformat}"))
                .isEqualTo("<codeblock>line1\r\nline2\r\nline3</codeblock>");
        Assertions.assertThat(simpleMarkdownToDita("{code}line1\nline2\nline3{code}"))
            .isEqualTo("<codeblock>line1\r\nline2\r\nline3</codeblock>");
        Assertions.assertThat(simpleMarkdownToDita("{code:xml}line1\nline2\nline3{code}"))
            .isEqualTo("<codeblock otherprops=\"lang(xml)\">line1\r\nline2\r\nline3</codeblock>");

        // test additional codeblock removal
        Assertions.assertThat(eliminateCodeBlocks(simpleMarkdownToDita("ABCDE {code:xml}line1\nline2\nline3{code}"), "REMOVED"))
                .isEqualTo("ABCDE <codeblock>REMOVED</codeblock>");
    }

    @Test
    public void testAdjustment() {
        final String result = RegExUtils.adjustXmlTags("<p><b><s>x");
        Assertions.assertThat(result).isEqualTo("<p><b><s>x</s></b></p>");
    }

    @Test
    public void testMarkdownToDita001() throws IOException {
        assertMarkdownToDita("example-001-input.txt", "example-001-output.dita");
    }

    @Test
    public void testMarkdownToDita002() throws IOException {
        assertMarkdownToDita("example-002-input.txt", "example-002-output.dita");
    }

    private void assertMarkdownToDita(String inputPath, String outputPath) throws IOException {
        final File examplesBaseDir = new File("src/test/resources/markdown-to-dita");
        final String input = FileUtils.readFileToString(new File(examplesBaseDir, inputPath));
        final String expectedOutput = FileUtils.readFileToString(new File(examplesBaseDir, outputPath));

        final EscapeTool escapeTool = new EscapeTool();
        final String escapedDescription = escapeTool.xml(input);

        final String ditaFragment = simpleMarkdownToDita(escapedDescription);

        FileUtils.write(new File("target/extract.html"), "<html>" + RegExUtils.ditaToHtml(ditaFragment) + "</html>");
        FileUtils.write(new File("target/extract.dita"), ditaFragment.toString());

        Assertions.assertThat(normalizeString(ditaFragment)).isEqualTo(normalizeString(expectedOutput));
    }

    private String normalizeString(String string) {
        // normalize on carriage returns
        return string.replaceAll("\r*\n", "\r\n");
    }

    @Ignore
    @Test
    public void _dumpJiraExportFormattingIntoHtmlFile() throws IOException {
        final File jiraExportFile = new File("<path-to-jira-export>");
        final JSONObject jsonObject = new JSONObject(FileUtils.readFileToString(jiraExportFile, FileUtils.ENCODING_UTF_8));

        final EscapeTool escapeTool = new EscapeTool();

        final JSONArray issues = jsonObject.getJSONArray("issues");
        final StringBuilder sb = new StringBuilder("<html>");

        final StringBuilder ditaBuilder = new StringBuilder();

        for (int i = 0 ; i < issues.length(); i++) {
            final JSONObject issue = issues.getJSONObject(i);
            final JSONObject fields = issue.getJSONObject("fields");
            final String description = fields.optString("description");
            final String key = issue.getString("key");

            if (StringUtils.isNotBlank(description)) {

                final String escapedDescription = escapeTool.xml(description);
                final String ditaFragment = simpleMarkdownToDita(escapedDescription);
                sb.append(String.format("%s: [%s]<br>%n", key, ditaToHtml(ditaFragment)));

                ditaBuilder.append(String.format("<p>%s:</p>%n<p>%s</p>%n", key, ditaFragment));

                System.out.println("Input: \n" + description);
                System.out.println("Output: \n" + ditaFragment);
            }
        }
        sb.append("</html>");

        FileUtils.write(new File("target/extract.html"), sb.toString());
        FileUtils.write(new File("target/extract.dita"), ditaBuilder.toString());
    }

}