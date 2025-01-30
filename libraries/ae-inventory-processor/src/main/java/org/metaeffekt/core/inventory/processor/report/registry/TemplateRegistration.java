package org.metaeffekt.core.inventory.processor.report.registry;

import java.util.HashMap;
import java.util.Map;

public class TemplateRegistration {

    private String SECTION_ID;
    private String TEMPLATE_ID;
    private Map<String, String> elementLinks;

    public TemplateRegistration(String SECTION_ID, String TEMPLATE_ID) {
       this.SECTION_ID = SECTION_ID;
       this.TEMPLATE_ID = TEMPLATE_ID;
       elementLinks = new HashMap<>();
    }

    public String register(String elementId) {
        String completeLink = SECTION_ID + ":" + TEMPLATE_ID + ":" + elementId;
        elementLinks.put(elementId, completeLink);
        return completeLink;
    }

    public String resolve(String elementId) {
        return elementLinks.get(elementId);
    }
}
