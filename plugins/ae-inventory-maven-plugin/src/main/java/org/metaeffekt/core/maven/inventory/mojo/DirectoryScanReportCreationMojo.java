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
package org.metaeffekt.core.maven.inventory.mojo;

import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.InventoryScanReport;

import java.io.File;

/**
 * Creates a report by scanning a folder in the file system.
 *
 * @goal create-directory-report
 */
public class DirectoryScanReportCreationMojo extends AbstractInventoryReportCreationMojo {

    /**
     * @parameter
     * @required
     */
    private File inputDirectory;

    /**
     * @parameter expression="${project.build.directory}/scan"
     * @required
     */
    private File scanDirectory;

    /**
     * @parameter
     * @required
     */
    private String[] scanIncludes = new String[]{"**/*"};

    /**
     * @parameter
     * @required
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

    @Override
    protected InventoryReport initializeInventoryReport() {
        InventoryScanReport report = new InventoryScanReport();

        // apply standard configuration (parent class)
        configureInventoryReport(report);

        report.setInputDirectory(inputDirectory);
        report.setScanDirectory(scanDirectory);
        report.setScanIncludes(scanIncludes);
        report.setScanExcludes(scanExcludes);

        report.setEnableImplicitUnpack(enableImplicitUnpack);
        report.setIncludeEmbedded(includeEmbedded);

        return report;
    }

}
