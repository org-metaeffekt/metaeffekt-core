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
package org.metaeffekt.core.document.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.document.report.DocumentDescriptorReportGenerator;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;

import java.io.File;
import java.util.*;

/**
 * Represents the descriptor for a document, encapsulating all the information necessary for generating a document report.
 * The `DocumentDescriptor` contains metadata about the document, such as inventory contexts, document type, parameters,
 * and the target directory for report output. This descriptor can be passed to the {@link DocumentDescriptorReportGenerator}
 * to initiate the document generation process.
 * <p>
 * A `DocumentDescriptor` contains:
 * <ul>
 *   <li>Inventory contexts that define the data to be included in the report.</li>
 *   <li>A document type to control the generation process based on specific pre-requisites.</li>
 *   <li>Parameters for customizing the document's structure, content, and placeholders.</li>
 *   <li>The target directory where the generated report will be saved.</li>
 * </ul>
 *
 * @see DocumentDescriptorReportGenerator
 * @see InventoryContext
 * @see DocumentType
 */
@Slf4j
@Getter
@Setter
public class DocumentDescriptor {

    /**
     * List containing the inventoryContexts for each inventory we want to add to a report. The information from
     * inventoryContext is used to control execution of report generation.
     */
    private List<InventoryContext> inventoryContexts;

    /**
     * Representation of each document type that we can report on, depending on the set documentType, different
     * pre-requisites are checked.
     */
    private DocumentType documentType;

    /**
     * Params may include parameters to control the document structure and content. They may also contain configurable
     * placeholder replacement values or complete text-blocks (including basic markup).
     */
    private Map<String, String> params;

    /**
     * The language in which the document should be produced.
     */
    private String templateLanguageSelector = "en";

    /**
     * The target directory for the report.
     */
    private File targetReportDir;

    /**
     * A documentDescriptor must be validated with basic integrity checks (e.g. check for missing inventoryId, missing
     * documentType etc.) before a document can be generated with it.
     */
    public void validate() {

        // check if document type is set
        if (documentType == null) {
            throw new IllegalStateException("The document type must be specified.");
        }
        // check if there are inventoryContexts set
        if (inventoryContexts.isEmpty()) {
            throw new IllegalStateException("No inventory contexts specified.");
        }
        // check if the targetReportDir is set
        if (targetReportDir == null) {
            throw new IllegalStateException("The target report directory must be specified.");
        }
        // check if the targetReportDir is actually a directory
        if (targetReportDir.exists() && !targetReportDir.isDirectory()) {
            throw new IllegalStateException("The target report directory must be a directory.");
        }
        //TODO: revise validation of targetReportDir, what do we anticipate of the targetReportDir?

        // validate each inventoryContext
        Set<String> identifiers = new HashSet<>();
        for (InventoryContext context : inventoryContexts) {
            // check if each inventoryContext references an inventory
            if (context.getInventory() == null) {
                throw new IllegalStateException("The inventory must be specified.");
            }
            // check if each inventoryContext has an identifier
            if (context.getIdentifier() == null){
                throw new IllegalStateException("The identifier must be specified.");
            }
            // check if each inventoryContext has an identifier
            if (context.getIdentifier().isEmpty()){
                throw new IllegalStateException("The identifier must not be empty.");
            }
        }
    }
}
