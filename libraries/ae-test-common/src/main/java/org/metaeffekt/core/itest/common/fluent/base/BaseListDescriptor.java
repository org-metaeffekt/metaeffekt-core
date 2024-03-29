/*
 * Copyright 2009-2022 the original author or authors.
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

import java.util.List;

public interface BaseListDescriptor<T, SELF extends BaseListDescriptor<T, SELF>> {
    List<T> getItemList();

    String getDescription();

    SELF createNewInstance(List<T> itemList);

    void setDescription(String description);

    default SELF as(String description) {
        setDescription(description);
        return createNewInstance(getItemList());
    }
}
