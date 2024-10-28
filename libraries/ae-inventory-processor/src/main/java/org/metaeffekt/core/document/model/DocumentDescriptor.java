package org.metaeffekt.core.document.model;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.document.report.DocumentDescriptorReportContext;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A documentDescriptor encapsulates all information needed for document generation. documentDescriptors can be passed
 * to DocumentDescriptorReportGenerator.generate() for document generation.
 */
@Getter
@Setter
public class DocumentDescriptor {

    /**
     * List containing the inventoryContexts for each inventory we want to add to a report. The information from
     * inventoryContext is used to control execution of report generation.
     */
    List<InventoryContext> inventoryContexts;

    /**
     * object containing the context specific report generation parameters.
     */
    DocumentDescriptorReportContext reportContext;

    /**
     * Representation of each document type that we can report on, depending on the set documentType, different
     * pre-requisites are checked.
     */
    DocumentType documentType;

    /**
     * Params may include parameters to control the document structure and content. They may also contain configurable
     * placeholder replacement values or complete text-blocks (including basic markup).
     */
    Map<String, String> params;

    /**
     * A documentDescriptor must be validated with basic integrity checks (e.g. check for missing inventoryId, missing
     * documentType etc.) before a document can be generated with it.
     */
    public void validate(){

    }

}
