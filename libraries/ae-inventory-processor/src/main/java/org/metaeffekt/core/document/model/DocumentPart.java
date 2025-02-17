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
import org.metaeffekt.core.inventory.processor.model.InventoryContext;

import java.util.List;
import java.util.Map;

/**
 * Class representing a part of a document.
 */
@Slf4j
@Getter
@Setter
public class DocumentPart {

    String identifier;

    /**
     * List containing the inventoryContexts for each inventory we want to add to a report. Each inventoryContext contains
     * an inventory and other data needed for the report.
     */
    private List<InventoryContext> inventoryContexts;

    /**
     * The type of DocumentPart (e.g. vulnerability-statistics-report, vulnerability-summary-report, etc.)
     */
    private DocumentPartType documentPartType;

    /**
     * We define params on document-level as well as document-part-level. If a documentPart defines a parameter that the
     * already defines, then we expect the Document parameter to be overwritten on document-part-level.
     */
    private Map<String, String> params;

    public DocumentPart(String identifier, List<InventoryContext> inventoryContexts, DocumentPartType documentPartType, Map<String, String> params) {
        this.inventoryContexts = inventoryContexts;
        this.documentPartType = documentPartType;
        this.params = params;
        this.identifier = identifier;

    }

    public void validate() {

        // check if the identifier is set
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalStateException("The identifier of a part must be specified.");
        }
        // check if document type is set
        if (documentPartType == null) {
            throw new IllegalStateException("The part type must be specified.");
        }
        // validate each inventoryContext
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
