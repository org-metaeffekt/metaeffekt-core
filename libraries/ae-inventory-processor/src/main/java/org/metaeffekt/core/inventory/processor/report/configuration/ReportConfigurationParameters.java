package org.metaeffekt.core.inventory.processor.report.configuration;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReportConfigurationParameters {

    /**
     * If true, disables the priority label in the document (all areas), hides the priority score and hides
     * the priority label columns in the Vulnerability list.
     */
    @Builder.Default
    private final boolean hidePriorityInformation = false;
}
