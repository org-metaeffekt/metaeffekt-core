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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Slf4j
public class ReportRegistry {

    private final Set<TargetTemplate> registry = new HashSet<>();

    /**
     * Templates register themselves when they are called. They must register themselves with their section id
     * which is equal to the directory name, as well as the templateId which has to be the template name without
     * the file extensions.
     *
     * @param sectionId The directory name in which the template resides.
     * @param templateId The file name without extensions of the template.
     * @return A TargetTemplate object used to register and store all element ids present in the template.
     */
    public TargetTemplate register(String sectionId, String templateId) {
        TargetTemplate targetTemplate = new TargetTemplate(sectionId, templateId);
        registry.add(targetTemplate);
        return targetTemplate;
    }

    /**
     * Resolves full id from the element id by calling findElement(). If no element is found, usually because the element
     * has not been registered yet, returns a placeholder to be resolved later.
     * @param template The template from which the method is called.
     * @param elementId The template from which the method is called.
     * @return Either the fully resolved if key or a placeholder UUID.
     */
    public String resolve(TargetTemplate template, String elementId) {
        String foundElement = findElement(template, elementId);

        // Adds the unresolved reference to the map for replacement in a later pass
        if (foundElement == null) {
            String placeholderUUID = UUID.randomUUID().toString();
            template.addUnresolvedPlaceholder(placeholderUUID, elementId);
            return placeholderUUID;
        }

        // FIXME: REMOVE
        log.debug("Element [{}] resolved.", foundElement);
        return foundElement;
    }

    /**
     * Attempts to find an element by its id. First checks the template from which the reference is called, then searches
     * the same section and finally searches all templates for the id.
     * @param template The template from which the method is called.
     * @param elementId The template from which the method is called.
     * @return Either the full id key or null if the element was not found.
     */
    private String findElement(TargetTemplate template, String elementId) {
        // first search for the ID in the same template.
        if (registry.contains(template) && template.containsElement(elementId)) {
            return template.getResolvedKey(elementId);
        }

        // second search for the ID in the same section.
        Optional<TargetTemplate> sectionTarget = registry.stream()
                .filter(t -> t.getSectionId().equals(template.getSectionId()) && t.containsElement(elementId))
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

        return null;
    }

    /**
     * This method collects all placeholder entries across all template entries in the registry. Based on which templates contain
     * placeholders the method reads those template files as strings and attempts to replace the placeholders with their correct ids.
     *
     * @param targetDirectory  The directory in which the report templates were generated in.
     */
    public void populateUnresolvedReferences(File targetDirectory) {
        for (TargetTemplate template : registry) {
            if (!template.getUnresolvedPlaceholders().isEmpty()) {
                File templateFile  = org.metaeffekt.core.util.FileUtils.findFirstFileByName(targetDirectory, template.getTemplateId());
                if (templateFile == null) {
                    throw new RuntimeException("Failed to locate template file [" + template.getTemplateId() + "] " +
                            "in  target directory [" + targetDirectory.getAbsolutePath() + "], check if all dita-templates have registered " +
                            "themselves correctly with the ReportRegistry.");
                }
                try {
                    String fileString = FileUtils.readFileToString(templateFile, StandardCharsets.UTF_8);

                    for (Map.Entry<String, String> entry : template.getUnresolvedPlaceholders().entrySet()) {
                        String resolved =  findElement(template, entry.getValue());

                        if (resolved != null) {
                            fileString = fileString.replace(entry.getKey(), resolved);
                            FileUtils.writeStringToFile(templateFile, fileString, StandardCharsets.UTF_8);
                        } else {
                            log.debug("Reference to element id [{}] from template [{}] was unsuccessfull.", entry.getValue(), template.getTemplateId());
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
