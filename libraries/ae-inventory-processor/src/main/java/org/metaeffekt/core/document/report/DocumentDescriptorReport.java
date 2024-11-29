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
import org.metaeffekt.core.document.model.DocumentType;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

    public static final String TEMPLATE_GROUP_ANNEX_BOOKMAP = "annex-bookmap";
    public static final String TEMPLATE_GROUP_VULNERABILITY_REPORT_BOOKMAP = "vulnerability-report-bookmap";
    public static final String TEMPLATE_GROUP_VULNERABILITY_SUMMARY_REPORT_BOOKMAP = "vulnerability-summary-report-bookmap";

    private String templateLanguageSelector = "en";

    /**
     * Creates a report based on the given DocumentDescriptor. This method will generate a DITA BookMap and other reports
     * using the template specified in the report generation process.
     *
     * @param documentDescriptor the descriptor containing document-specific information for the report generation
     * @throws IOException if there is an error reading or writing report files
     */
    protected void createReport(DocumentDescriptor documentDescriptor) throws IOException {
        if (documentDescriptor.getDocumentType() == DocumentType.ANNEX) {
            writeReports(documentDescriptor, new DocumentDescriptorReportAdapters(), deriveTemplateBaseDir(), TEMPLATE_GROUP_ANNEX_BOOKMAP);
        }
        if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_REPORT) {
            writeReports(documentDescriptor, new DocumentDescriptorReportAdapters(), deriveTemplateBaseDir(), TEMPLATE_GROUP_VULNERABILITY_REPORT_BOOKMAP);
        }
        if (documentDescriptor.getDocumentType() == DocumentType.VULNERABILITY_SUMMARY_REPORT) {
            writeReports(documentDescriptor, new DocumentDescriptorReportAdapters(), deriveTemplateBaseDir(), TEMPLATE_GROUP_VULNERABILITY_SUMMARY_REPORT_BOOKMAP);
        }
    }

    /**
     * Adapter class for holding properties related to document report generation.
     * <p>This class acts as a container for properties associated with each inventory context. It allows dynamic
     * manipulation and management of properties during the report creation process.</p>
     */
    @Setter
    @Getter
    public static class DocumentDescriptorReportAdapters {
            Map<String, Properties> propertiesMap = new HashMap<String, Properties>();

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
     * @throws IOException if there is an error loading the properties file
     */
    private void addPropertiesToAdapter(DocumentDescriptor documentDescriptor, DocumentDescriptorReportAdapters adapters){
        for (InventoryContext inventoryContext : documentDescriptor.getInventoryContexts()) {
            Properties properties = PropertiesUtils.loadPropertiesFile(new File (targetReportDir + "/" + inventoryContext.getIdentifier() + "/inventory-report.properties"));
            adapters.getPropertiesMap().put(inventoryContext.getIdentifier(), properties);
        }
    }

    /**
     * Writes the generated reports based on the provided document descriptor and adapters.
     * <p>This method resolves Velocity template files and applies them to generate reports in the target report
     * directory. It iterates through all relevant templates and produces the final report output.</p>
     *
     * @param documentDescriptor the document descriptor containing the metadata for report generation
     * @param adapters the adapters holding additional properties to be used in the templates
     * @param templateBaseDir the base directory for loading the template files
     * @param templateGroup the group of templates to be applied for report generation
     * @throws IOException if there is an error reading templates or writing reports
     */
    protected void writeReports(DocumentDescriptor documentDescriptor, DocumentDescriptorReportAdapters adapters,
                    String templateBaseDir, String templateGroup) throws IOException {

        addPropertiesToAdapter(documentDescriptor, adapters);

        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final String vtClasspathResourcePattern = templateBaseDir + SEPARATOR_SLASH + templateGroup + SEPARATOR_SLASH + PATTERN_ANY_VT;
        final Resource[] resources = resolver.getResources(vtClasspathResourcePattern);
        final Resource parentResource = resolver.getResource(templateBaseDir);
        final String parentPath = parentResource.getURI().toASCIIString();

        for (Resource r : resources) {
            String filePath = r.getURI().toASCIIString();
            String path = filePath.replace(parentPath, "");
            filePath = templateBaseDir + path;
            String targetFileName = r.getFilename().replace(".vt", "");

            File relPath = new File(path.replace("/" + templateGroup + "/", "")).getParentFile();
            final File targetReportPath = new File(this.targetReportDir, new File(relPath, targetFileName).toString());

            produceDita(documentDescriptor, adapters, filePath, targetReportPath);
        }
    }

    /**
     * Produces a DITA report by applying a Velocity template to the given context data and writing the result to the target file.
     *
     * @param documentDescriptor the document descriptor containing the metadata for report generation
     * @param adapters the adapters containing properties to be used in the report
     * @param templateResourcePath the path to the Velocity template resource
     * @param target the file where the generated report will be saved
     * @throws IOException if there is an error during the report generation process
     */
    private void produceDita(DocumentDescriptor documentDescriptor, DocumentDescriptorReportAdapters adapters,
                    String templateResourcePath, File target) throws IOException {

        log.info("Producing BookMap for template [{}]", templateResourcePath);
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

        context.put("targetReportDir", this.targetReportDir);
        context.put("StringEscapeUtils", org.apache.commons.lang.StringEscapeUtils.class);
        context.put("RegExUtils", RegExUtils.class);
        context.put("utils", new ReportUtils());

        context.put("Double", Double.class);
        context.put("Float", Float.class);
        context.put("String", String.class);

        // regarding the report we only use the filtered inventory for the time being
        context.put("documentDescriptor", documentDescriptor);
        context.put("documentDescriptorReportAdapters", adapters);

        template.merge(context, sw);

        FileUtils.write(target, sw.toString(), "UTF-8");
    }

    private String deriveTemplateBaseDir() {
        return TEMPLATES_BASE_DIR + SEPARATOR_SLASH + getTemplateLanguageSelector();
    }

}


