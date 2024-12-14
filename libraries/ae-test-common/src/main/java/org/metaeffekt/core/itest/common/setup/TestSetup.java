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
package org.metaeffekt.core.itest.common.setup;

import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;


public interface TestSetup {

    TestSetup setName(String testName);

    TestSetup setSource(String source);

    boolean clear() throws Exception;

    boolean load(boolean overwrite) throws Exception;

    boolean inventorize(boolean overwrite) throws Exception;

    boolean loadInventory() throws Exception;

    boolean rebuildInventory() throws Exception;

    Inventory getInventory() throws Exception;

    Inventory readReferenceInventory() throws Exception;

    TestSetup setReferenceInventory(String referenceInventory);

    TestSetup setSha256Hash(String sha256);

    String getScanFolder();

    String getInventoryFolder();

    String getAggregationDir();
}
