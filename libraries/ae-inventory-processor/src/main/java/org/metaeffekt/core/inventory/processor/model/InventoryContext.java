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
package org.metaeffekt.core.inventory.processor.model;

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.document.model.DocumentDescriptor;

import java.io.File;

/**
 * Represents the context for a specific inventory, providing the necessary metadata and references to facilitate
 * document generation.
 * <p>
 * The {@code InventoryContext} defines a unique identifier, the associated inventory, and additional information
 * required for generating structured reports. It also includes a reference inventory to handle unknown fields or
 * provide comparisons.
 * </p>
 * <p>
 * Each {@code InventoryContext} is validated to ensure all required fields are properly configured before being used
 * in the document generation process.
 * </p>
 *
 * @see DocumentDescriptor
 */
@Getter
@Setter
public class InventoryContext {

    /**
     * The custom identifier of an inventory, which is used for structuring the report.
     */
    private String identifier;

    /**
     * The inventory which is defined in this context.
     */
    private Inventory inventory;

    /**
     * The version of the defined inventory context.
     */
    private String inventoryVersion;

    private InventoryContext referenceInventoryContext;

    private File licensePath;

    private File componentPath;

    /**
     * Fields that are passed to reportContext for inventoryReport generation.
     */
    // FIXME: consider renaming these, currently the name is identical to the fields in reportContext.java, however these names do not make a lot of sense in this context
    private String reportContextTitle;
    private String reportContext;

    public InventoryContext(Inventory inventory, String identifier, String reportContextTitle, String reportContext, String inventoryVersion, File licensePath, File componentPath) {
        this.inventory = inventory;
        this.identifier = identifier;
        this.reportContextTitle = reportContextTitle;
        this.reportContext = reportContext;
        this.inventoryVersion = inventoryVersion;
        this.licensePath = licensePath;
        this.componentPath = componentPath;
    }

    public void validate() {
        // check if the referenced inventory is set
        if (inventory == null) {
            throw new IllegalStateException("The Inventory must be specified");
        }
        // check if the identifier is set
        if (identifier == null) {
            throw new IllegalStateException("The identifier must be specified");
        }
        // check if the reportContextTitle is set
        if (reportContextTitle == null) {
            throw new IllegalStateException("The reportContextTitle must be specified");
        }
        // check if the reportContext is set
        if (reportContext == null) {
            throw new IllegalStateException("The reportContext must be specified");
        }
        // check if the inventoryVersion is set
        if (inventoryVersion == null) {
            throw new IllegalStateException("The inventoryVersion must be specified");
        }
    }
}
