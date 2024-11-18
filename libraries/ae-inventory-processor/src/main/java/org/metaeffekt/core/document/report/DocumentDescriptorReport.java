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
import org.metaeffekt.core.inventory.processor.report.ReportUtils;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.RegExUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

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

    private String templateLanguageSelector = "en";

    protected void createReport(DocumentDescriptor documentDescriptor) throws IOException {
        writeReports(documentDescriptor, new DocumentDescriptorReportAdapters(), deriveTemplateBaseDir(), TEMPLATE_GROUP_ANNEX_BOOKMAP);
    }

    @Getter
    public static class DocumentDescriptorReportAdapters {
        private DocumentDescriptorReportAdapters() {
            // yet empty adapters list
        }
    }

    protected void writeReports(DocumentDescriptor documentDescriptor, DocumentDescriptorReportAdapters adapters,
                    String templateBaseDir, String templateGroup) throws IOException {

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

        template.merge(context, sw);

        FileUtils.write(target, sw.toString(), "UTF-8");
    }

    private String deriveTemplateBaseDir() {
        return TEMPLATES_BASE_DIR + SEPARATOR_SLASH + getTemplateLanguageSelector();
    }

}


