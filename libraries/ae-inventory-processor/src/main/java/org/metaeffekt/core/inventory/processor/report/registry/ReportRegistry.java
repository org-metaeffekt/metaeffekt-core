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

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ReportRegistry {

    private final Set<TargetTemplate> registry = new HashSet<>();

    public TargetTemplate register(String sectionId, String templateId) {
        TargetTemplate targetTemplate = new TargetTemplate(sectionId, templateId);
        registry.add(targetTemplate);
        return targetTemplate;
    }

    public String resolve(TargetTemplate target, String elementId) {
        // first search for the ID in the same template.
        if (registry.contains(target) && target.containsElement(elementId)) {
            return target.getResolvedKey(elementId);
        }

        // second search for the ID in the same section.
        Optional<TargetTemplate> sectionTarget = registry.stream()
                .filter(t -> t.getSectionId().equals(target.getSectionId()) && t.containsElement(elementId))
                .findFirst();

        if (sectionTarget.isPresent()) {
            return sectionTarget.get().getResolvedKey(elementId);
        }

        // finally search everywhere
        Optional<TargetTemplate> anyTarget = registry.stream()
                .filter(t -> t.containsElement(elementId))
                .findFirst();

        if (anyTarget.isPresent()) {
            return anyTarget.get().getResolvedKey(elementId);
        }

        log.warn("Element with ID: [{}] was not found in registry.", elementId);
        return null;
    }
}
