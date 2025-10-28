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
package org.metaeffekt.core.inventory.processor.report;

import org.junit.Before;
import org.junit.Test;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.report.configuration.ReportConfigurationParameters;
import org.metaeffekt.core.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("resource")
public class ValidReferencesTest {

    final static File reportDir = new File("target/report/valid-references/");

    @Before
    public void setUp() throws Exception {
        final File inventoryDir = new File("src/test/resources/test-inventory-01/");

        InventoryReport report = new InventoryReport(ReportConfigurationParameters.builder()
                .inventoryBomReportEnabled(true)
                .inventoryDiffReportEnabled(true)
                .inventoryPomEnabled(true)
                .inventoryVulnerabilityReportEnabled(true)
                .inventoryVulnerabilityReportSummaryEnabled(true)
                .inventoryVulnerabilityStatisticsReportEnabled(true)
                .assetBomReportEnabled(true)
                .assessmentReportEnabled(true)
                .build());

        report.setReportContext(new ReportContext("test", "Test", "Test Context"));
        report.setReferenceLicensePath("licenses");
        report.setReferenceComponentPath("components");
        report.setInventory(InventoryUtils.readInventory(inventoryDir, "*.xls"));
        report.setTargetReportDir(new File(reportDir, "report"));

        reportDir.mkdirs();

        final File targetLicensesDir = new File(reportDir, "licenses");
        final File targetComponentDir = new File(reportDir, "components");
        report.setTargetLicenseDir(targetLicensesDir);
        report.setTargetComponentDir(targetComponentDir);

        report.setTargetInventoryDir(reportDir);
        report.setTargetInventoryPath("result.xls");

        report.createReport();
        FileUtils.copyFileToDirectory(new File(inventoryDir, "bm_test.ditamap"), reportDir);

    }

    @Test
    public void testValidLinksForEffectiveLicenses() throws IOException {
        File originTemplateFile = new File(reportDir, "report/tpc_inventory-licenses-effective.dita");
        File targetTemplateFile = new File(reportDir, "report/tpc_inventory-license-usage.dita");

        assertThat(originTemplateFile).exists();
        assertThat(targetTemplateFile).exists();

        assertThat(Files.lines(originTemplateFile.toPath())
                .anyMatch(l -> l.contains("href=\"tpc_inventory-license-usage.dita#tpc_effective_license_a-license\""))).isTrue();

        assertThat(Files.lines(targetTemplateFile.toPath())
                .anyMatch(l -> l.contains("<topic id=\"tpc_effective_license_a-license\">"))).isTrue();

        assertThat(Files.lines(originTemplateFile.toPath())
                .anyMatch(l -> l.contains("href=\"tpc_inventory-license-usage.dita#tpc_effective_license_g-license\""))).isTrue();

        assertThat(Files.lines(targetTemplateFile.toPath())
                .anyMatch(l -> l.contains("<topic id=\"tpc_effective_license_g-license\">"))).isTrue();

    }

    @Test
    public void testValidLinksForAssetLicenses() throws IOException {
        File originTemplateFile = new File(reportDir, "report/tpc_asset-licenses.dita");
        File targetTemplateFile = new File(reportDir, "report/tpc_asset-licenses.dita");

        assertThat(originTemplateFile).exists();
        assertThat(targetTemplateFile).exists();

        assertThat(Files.lines(originTemplateFile.toPath())
                .anyMatch(l -> l.contains("href=\"#tpc_associated_license_details_a-license\""))).isTrue();

        assertThat(Files.lines(targetTemplateFile.toPath())
                .anyMatch(l -> l.contains("<topic id=\"tpc_associated_license_details_a-license\">"))).isTrue();

        assertThat(Files.lines(originTemplateFile.toPath())
                .anyMatch(l -> l.contains("href=\"#tpc_associated_license_details_b-license\""))).isTrue();

        assertThat(Files.lines(targetTemplateFile.toPath())
                .anyMatch(l -> l.contains("<topic id=\"tpc_associated_license_details_b-license\">"))).isTrue();

    }

    @Test
    public void testTemplatesContainValidLinkTargets() {
        List<String> errors = DitaTemplateLinkValidator.validateDir(new File(reportDir, "report"));
        assertThat(errors).isEmpty();
    }

    static class DitaTemplateLinkValidator {

        static HashMap<File, List<String>> hrefTargetsPerFile = new HashMap<>();
        static HashMap<File, List<String>> idsPerFile = new HashMap<>();

        public static List<String> validateDir(File templateDir) {

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

        private static void collectHrefsAndIds(File templateFile, Document document) {

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

        private static List<String> validateLinks() {
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
}
