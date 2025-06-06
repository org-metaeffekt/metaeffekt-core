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
import java.util.stream.Collectors;

/**
 * Represents the descriptor for a document, encapsulating all the information necessary for generating a document report.
 * The `DocumentDescriptor` contains metadata about the document, such as document parts, document type, parameters,
 * and the target directory for report output. This descriptor can be passed to the {@link DocumentDescriptorReportGenerator}
 * to initiate the document generation process.
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
     * List containing the documentParts with which we construct the final document. Each document is made up of
     * documentParts that define its structure and content.
     */
    private List<DocumentPart> documentParts;

    /**
     * This identifier is used to distinguish between multiple documents when creating bookMaps in the targetReportDir.
     */
    private String identifier;

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
    private String language = "en";

    /**
     * The target directory for the report.
     */
    private File targetReportDir;

    /**
     * A documentDescriptor must be validated with basic integrity checks (e.g. check for missing inventoryId, missing
     * documentType etc.) before a document can be generated with it.
     */
    public void validate() {

        // check if the identifier is set
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalStateException("The identifier of a document must be specified.");
        }
        // check if parts are set
        if (documentParts == null) {
            throw new IllegalStateException("The parts of a document must be specified.");
        }
        // check if document type is set
        if (documentType == null) {
            throw new IllegalStateException("The document type must be specified.");
        }
        // check if the targetReportDir is set
        if (targetReportDir == null) {
            throw new IllegalStateException("The target report directory must be specified.");
        }
        // check if the targetReportDir is actually a directory, the targetReportDir may not exist when initialising the
        // Descriptor, if it does not exist, then the dita generation process for the document creates the directory
        if (targetReportDir.exists() && !targetReportDir.isDirectory()) {
            throw new IllegalStateException("The target report directory must be a directory.");
        }

        // Validation for unique InventoryContexts within the same DocumentPartType
        Map<DocumentPartType, Set<String>> typeToInventoryContextIds = new HashMap<>();

        for (DocumentPart part : documentParts) {

            DocumentPartType type = part.getDocumentPartType();
            Set<String> inventoryContextIds = part.getInventoryContexts().stream()
                    .map(InventoryContext::getIdentifier)
                    .collect(Collectors.toSet());

            typeToInventoryContextIds.putIfAbsent(type, new HashSet<>());

            // Check if any inventory context in the current list already exists in the map
            for (String inventoryContextId : inventoryContextIds) {
                if (!typeToInventoryContextIds.get(type).add(inventoryContextId)) {
                    throw new IllegalStateException("Duplicate InventoryContext detected for DocumentPartType: " + type);
                }
            }
        }

    }
}
