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
package org.metaeffekt.core.document.report;

import org.junit.jupiter.api.Test;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentPart;
import org.metaeffekt.core.document.model.DocumentPartType;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentDescriptorReportGeneratorDeriveAssetsTest {

    private DocumentDescriptor setupDescriptor(InventoryContext inventoryContext, Map<String, String> partParams) {
        DocumentDescriptor documentDescriptor = new DocumentDescriptor();
        DocumentPart part = new DocumentPart("testPart", Collections.singletonList(inventoryContext), DocumentPartType.LICENSE_DOCUMENTATION, partParams);
        documentDescriptor.setDocumentParts(Collections.singletonList(part));
        return documentDescriptor;
    }

    @Test
    public void reportWithoutAsset() {
        InventoryContext context = new InventoryContext(new Inventory(), "test-context", "reportContext", null, null);
        context.setAssetName("test-asset");
        context.setAssetVersion("1.0");

        Map<String, String> params = new HashMap<>();
        params.put("reportWithoutAsset", "true");

        DocumentDescriptor descriptor = setupDescriptor(context, params);
        DocumentDescriptorReportGenerator.deriveAssets(descriptor);

        InventoryContext resultContext = descriptor.getDocumentParts().get(0).getInventoryContexts().get(0);
        assertEquals("", resultContext.getAssetName());
        assertEquals("", resultContext.getAssetVersion());
    }

    @Test
    public void bothNameAndVersionProvided() {
        InventoryContext context = new InventoryContext(new Inventory(), "test-context", "reportContext", null, null);
        context.setAssetName("test-asset");
        context.setAssetVersion("1.0");

        DocumentDescriptor descriptor = setupDescriptor(context, null);
        DocumentDescriptorReportGenerator.deriveAssets(descriptor);

        InventoryContext resultContext = descriptor.getDocumentParts().get(0).getInventoryContexts().get(0);
        assertEquals("test-asset", resultContext.getAssetName());
        assertEquals("1.0", resultContext.getAssetVersion());
    }

    @Test
    public void onlyNameProvided() {
        InventoryContext context = new InventoryContext(new Inventory(), "context-1", "reportContext", null, null);
        context.setAssetName("test-asset");
        context.setAssetVersion(null);

        DocumentDescriptor descriptor = setupDescriptor(context, null);
        DocumentDescriptorReportGenerator.deriveAssets(descriptor);

        InventoryContext resultContext = descriptor.getDocumentParts().get(0).getInventoryContexts().get(0);
        assertEquals("test-asset", resultContext.getAssetName());
        assertEquals("", resultContext.getAssetVersion());
    }

    @Test
    public void onlyVersionProvided() {
        InventoryContext context = new InventoryContext(new Inventory(), "context-1", "reportContext", null, null);
        context.setAssetName(null);
        context.setAssetVersion("1.0");

        DocumentDescriptor descriptor = setupDescriptor(context, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            DocumentDescriptorReportGenerator.deriveAssets(descriptor);
        });

        assertTrue(ex.getMessage().contains("no 'assetName' is specified"));
    }

    @Test
    public void deriveWithInventorySeparator() {
        Inventory inventory = new Inventory();
        AssetMetaData amd = new AssetMetaData();
        amd.set("Primary", "x");
        amd.set(AssetMetaData.Attribute.ASSET_ID, "AID-1");
        amd.set(AssetMetaData.Attribute.NAME, "DerivedAsset");
        amd.set(AssetMetaData.Attribute.VERSION, "2.0");
        inventory.getAssetMetaData().add(amd);

        InventoryContext context = new InventoryContext(inventory, "context-1", "reportContext", null, null);
        context.setAssetName(null);
        context.setAssetVersion(null);

        DocumentDescriptor descriptor = setupDescriptor(context, null);
        DocumentDescriptorReportGenerator.deriveAssets(descriptor);

        InventoryContext resultContext = descriptor.getDocumentParts().get(0).getInventoryContexts().get(0);
        assertEquals("DerivedAsset", resultContext.getAssetName());
        assertEquals("2.0", resultContext.getAssetVersion());
    }

    @Test
    public void deriveWithSeparatorNoVersion() {
        Inventory inventory = new Inventory();
        AssetMetaData amd = new AssetMetaData();
        amd.set("Primary", "x");
        amd.set(AssetMetaData.Attribute.ASSET_ID, "AID-1");
        amd.set(AssetMetaData.Attribute.NAME, "DerivedAsset");
        // No version specified
        inventory.getAssetMetaData().add(amd);

        InventoryContext context = new InventoryContext(inventory, "context-1", "reportContext", null, null);
        context.setAssetName(null);
        context.setAssetVersion(null);

        DocumentDescriptor descriptor = setupDescriptor(context, null);
        DocumentDescriptorReportGenerator.deriveAssets(descriptor);

        InventoryContext resultContext = descriptor.getDocumentParts().get(0).getInventoryContexts().get(0);
        assertEquals("DerivedAsset", resultContext.getAssetName());
        assertEquals("", resultContext.getAssetVersion());
    }

    @Test
    public void deriveWithSeparatorNoneProvided() {
        Inventory inventory = new Inventory();
        AssetMetaData amd = new AssetMetaData();
        amd.set("Primary", "x");
        amd.set(AssetMetaData.Attribute.ASSET_ID, "AID-1");
        // No name specified
        amd.set(AssetMetaData.Attribute.VERSION, "2.0");
        inventory.getAssetMetaData().add(amd);

        InventoryContext context = new InventoryContext(inventory, "context-1", "reportContext", null, null);
        context.setAssetName(null);
        context.setAssetVersion(null);

        DocumentDescriptor descriptor = setupDescriptor(context, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            DocumentDescriptorReportGenerator.deriveAssets(descriptor);
        });

        assertTrue(ex.getMessage().contains("Missing asset name"));
    }
}
