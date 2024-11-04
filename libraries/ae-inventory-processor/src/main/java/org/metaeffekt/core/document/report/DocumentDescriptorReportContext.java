package org.metaeffekt.core.document.report;

import lombok.Getter;
import lombok.Setter;

/**
 * Class for assigning report specific settings to a documentDescriptorReportGenerator. Each ReportGenerator has one
 * context passed to it containing the following fields with their default values.
 */
@Getter
@Setter
public class DocumentDescriptorReportContext {

    /**
     * Fields for handling different fail scenarios in report generation.
     */
    // FIXME: review default values
   private Boolean FailOnUnknown = false;
   private Boolean FailOnUnknownVersion = false;
   private Boolean FailOnMissingLicense = false;

   private String referenceLicensePath = "licenses";
   private String referenceComponentPath = "components";

   private String targetReportPath;
}
