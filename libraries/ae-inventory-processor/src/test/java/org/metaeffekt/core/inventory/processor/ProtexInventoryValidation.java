/**
 * Copyright 2009-2017 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.dom4j.DocumentException;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.reader.ProtexInventoryReader;

import java.io.File;
import java.io.IOException;

public class ProtexInventoryValidation {

    public static void main(String[] args) throws IOException, DocumentException {
        File protexFile = new File("C:/dev/workspace/artifact-inventory-trunk/thirdparty/input/globalInventoryReport-2013-09-02.xls");

        // read protext inventory
        final Inventory protexInventory = new ProtexInventoryReader().readInventory(protexFile);
        protexInventory.removeInconsistencies();
    }

}
