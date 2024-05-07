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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PreFormattedEscapeUtilsTest {

    private static PreFormattedEscapeUtils escapeUtils;

    @BeforeClass
    public static void init() {
        escapeUtils = new PreFormattedEscapeUtils();
    }

    @Test
    public void xmlEscapeUnspecifiedTagsTest() {
        final String input = "<code>In modern browsers, the MIME type<navbar>that is sent with the HTML document may<br>affect how the document is initially interpreted.</navbar></code>A document sent with the XHTML<test>MIME type is expected to be well";
        Assert.assertEquals("&lt;code&gt;In modern browsers, the MIME type&lt;navbar&gt;that is sent with the HTML document may&lt;br&gt;affect how the document is initially interpreted.&lt;/navbar&gt;&lt;/code&gt;A document sent with the XHTML&lt;test&gt;MIME type is expected to be well", escapeUtils.xml(input));
    }

    @Test
    public void xmlEscapeKnownBalancedTagsTest() {
        final String input = "<p>syntax errors may cause</p>";
        Assert.assertEquals("<p>syntax errors may cause</p>", escapeUtils.xml(input));
    }

    @Test
    public void xmlEscapeNestedKnownBalancedTagsTest() {
        final String input = "<p>There are two axes differentiating various variations of HTML as currently specified:<ul><li>SGML-based HTML</li><li>XML-based HTML</li></ul></p>";
        Assert.assertEquals("<p>There are two axes differentiating various variations of HTML as currently specified:<ul><li>SGML-based HTML</li><li>XML-based HTML</li></ul></p>", escapeUtils.xml(input));
    }

    @Test
    public void xmlEscapeNestedKnownUnbalanced1TagsTest() {
        final String input = "<p>There are two axes differentiating various variations of HTML as currently specified:<ul><li>SGML-based HTML</li>XML-based HTML</li></p>";
        Assert.assertEquals("<p>There are two axes differentiating various variations of HTML as currently specified:&lt;ul&gt;&lt;li&gt;SGML-based HTML&lt;/li&gt;XML-based HTML&lt;/li&gt;</p>", escapeUtils.xml(input));
    }

    @Test
    public void xmlEscapeNestedKnownUnbalanced2TagsTest() {
        final String input = "There are two axes differentiating various variations of HTML as currently specified:<ul><li>SGML-based HTML<li>XML-based HTML</li></ul></p>";
        Assert.assertEquals("There are two axes differentiating various variations of HTML as currently specified:<ul>&lt;li&gt;SGML-based HTML&lt;li&gt;XML-based HTML&lt;/li&gt;</ul>&lt;/p&gt;", escapeUtils.xml(input));
    }

    @Test
    public void xmlEscapeDoubleEscapeCharactersTest() {
        final String input = "&lt;Test&gt;";
        Assert.assertEquals("&lt;Test&gt;", escapeUtils.xml(input));
    }

    @Test
    public void xmlEscapeHeaderHandlingTest() {
        final String input = "<h3>To understand the subtle</h3><h2>differences between HTML and XHTML</h2><h1>it is important to understand</h1>";
        Assert.assertEquals("<p><b>To understand the subtle</b></p><p><b>differences between HTML and XHTML</b></p><p><b>it is important to understand</b></p>", escapeUtils.xml(input));
    }

    @Test
    public void xmlEscapeHeaderAndRawHeaderHandlingTest() {
        final String input = "<h3>To understand the subtle</h3><h2>differences <p><b>between</p></b> HTML and XHTML</h2><h1>it is important to understand</h1>";
        Assert.assertEquals("<p><b>To understand the subtle</b></p><p><b>differences <p><b>between</p></b> HTML and XHTML</b></p><p><b>it is important to understand</b></p>", escapeUtils.xml(input));
    }

    @Test
    public void xmlEscapeHeaderAndRawHeaderHandlingUnbalancedTest() {
        final String input = "<h3>To understand the subtle</h3><h2>differences <p><b>between</p></b> HTML and</b> XHTML</h2><h1>it is important to understand</h1>";
        Assert.assertEquals("<p><b>To understand the subtle</b></p><p><b>differences <p>&lt;b&gt;between</p>&lt;/b&gt; HTML and&lt;/b&gt; XHTML</b></p><p><b>it is important to understand</b></p>", escapeUtils.xml(input));
    }

    @Test
    public void testEscaping() {

        Assert.assertEquals("&copy;", escapeUtils.xml("&copy;"));
        Assert.assertEquals("Copyright &copy; 2021 metaeffekt", escapeUtils.xml("Copyright &copy; 2021 metaeffekt"));
        Assert.assertEquals("Copyright &#169; 2021 metaeffekt", escapeUtils.xml("Copyright © 2021 metaeffekt"));

        Assert.assertEquals("<lq>Copyright &#169; 2021 metaeffekt</lq>", escapeUtils.xml("<lq>Copyright © 2021 metaeffekt</lq>"));
        Assert.assertEquals("<p>Copyright &#169; 2021 metaeffekt</p>", escapeUtils.xml("<p>Copyright © 2021 metaeffekt</p>"));

        // event if already escaped the result removed the escapes

        Assert.assertEquals("Copyright &#169; 2021 metaeffekt", escapeUtils.xml("Copyright &#169; 2021 metaeffekt"));
        Assert.assertEquals("<codeph>Paragraph</codeph>", escapeUtils.xml("<codeph>Paragraph</codeph>"));
        Assert.assertEquals("<p>Paragraph</p>", escapeUtils.xml("&lt;p&gt;Paragraph&lt;/p&gt;"));

        Assert.assertEquals("<p>Paragraph</p>&lt;", escapeUtils.xml("&lt;p&gt;Paragraph&lt;/p&gt;&lt;"));
    }
    
}