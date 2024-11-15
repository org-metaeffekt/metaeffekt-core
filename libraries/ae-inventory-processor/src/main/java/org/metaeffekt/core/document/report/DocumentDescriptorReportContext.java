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
package org.metaeffekt.core.document.report;

import lombok.Getter;
import lombok.Setter;

/**
 * Class for assigning report specific settings to a documentDescriptorReportGenerator. Each ReportGenerator has one
 * context passed to it containing the following fields with their default values.
 */
@Getter
@Setter
// FIXME: make this extend ReportContext and clean up wrong usage in DocumentDescriptorReportGenerator & DocumentDescriptorReportTest
public class DocumentDescriptorReportContext {

    /**
     * Fields for handling different fail scenarios in report generation.
     */

   private String referenceLicensePath = "licenses";
   private String referenceComponentPath = "components";

   // FIXME: make a file; rename to targetReportDir
   private String targetReportPath;

}
