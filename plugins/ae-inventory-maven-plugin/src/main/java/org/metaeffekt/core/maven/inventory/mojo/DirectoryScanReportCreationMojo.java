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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.InventoryScanReport;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;

import java.io.File;

/**
 * Creates a report by scanning a folder in the file system.
 *
 * @goal create-directory-report
 */
public class DirectoryScanReportCreationMojo extends AbstractInventoryReportCreationMojo {

    /**
     * Change: 12/2022: not mandatory anymore; when no input directory is provided the mojo anticipated the scanDirectory
     * is managed in advance to an invocation. The other attributes (scanIncludes, scanExcludes) are ignored in this case.
     *
     * @parameter
     */
    private File inputDirectory;

    /**
     * @parameter expression="${project.build.directory}/scan"
     */
    private File scanDirectory;

    /**
     * @parameter
     */
    private String[] scanIncludes = new String[]{"**/*"};

    /**
     * @parameter
     */
    private String[] scanExcludes;

    /**
     * @parameter
     */
    private boolean enableImplicitUnpack = true;

    /**
     * For backward compatibility reasons the default value is false. The feature requires explicit activation.
     * @parameter
     */
    private boolean includeEmbedded = false;

    /**
     * For backward compatibility reasons the default value is false. The feature requires explicit activation.
     * @parameter
     */
    private boolean enableDetectComponentPatterns = false;

    @Override
    protected InventoryReport initializeInventoryReport() throws MojoExecutionException {

        // use this to modify the config parameters specific to this mojo
        ReportConfigurationParameters.ReportConfigurationParametersBuilder configParams = configureParameters();

        InventoryScanReport report = new InventoryScanReport(configParams.build());

        // apply standard configuration (parent class)
        configureInventoryReport(report);

        report.setInputDirectory(inputDirectory);
        report.setScanDirectory(scanDirectory);
        report.setScanIncludes(scanIncludes);
        report.setScanExcludes(scanExcludes);

        report.setEnableImplicitUnpack(enableImplicitUnpack);
        report.setIncludeEmbedded(includeEmbedded);
        report.setEnableDetectComponentPatterns(enableDetectComponentPatterns);

        return report;
    }

}
