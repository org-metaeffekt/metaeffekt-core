/*
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.maven.jira.mojo;


import org.apache.maven.plugin.MojoFailureException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

@Ignore // This test requires to be online; it accesses the metaeffekt atlassian project.
public class ExtractFromJiraTest {

    @Test
    public void testExtract() throws MojoFailureException {

        JiraReportMojo jiraReportMojo = new JiraReportMojo() {
            @Override
            protected String getBaseDir() {
                return ".";
            }

            @Override
            protected String getOutputDirectory() {
                return "target/output";
            }
        };
        jiraReportMojo.setSource(new File("src/test/resources/templates"));
        jiraReportMojo.setTarget(new File("target/output"));
        jiraReportMojo.setUserName("reader");
        jiraReportMojo.setUserPassword(System.getProperty("metaeffekt.atlassian.net.password"));
        jiraReportMojo.setServerUrl("https://metaeffekt.atlassian.net");
        jiraReportMojo.setOverwrite(true);
        jiraReportMojo.dump = true;

        jiraReportMojo.execute();

    }
}
