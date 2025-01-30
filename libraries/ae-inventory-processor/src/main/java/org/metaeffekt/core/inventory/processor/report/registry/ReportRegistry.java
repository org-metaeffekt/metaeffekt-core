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


import java.util.HashMap;
import java.util.Map;

public class ReportRegistry {

    private Map<String, TemplateRegistration> registry = new HashMap<>();

    public TemplateRegistration registerTemplate(String sectionId, String templateId) {
        String key = sectionId + ":" + templateId;
        if (!registry.containsKey(key)) {
            registry.put(key, new TemplateRegistration(sectionId, templateId));
        }
        return registry.get(key);
    }

    public String resolve(String elementId) {
        for (TemplateRegistration registration : registry.values()) {
            String link = registration.resolve(elementId);
            if (link != null) {
                return link;
            }
        }
        return null;
    }

}
