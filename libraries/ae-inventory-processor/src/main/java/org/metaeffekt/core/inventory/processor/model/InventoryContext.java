/*
 * Copyright 2009-2026 the original author or authors.
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

import java.text.Normalizer;
import java.util.regex.Pattern;

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
    private String assetVersion;

    /**
     * The assetIdentifier is a combination of the assetName and assetVersion with sanitization. It is used to create
     * unique directory names for Dita content.
     */
    private String assetIdentifier;

    private InventoryContext referenceInventoryContext;

    private String licensesPath;

    private String componentsPath;

    /**
     * Patterns for sanitization.
     */
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern DUPLICATES = Pattern.compile("[-_]{2,}");
    private static final Pattern TRIM_EDGES = Pattern.compile("^[-_]+|[-_]+$");

    /**
     * Fields that are passed to reportContext for inventoryReport generation.
     */
    // FIXME: consider renaming these, currently the name is identical to the fields in reportContext.java, however these names do not make a lot of sense in this context
    private String assetName;
    private String reportContext;

    public InventoryContext(Inventory inventory, String identifier, String reportContext, String licensesPath, String componentsPath) {
        this.inventory = inventory;
        this.identifier = identifier;
        this.reportContext = reportContext;
        this.licensesPath = licensesPath;
        this.componentsPath = componentsPath;
    }

    /**
     * Sanitizes and sets the assetIdentifier using the provided name and version.
     *
     * @param assetName the name of the asset to set the identifier with.
     * @param assetVersion the version of the asset to set the identifier with.
     */
    public void setAssetIdentifier(String assetName, String assetVersion) {
        // Handle Nulls/Empty and combine
        String name = (assetName == null || assetName.trim().isEmpty()) ? "unknown" : assetName.trim();
        String version = (assetVersion == null || assetVersion.trim().isEmpty()) ? "0" : assetVersion.trim();

        String combined = name + "-" + version;

        // Normalize Unicode and remove accents
        String normalized = Normalizer.normalize(combined, Normalizer.Form.NFD);

        // Replace spaces with hyphens and remove illegal characters
        String sanitized = normalized.replace(" ", "-");
        sanitized = NON_LATIN.matcher(sanitized).replaceAll("");

        // Collapse duplicate separators (e.g., "--" -> "-")
        sanitized = DUPLICATES.matcher(sanitized).replaceAll("-");

        // Trim hyphens/underscores from the start and end
        sanitized = TRIM_EDGES.matcher(sanitized).replaceAll("");

        // Final length check & Fallback
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        this.assetIdentifier = sanitized.isEmpty() ? "asset_dir" : sanitized;
    }

    /**
     * Validates a given inventoryContext.
     */
    public void validate() {
        // check if the referenced inventory is set
        if (inventory == null) {
            throw new IllegalStateException("The Inventory must be specified");
        }
        // check if the identifier is set
        if (identifier == null) {
            throw new IllegalStateException("The identifier must be specified");
        }
        // check if the reportContext is set
        if (reportContext == null) {
            throw new IllegalStateException("The reportContext must be specified");
        }
    }
}
