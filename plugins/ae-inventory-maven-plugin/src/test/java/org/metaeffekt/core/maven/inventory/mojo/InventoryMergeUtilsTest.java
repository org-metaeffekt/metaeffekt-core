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
package org.metaeffekt.core.maven.inventory.mojo;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InventoryMergeUtilsTest {

    @Ignore
    @Test
    public void testMerge() throws IOException {
        Inventory targetInventory = new Inventory();
        File sourceInventoryFile = new File("<path to file>");

        List<File> sourceInventories = new ArrayList<>();
        sourceInventories.add(sourceInventoryFile);

        new InventoryMergeUtils().merge(sourceInventories, targetInventory);

        System.out.println(targetInventory.getLicenseData().size());

        Assert.assertTrue(targetInventory.getLicenseData().size() > 0);
    }


}