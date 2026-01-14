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

package org.metaeffekt.core.itest.common.fluent;

import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.itest.common.fluent.base.*;

import java.util.List;

public class ComponentPatternList extends BaseList<ComponentPatternData>
        implements BaseListAsserts<ComponentPatternData>, BaseListLogger<ComponentPatternData, ComponentPatternList>,
        BaseListFilter<ComponentPatternData, ComponentPatternList>,
        BaseListSize<ComponentPatternData, ComponentPatternList> {
    public ComponentPatternList(List<ComponentPatternData> componentPatternDataList, String description) {
        super(componentPatternDataList, description);
    }

    public ComponentPatternList() {
        super();
    }

    @Override
    public ComponentPatternList createNewInstance(List<ComponentPatternData> filteredList) {
        return new ComponentPatternList(filteredList, this.description);
    }
}
