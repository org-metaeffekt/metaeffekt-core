package org.metaeffekt.core.inventory.processor.report.registry;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ReportRegistry {

    private Set<Target> registry = new HashSet<>();

    public Target register(String sectionId, String templateId) {
        Target target = new Target(sectionId, templateId);
        registry.add(target);
        return target;
    }

    public String resolve(String elementId) {
        for (Target target : registry) {
            if (target.containsElement(elementId)) {
                return target.getResolvedKey(elementId);
            }
        }
        log.warn("Element with ID: [{}] was not found in registry.", elementId);
        return null;
    }

    private static class Target {

        private final String sectionId;
        private final String templateId;
        private final Set<String> elementIds;

        private Target(String sectionId, String templateId) {
            this.sectionId = sectionId;
            this.templateId = templateId;
            this.elementIds = new HashSet<>();
        }

        public String registerElement(String elementId) {
            if (elementIds.add(elementId)) {
                return sectionId + ":" + templateId + ":" + elementId;
            } else {
                log.warn("Duplicate element ID: [{}] in template [{}]", elementId, templateId);
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
}
