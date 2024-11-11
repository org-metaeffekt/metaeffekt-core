package org.metaeffekt.core.document.report;

import lombok.Getter;
import lombok.Setter;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.metaeffekt.core.document.model.DocumentDescriptor;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.InventoryReport;
import org.metaeffekt.core.inventory.processor.report.ReportContext;
import org.metaeffekt.core.inventory.processor.report.ReportUtils;
import org.metaeffekt.core.inventory.processor.report.adapter.AssessmentReportAdapter;
import org.metaeffekt.core.inventory.processor.report.adapter.AssetReportAdapter;
import org.metaeffekt.core.inventory.processor.report.adapter.VulnerabilityReportAdapter;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.RegExUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

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
    public static final String TEMPLATE_GROUP_INVENTORY_REPORT_BOOKMAP = "inventory-report-bookmap";
    private String templateLanguageSelector = "en";

    /*
    next steps:

    - extract functionality of writeReports() & produceDita() from InventoryReport.java
    - change process to create BookMaps by passing a DocumentDescriptor instead of an Inventory

    - the process of bookmap creation is controlled by DocumentDescriptorReportGenerator.java
    - creation of ditas for multiple inventories stays the way it's currently handled using InventoryReport.java
     */

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReport.class);

    protected boolean createBookMap(DocumentDescriptor documentDescriptor) {
        return false;
    }

    protected void writeReports(Inventory projectInventory, Inventory filteredInventory, InventoryReport.InventoryReportAdapters inventoryReportAdapters, String templateBaseDir, String templateGroup, ReportContext reportContext) throws Exception {
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

            produceBookMap(
                    projectInventory, filteredInventory,
                    inventoryReportAdapters.getAssetReportAdapter(),
                    inventoryReportAdapters.getVulnerabilityReportAdapter(),
                    inventoryReportAdapters.getAssessmentReportAdapter(),
                    filePath, targetReportPath, reportContext
            );
        }
    }

    private void produceBookMap(Inventory projectInventory, Inventory filteredInventory, AssetReportAdapter assetReportAdapter,
                                VulnerabilityReportAdapter vulnerabilityReportAdapter, AssessmentReportAdapter assessmentReportAdapter,
                                String templateResourcePath, File target, ReportContext reportContext) throws IOException {

        LOG.info("Producing BookMap for template [{}]", templateResourcePath);

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

        // regarding the report we only use the filtered inventory for the time being
        context.put("inventory", filteredInventory);
        context.put("vulnerabilityAdapter", vulnerabilityReportAdapter);
        context.put("assessmentReportAdapter", assessmentReportAdapter);
        context.put("assetAdapter", assetReportAdapter);
        context.put("report", this);
        context.put("StringEscapeUtils", org.apache.commons.lang.StringEscapeUtils.class);
        context.put("RegExUtils", RegExUtils.class);
        context.put("utils", new ReportUtils());

        context.put("Double", Double.class);
        context.put("Float", Float.class);
        context.put("String", String.class);

        context.put("targetReportDir", this.targetReportDir);

        context.put("reportContext", reportContext);

        template.merge(context, sw);

        FileUtils.write(target, sw.toString(), "UTF-8");
    }

    private String deriveTemplateBaseDir() {
        return TEMPLATES_BASE_DIR + SEPARATOR_SLASH + getTemplateLanguageSelector();
    }
}


