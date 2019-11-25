/**
 * Copyright 2009-2019 the original author or authors.
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

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.DirectoryScanner;
import org.metaeffekt.core.maven.jira.template.TemplateConfigurationParser;
import org.metaeffekt.core.maven.jira.template.TemplateProcessor;
import org.metaeffekt.core.maven.jira.util.JsonTransformer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Mojo to populate velocity templates with content fetched from the JIRA REST API.
 */
@Mojo(name="generate-report", defaultPhase=LifecyclePhase.INITIALIZE)
public class JiraReportMojo extends AbstractJiraRestMojo {

    /** File suffix used to detect a velocity template. */
    private static final String VELOCITY_SUFFIX = ".vt";

    private static final String DUMP_FOLDER_NAME = "jira-report-dump";

    private static final String JQL_PREFIX = "jql_";

    /**
     * The source. This is where the plugin searches for template folders.
     */
    @Parameter(required=true)
    protected File source;

    /**
     * The target folder, where the results are written.
     */
    @Parameter(required=true)
    protected File target;

    /**
     * Flag if existing files should be overwritten or not.
     */
    @Parameter(defaultValue="true")
    protected boolean overwrite;

    /**
     * Indicator whether to skip the execution.
     */
    @Parameter(defaultValue="${ae.jira-report.skip}")
    protected boolean skip;

    /**
     * Whether to dump the intermediate results from JIRA.
     */
    @Parameter(defaultValue="false")
    protected boolean dump;

    @Override
    public void execute() throws MojoFailureException {
        // skip execution for non-pom packaged projects
        if (getProject() != null && isPomPackagingProject()) {
            return;
        }

        // check explicit skip property
        if (skip) {
            getLog().info("in a skipped/offline build JIRA DITA extracts will not be updated");
            return;
        }

        try {
            // velocity can only process files under the configured TEMPLATE_ROOT folder
            // so using the Maven project basedir as template root allows to access all subfolders
            TemplateProcessor processor = new TemplateProcessor(source.getAbsolutePath());

            // scan for velocity templates
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(source);
            scanner.setIncludes(new String[] { "**/*.vt" });
            scanner.scan();

            File dumpFolder = new File(getOutputDirectory(), DUMP_FOLDER_NAME);

            for (String f : scanner.getIncludedFiles()) {
                // substract extension
                final String fileName = f.substring(0, f.length() - VELOCITY_SUFFIX.length());

                // compose files
                final File sourceFile = new File(source, fileName);
                final File targetFile = new File(target, fileName);
                final File dumpFile = new File(dumpFolder, fileName + ".json");

                if (!overwrite && targetFile.exists()) {
                    // skip this file because overwrite is false and it already exists ...
                    getLog().info("skipping file: " + sourceFile);
                    continue;
                }

                getLog().debug("processing source file path: " + sourceFile);
                getLog().debug("writing to target file path: " + targetFile);

                final Map<String, Object> data = new HashMap<>();
                final Map<String, String> configs;
                try {
                    configs = TemplateConfigurationParser.parse(new File(source, f));
                    for (String configKey : configs.keySet()) {
                        // if config is a JCL query, add the search result to the template data
                        if (configKey.toLowerCase().startsWith(JQL_PREFIX)) {
                            String jql = configs.get(configKey);
                            String jqlKey = configKey.substring(JQL_PREFIX.length());
                            jql = processor.processString(jql, data);
                            Object searchResult = getSearchResult(jql);
                            if (dump) {
                                getLog().debug("dumping raw JSON response to: " + dumpFile);
                                String dumpData = JsonTransformer.transform(searchResult, true);
                                FileUtils.write(dumpFile, dumpData, "UTF-8");
                            }
                            data.put(jqlKey, searchResult);
                        }
                    }
                    if (!configs.isEmpty()) {
                        // create target directories if missing
                        targetFile.getParentFile().mkdirs();
                        processor.processFile(f, targetFile, data);
                    }
                } catch (Exception e) {
                    getLog().error(e.getMessage(), e);
                    throw new MojoFailureException(e.getMessage());
                }
            }
        } finally {
            closeClient();
        }
    }

    protected String getOutputDirectory() {
        return getProject().getBuild().getDirectory();
    }

    protected String getBaseDir() {
        return getProject().getBasedir().getAbsolutePath();
    }

    public void setSource(File source) {
        this.source = source;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

}
