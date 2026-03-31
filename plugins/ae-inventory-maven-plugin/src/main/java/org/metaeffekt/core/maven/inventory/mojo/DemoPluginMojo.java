/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.metaeffekt.core.inventory.processor.report.adapter.ReportAdapterLoader;
import org.metaeffekt.core.inventory.processor.report.adapter.IAssessmentReportAdapter;

import java.util.Optional;

/**
 * Tests the Artifact Analysis Report Adapter Infrastructure
 *
 * @goal demo-plugin-mojo
 */
public class DemoPluginMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Adapter Registry found [" + ReportAdapterLoader.getAllAdapters().size() + "] adapter(s)");

        final Optional<IAssessmentReportAdapter> instance = ReportAdapterLoader.getAdapter(IAssessmentReportAdapter.class);
        if (instance.isPresent()) {
            getLog().info("- Adapter: " + instance);
        } else {
            getLog().warn("- Adapter could not be found");
        }
    }
}