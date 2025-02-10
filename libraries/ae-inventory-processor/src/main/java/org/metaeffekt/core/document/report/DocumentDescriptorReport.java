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
package org.metaeffekt.core.document.report;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.document.model.DocumentPart;
import org.metaeffekt.core.document.model.DocumentPartType;
import org.metaeffekt.core.inventory.processor.model.InventoryContext;
import org.metaeffekt.core.inventory.processor.report.ReportUtils;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.PropertiesUtils;
import org.metaeffekt.core.util.RegExUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Class responsible for generating reports for document descriptors. It facilitates the creation of a DITA BookMap
 * and other report-related tasks by integrating with the Velocity templating engine.
 * <p>
 * This class manages the process of applying inventory context information to Velocity templates and producing final
 * reports (typically in DITA format). It reads template resources from the classpath, applies necessary context data,
 * and outputs the results to the specified target directory.
 * </p>
 *
 * @see DocumentDescriptor
 * @see InventoryContext
 * @see org.apache.velocity.Template
 */
@Slf4j
@Getter
@Setter
public class DocumentDescriptorReport {

    /**
     * The target path where to produce the reports.
     */
    private File targetReportDir;

    private static final String SEPARATOR_SLASH = "/";
    private static final String PATTERN_ANY_VT = "**/*.vt";
    private static final String TEMPLATES_BASE_DIR = "/META-INF/templates";

    // FIXME-RTU: divide into documentType based template groups and documentPartType based template groups
    public static final String TEMPLATE_GROUP_ANNEX_BOOKMAP = "annex-bookmap";
    public static final String TEMPLATE_GROUP_VULNERABILITY_REPORT_BOOKMAP = "vulnerability-report-bookmap";
    public static final String TEMPLATE_GROUP_VULNERABILITY_STATISTICS_REPORT_BOOKMAP = "vulnerability-statistics-report-bookmap";
    public static final String TEMPLATE_GROUP_VULNERABILITY_SUMMARY_REPORT_BOOKMAP = "vulnerability-summary-report-bookmap";

    /**
     * Creates a report based on the given DocumentDescriptor. This method will generate a DITA BookMap and other reports
     * using the template specified in the report generation process.
     *
     * @param documentDescriptor the descriptor containing document-specific information for the report generation
     * @throws IOException if there is an error reading or writing report files
     */
    protected void createPartBookMap(DocumentDescriptor documentDescriptor) throws IOException {
        for (DocumentPart documentPart : documentDescriptor.getDocumentParts()) {
            if (documentPart.getDocumentPartType() == DocumentPartType.ANNEX) {
                writeReports(documentDescriptor, documentPart, new DocumentDescriptorReportAdapters(), TEMPLATE_GROUP_ANNEX_BOOKMAP);
            }
            if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_REPORT) {
                writeReports(documentDescriptor, documentPart, new DocumentDescriptorReportAdapters(), TEMPLATE_GROUP_VULNERABILITY_REPORT_BOOKMAP);
            }
            if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_STATISTICS_REPORT) {
                writeReports(documentDescriptor, documentPart, new DocumentDescriptorReportAdapters(), TEMPLATE_GROUP_VULNERABILITY_STATISTICS_REPORT_BOOKMAP);
            }
            if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_SUMMARY_REPORT) {
                writeReports(documentDescriptor, documentPart, new DocumentDescriptorReportAdapters(), TEMPLATE_GROUP_VULNERABILITY_SUMMARY_REPORT_BOOKMAP);
            }
        }
    }

    /**
     * Method for creating the bookMap for the final document. This bookMap contains references to each part bookMap.
     *
     * @param documentDescriptor the descriptor containing document-specific information for the report generation
     * @throws IOException if there is an error reading or writing report files
     */
    protected void createDocumentBookMap(DocumentDescriptor documentDescriptor) throws IOException {
        // Collect the file names of the generated part bookmaps.
        List<String> partBookMaps = new ArrayList<>();
        for (DocumentPart documentPart : documentDescriptor.getDocumentParts()) {
            String bookMapFilename = null;
            if (documentPart.getDocumentPartType() == DocumentPartType.ANNEX) {
                bookMapFilename = "map_" + documentPart.getIdentifier() + "-annex.ditamap";
            } else if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_REPORT) {
                bookMapFilename = "map_" + documentPart.getIdentifier() + "-vulnerability-report.ditamap";
            } else if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_STATISTICS_REPORT) {
                bookMapFilename = "map_" + documentPart.getIdentifier() + "-vulnerability-statistics-report.ditamap";
            } else if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_SUMMARY_REPORT) {
                bookMapFilename = "map_" + documentPart.getIdentifier() + "-summary-report.ditamap";
            }
            if (bookMapFilename != null) {
                partBookMaps.add(bookMapFilename);
            }
        }

        // Specify the overall document bookmap template and target file.
        String templateResourcePath = TEMPLATES_BASE_DIR + "/document-bookmap/map_document.ditamap.vt";
        File targetFile = new File(this.targetReportDir, "map_" + documentDescriptor.getIdentifier() + "-document.ditamap");

        log.info("Producing Dita for template [{}]", templateResourcePath);

        // Build extra context with the list of part bookmaps.
        Map<String, Object> extraContext = new HashMap<>();
        extraContext.put("partBookMaps", partBookMaps);

        // Call the unified produceBookMapDita() method.
        produceBookMapDita(documentDescriptor,
                null,
                new DocumentDescriptorReportAdapters(),
                templateResourcePath,
                targetFile,
                extraContext);
    }

    /**
     * Adapter class for holding properties related to document report generation.
     * <p>This class acts as a container for properties associated with each inventory context. It allows dynamic
     * manipulation and management of properties during the report creation process.</p>
     */
    @Setter
    @Getter
    public static class DocumentDescriptorReportAdapters {
            Map<String, Properties> propertiesMap = new HashMap<>();

        private DocumentDescriptorReportAdapters() {
        }
    }

    /**
     * Adds inventory-specific properties to the given {@link DocumentDescriptorReportAdapters} object.
     * This method loads the properties from the inventory's report properties file and adds them to the context used
     * in DITA creation.
     *
     * @param documentDescriptor the document descriptor used to extract inventory contexts
     * @param adapters the adapters object to which the properties will be added
     */
    private void addPropertiesToAdapter(DocumentDescriptor documentDescriptor, DocumentDescriptorReportAdapters adapters){
        for (DocumentPart documentPart : documentDescriptor.getDocumentParts()) {
            for (InventoryContext inventoryContext : documentPart.getInventoryContexts()) {
                if (documentPart.getDocumentPartType() == DocumentPartType.ANNEX){
                    Properties properties = PropertiesUtils.loadPropertiesFile(new File (targetReportDir + "/" + inventoryContext.getIdentifier() + "/inventory-report.properties"));
                    adapters.getPropertiesMap().put(inventoryContext.getIdentifier(), properties);
                }
                if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_REPORT){
                    Properties properties = PropertiesUtils.loadPropertiesFile(new File (targetReportDir + "/" + inventoryContext.getIdentifier() + "/vulnerability-report.properties"));
                    adapters.getPropertiesMap().put(inventoryContext.getIdentifier(), properties);
                }
                if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_STATISTICS_REPORT){
                    Properties properties = PropertiesUtils.loadPropertiesFile(new File (targetReportDir + "/" + inventoryContext.getIdentifier() + "/vulnerability-statistics-report.properties"));
                    adapters.getPropertiesMap().put(inventoryContext.getIdentifier(), properties);
                }
                if (documentPart.getDocumentPartType() == DocumentPartType.VULNERABILITY_SUMMARY_REPORT){
                    Properties properties = PropertiesUtils.loadPropertiesFile(new File (targetReportDir + "/" + inventoryContext.getIdentifier() + "/vulnerability-summary-report.properties"));
                    adapters.getPropertiesMap().put(inventoryContext.getIdentifier(), properties);
                }
            }
        }
    }

    /**
     * Writes the generated reports based on the provided document descriptor and adapters.
     * <p>This method resolves Velocity template files and applies them to generate reports in the target report
     * directory. It iterates through all relevant templates and produces the final report output.</p>
     *
     * @param documentDescriptor the document descriptor containing the metadata for report generation
     * @param adapters the adapters holding additional properties to be used in the templates
     * @param templateGroup the group of templates to be applied for report generation
     * @param documentPart the part of a document for which the report will be written
     * @throws IOException if there is an error reading templates or writing reports
     */
    protected void writeReports(DocumentDescriptor documentDescriptor, DocumentPart documentPart,
                                DocumentDescriptorReportAdapters adapters, String templateGroup)
            throws IOException {

        addPropertiesToAdapter(documentDescriptor, adapters);

        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final String vtClasspathResourcePattern = TEMPLATES_BASE_DIR + SEPARATOR_SLASH + templateGroup + SEPARATOR_SLASH + PATTERN_ANY_VT;
        final Resource[] resources = resolver.getResources(vtClasspathResourcePattern);
        final Resource parentResource = resolver.getResource(TEMPLATES_BASE_DIR);
        final String parentPath = parentResource.getURI().toASCIIString();

        for (Resource r : resources) {
            String filePath = r.getURI().toASCIIString();
            String path = filePath.replace(parentPath, "");
            filePath = TEMPLATES_BASE_DIR + path;
            String originalFileName = Objects.requireNonNull(r.getFilename()).replace(".vt", "");

            // Modify filename only if it starts with "map_"
            String targetFileName;
            if (originalFileName.startsWith("map_")) {
                int splitIndex = originalFileName.indexOf("_") + 1; // Position after "map_"
                targetFileName = "map_" + documentPart.getIdentifier() + "-" + originalFileName.substring(splitIndex);
            } else {
                targetFileName = originalFileName;
            }

            File relPath = new File(path.replace("/" + templateGroup + "/", "")).getParentFile();
            final File targetReportPath = new File(this.targetReportDir, new File(relPath, targetFileName).toString());

            produceBookMapDita(documentDescriptor, documentPart, adapters, filePath, targetReportPath);
        }
    }

    /**
     * Produces a DITA report by applying a Velocity template to the given context data and writing the result to the target file.
     *
     * @param documentDescriptor the document descriptor containing the metadata for report generation
     * @param adapters the adapters containing properties to be used in the report
     * @param templateResourcePath the path to the Velocity template resource
     * @param target the file where the generated report will be saved
     * @param documentPart the part of the document for which the bookMap will be generated
     * @throws IOException if there is an error during the report generation process
     */
    private void produceBookMapDita(DocumentDescriptor documentDescriptor,
                                    DocumentPart documentPart,
                                    DocumentDescriptorReportAdapters adapters,
                                    String templateResourcePath,
                                    File target,
                                    Map<String, Object> extraContext) throws IOException {

        log.info("Producing Dita for template [{}]", templateResourcePath);
        final Properties properties = new Properties();
        properties.put(Velocity.RESOURCE_LOADER, "class, file");
        properties.put("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        properties.put(Velocity.INPUT_ENCODING, FileUtils.ENCODING_UTF_8);
        properties.put(Velocity.OUTPUT_ENCODING, FileUtils.ENCODING_UTF_8);
        properties.put(Velocity.SET_NULL_ALLOWED, true);

        final VelocityEngine velocityEngine = new VelocityEngine(properties);
        final Template template = velocityEngine.getTemplate(templateResourcePath);
        final StringWriter sw = new StringWriter();
        final VelocityContext context = new VelocityContext();

        // Add common context entries.
        context.put("targetReportDir", this.targetReportDir);
        context.put("StringEscapeUtils", org.apache.commons.lang.StringEscapeUtils.class);
        context.put("RegExUtils", RegExUtils.class);
        context.put("utils", new ReportUtils());
        context.put("Double", Double.class);
        context.put("Float", Float.class);
        context.put("String", String.class);

        context.put("documentDescriptor", documentDescriptor);
        if (documentPart != null) {
            context.put("documentPart", documentPart);
        }
        context.put("documentDescriptorReportAdapters", adapters);

        // Add any additional context provided.
        if (extraContext != null) {
            for (Map.Entry<String, Object> entry : extraContext.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }

        template.merge(context, sw);

        FileUtils.write(target, sw.toString(), "UTF-8");
    }

    /**
     * Overloaded produceBookMapDita() for cases where no extra context is required.
     *
     * @param documentDescriptor the document descriptor containing the metadata for report generation
     * @param documentPart the part of the document for which the bookMap will be generated
     * @param adapters the adapters containing properties to be used in the report
     * @param templateResourcePath the path to the Velocity template resource
     * @param target the file where the generated report will be saved
     */
    private void produceBookMapDita(DocumentDescriptor documentDescriptor,
                                    DocumentPart documentPart,
                                    DocumentDescriptorReportAdapters adapters,
                                    String templateResourcePath,
                                    File target) throws IOException {
        produceBookMapDita(documentDescriptor, documentPart, adapters, templateResourcePath, target, Collections.emptyMap());
    }
}


