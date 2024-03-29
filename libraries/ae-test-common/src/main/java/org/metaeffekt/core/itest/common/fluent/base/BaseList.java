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
package org.metaeffekt.core.itest.common.fluent.base;

import org.metaeffekt.core.inventory.processor.model.AbstractModelBase;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseList<T extends AbstractModelBase> {
    protected final List<T> itemList;
    protected String description;

    public BaseList(List<T> itemList, String description) {
        this.itemList = itemList;
        this.description = description;
    }

    public BaseList() {
        this.itemList = new ArrayList<>();
        this.description = "unnamed list";
    }

    public List<T> getItemList() {
        return itemList;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
