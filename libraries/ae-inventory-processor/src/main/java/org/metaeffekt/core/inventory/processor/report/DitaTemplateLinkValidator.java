package org.metaeffekt.core.inventory.processor.report;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class DitaTemplateLinkValidator {

    HashMap<File, List<String>> hrefTargetsPerFile = new HashMap<>();
    HashMap<File, List<String>> idsPerFile = new HashMap<>();

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

        return validateLinks();
    }

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

    private List<String> validateLinks() {
        List<String> errors = new ArrayList<>();

        for (Map.Entry<File, List<String>> entry : hrefTargetsPerFile.entrySet()) {
            for (String href : entry.getValue()) {
                if (href.startsWith("http://") ||  href.startsWith("https://")) {
                    continue;
                }

                File targetFile = entry.getKey();
                String targetId = null;

                if (href.startsWith("#")) {
                    targetId = href.substring(1);
                } else {
                    String[] parts = href.split("#", 2);
                    Optional<File> optionalTargetFile = idsPerFile.keySet().stream()
                            .filter(f -> f.getName().equals(parts[0]))
                            .findFirst();

                    targetFile = optionalTargetFile.orElse(null);

                    if (targetFile == null) {
                        errors.add("Href target " + href + " points to non existent file: " + parts[0]);
                    }

                    if (parts.length > 1) {
                        targetId = parts[1];
                    } else {
                        errors.add("Href target " + href + " is missing a target id.");
                    }
                }

                if (idsPerFile.containsKey(targetFile)) {
                    List<String> ids = idsPerFile.get(targetFile);
                    if (!ids.contains(targetId)) {
                        errors.add("Href target " + href + " does not exist in file " + targetFile.getName());
                    }
                }
            }
        }

        return errors;
    }
}
