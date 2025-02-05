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
package org.metaeffekt.core.inventory.processor.report.registry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class TargetTemplate {

    @Getter
    private final String sectionId;

    @Getter
    private final String templateId;

    private final Set<String> elementIds;

    public TargetTemplate(String sectionId, String templateId) {
        this.sectionId = sectionId;
        this.templateId = templateId;
        this.elementIds = new HashSet<>();
    }

    public String registerElement(String elementId) {
        if (elementIds.add(elementId)) {
            return sectionId + ":" + templateId + ":" + elementId;
        } else {
            log.warn("Duplicate element id [{}] in template [{}].", elementId, templateId);
            return null;
        }
    }

    public boolean containsElement(String elementId) {
        return elementIds.contains(elementId);
    }

    public String getResolvedKey(String elementId) {
        return sectionId + ":" + templateId + ":" + elementId;
    }
}
