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
package org.metaeffekt.core.inventory.processor.report;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * This class enables the validation of all links found in .dita files and if their respective targets exist.
 */
public class DitaTemplateLinkValidator {

    private final HashMap<File, List<String>> hrefTargetsPerFile = new HashMap<>();
    private final HashMap<File, List<String>> idsPerFile = new HashMap<>();
    private final List<String> errors = new ArrayList<>();

    /**
     * Validates a directory containing dita xml files for valid link targets by checking whether all href attributes
     * point to existing ids.
     * @param templateDir the directory containing the dita xml files.
     * @return a list of errors collected during the validation process.
     */
    public List<String> validateDir(File templateDir) {

        File[] ditaFiles = templateDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".dita"));

        if (ditaFiles == null || ditaFiles.length == 0) {
            throw new RuntimeException("No dita files found in " + templateDir);
        }

        for (File templateFile : ditaFiles) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document document;

            try {
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(templateFile);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse template file: " + templateFile.getAbsolutePath(), e);
            }

            collectHrefsAndIds(templateFile, document);
        }

        validateLinks();
        return errors;
    }

    /**
     * Collects all elements with href attributes which have not been specifically excluded, as well as all ids
     * in a single .dita template file.
     * @param templateFile the dita file from which to collect the ids and hrefs.
     * @param document the xml document object.
     */
    private void collectHrefsAndIds(File templateFile, Document document) {

        NodeList allElements = document.getElementsByTagName("*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            String href = element.getAttribute("href");
            String scope = element.getAttribute("scope");
            String id = element.getAttribute("id");

            if (!href.isEmpty() && !scope.equals("external") && !element.getTagName().equals("image")) {
                hrefTargetsPerFile.computeIfAbsent(templateFile, k -> new ArrayList<>()).add(href);
            }

            if (!id.isEmpty()) {
                idsPerFile.computeIfAbsent(templateFile, k -> new ArrayList<>()).add(id);
            }
        }
    }

    /**
     * Validates all links from href attributes to their respective targets via the ids and target files
     * extracted during previous steps.
     */
    private void validateLinks() {

        for (Map.Entry<File, List<String>> entry : hrefTargetsPerFile.entrySet()) {
            for (String href : entry.getValue()) {
                if (href.startsWith("http://") ||  href.startsWith("https://")) {
                    continue;
                }

                File hrefTarget = getHrefTargetFile(href);
                File targetFile = hrefTarget != null ? hrefTarget : entry.getKey();

                List<String> targetIds = getHrefTargetIds(href);

                if (!targetIds.isEmpty() && targetFile != null) {
                    if (idsPerFile.containsKey(targetFile)) {
                        List<String> ids = idsPerFile.get(targetFile);
                        for (String id : targetIds) {
                            if (!ids.contains(id)) {
                                errors.add("Href target " + href + " does not exist in file " + targetFile.getName());
                            }
                        }
                    } else {
                        errors.add("Href target " + href + " does not exist in file " + targetFile.getName());
                    }
                } else {
                    errors.add("Href target " + href + " does not point to a target file and/or has no target id listed.");
                }
            }
        }
    }

    /**
     * Extracts the target file from a href attribute.
     * @param href the href attribute which to scan.
     * @return a file object pointing to the file name or null if the href points to a target inside the same file.
     */
    private File getHrefTargetFile(String href) {
        File targetFile;

        if (href.startsWith("#")) {
            return null;
        } else {
            String[] parts = href.split("#", 2);
            Optional<File> optionalTargetFile = idsPerFile.keySet().stream()
                    .filter(f -> f.getName().equals(parts[0]))
                    .findFirst();

            targetFile = optionalTargetFile.orElse(null);
            if (targetFile == null) {
                errors.add("Href target " + href + " points to non existent file: " + parts[0]);
            }
        }

        return targetFile;
    }

    /**
     * Extracts a list of target ids from a href attribute. This needs to be a list as hrefs can point to nested
     * ids via '/' seperated ids.
     * @param href the href attribute which to scan.
     * @return a list of all single ids in the href attribute.
     */
    private List<String> getHrefTargetIds(String href) {

        String fullId = "";
        if (href.startsWith("#")) {
            fullId = href.substring(1);
        } else {
            String[] parts = href.split("#", 2);

            if (parts.length > 1) {
                fullId = parts[1];
            } else {
                errors.add("Href target " + href + " contains no valid id. ");
            }
        }

        if (fullId.contains("/")) {
            return Arrays.asList(fullId.split("/"));
        } else {
            return Collections.singletonList(fullId);
        }
    }
}
