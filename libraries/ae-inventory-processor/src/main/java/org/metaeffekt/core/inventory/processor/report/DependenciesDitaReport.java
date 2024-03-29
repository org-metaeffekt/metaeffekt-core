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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.DependencyData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.*;

public class DependenciesDitaReport {

    private static final String DITA_REPORT_TEMPLATE =
            "/META-INF/templates/dependencies/tpc_dependencies-report.dita.vt";
    private static final String PROVIDED_SCOPE = "provided";
    private static final String[] VALID_CLASSIFIERS = {"api", "bootstrap", "config", "runtime"};
    private static final String[] VALID_SCOPES = {"compile", PROVIDED_SCOPE, "runtime"};
    private final Logger LOG = LoggerFactory.getLogger(DependenciesDitaReport.class);
    private String sourceInventoryPath;
    private String artifactNameMappingPath;
    private String targetDitaReportPath;
    private String artifactId;
    private List<DependencyData> dependencies;

    private Inventory sourceInventory;

    private Properties artifactNameMapping;

    private String internalArtifactPrefix = "ae-";


    public void createReport() throws Exception {
        if (sourceInventoryPath != null) {
            File file = new File(sourceInventoryPath);
            sourceInventory = InventoryUtils.readInventory(file.getParentFile(), file.getName());
        }

        if (artifactNameMappingPath != null) {
            Resource mappingResource = new ClassPathResource(artifactNameMappingPath);
            artifactNameMapping = new Properties();
            try {
                InputStream is = mappingResource.getInputStream();
                if (is != null) {
                    Reader reader = new InputStreamReader(is);
                    artifactNameMapping.load(reader);
                }
            } catch (IOException e) {
                LOG.error("Could not find artifactNameMappingPath: {}", artifactNameMappingPath);
                // continue without artifactNameMapping, internal module names will not be resolved
            }
        }

        DependencySummary internalDependencies = new DependencySummary();
        DependencySummary externalDependencies = new DependencySummary();

        for (DependencyData dependency : dependencies) {
            String artifactId = dependency.getArtifactId();
            String groupId = dependency.getGroupId();
            String classifier = dependency.getClassifier();
            String scope = dependency.getScope();
            String name = resolveName(artifactId);
            boolean optional = dependency.isOptional();

            if (classifier != null && !ArrayUtils.contains(VALID_CLASSIFIERS, classifier)) {
                continue;
            }
            if (scope != null && !ArrayUtils.contains(VALID_SCOPES, scope)) {
                continue;
            }

            DependencyData entry = new DependencyData();
            entry.setArtifactId(artifactId);
            entry.setGroupId(groupId);
            entry.setName(name);
            entry.setClassifier(classifier);
            entry.setScope(scope);
            entry.setOptional(optional);

            if (artifactId != null) {
                boolean internal = false;
                if (internalArtifactPrefix != null) {
                    String prefix = internalArtifactPrefix + "-";
                    internal = artifactId.toLowerCase().startsWith(prefix);
                }
                if (internal) {
                    internalDependencies.addDependency(entry);
                } else {
                    externalDependencies.addDependency(entry);
                }
            }
        }

        produceDita(internalDependencies, externalDependencies, DITA_REPORT_TEMPLATE, new File(
                targetDitaReportPath));

    }


    private String resolveName(String artifactId) {
        if (artifactNameMapping != null) {
            String name = artifactNameMapping.getProperty(artifactId);
            if (name != null) {
                return name;
            }
        }
        if (sourceInventory != null) {
            for (Artifact a : sourceInventory.getArtifacts()) {
                String idWithVersion = a.getId();
                if (idWithVersion == null) {
                    continue;
                }
                idWithVersion = idWithVersion.trim();
                String version = a.getVersion();
                if (version == null) {
                    continue;
                }
                version = version.trim();
                int i = idWithVersion.indexOf(version);
                if (i <= 1) {
                    continue;
                }
                String artifactIdWithoutVersion = idWithVersion.substring(0, i - 1);
                if (artifactIdWithoutVersion.equals(artifactId) && a.getComponent() != null) {
                    return a.getComponent();
                }
            }
        }
        return artifactId;
    }

    private void produceDita(DependencySummary internalDependencies,
                             DependencySummary externalDependencies, String templateResourcePath,
                             File target)
            throws Exception {
        String ENCODING_UTF_8 = "UTF-8";
        Properties properties = new Properties();
        properties.put(Velocity.RESOURCE_LOADER, "class, file");
        properties.put("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        properties.put(Velocity.INPUT_ENCODING, ENCODING_UTF_8);
        properties.put(Velocity.OUTPUT_ENCODING, ENCODING_UTF_8);

        VelocityEngine velocityEngine = new VelocityEngine(properties);
        Template template = velocityEngine.getTemplate(templateResourcePath);
        StringWriter sw = new StringWriter();
        VelocityContext context = new VelocityContext();

        context.put("report", this);

        context.put("internalDependencies", internalDependencies);
        context.put("externalDependencies", externalDependencies);
        context.put("projectId", artifactId);
        context.put("projectName", resolveName(artifactId));

        context.put("utils", new ReportUtils());

        template.merge(context, sw);

        FileUtils.write(target, sw.toString());
    }

    public String getSourceInventoryPath() {
        return sourceInventoryPath;
    }

    public void setSourceInventoryPath(String sourceInventoryPath) {
        this.sourceInventoryPath = sourceInventoryPath;
    }

    public String getArtifactNameMappingPath() {
        return artifactNameMappingPath;
    }

    public void setArtifactNameMappingPath(String artifactNameMappingPath) {
        this.artifactNameMappingPath = artifactNameMappingPath;
    }

    public String getTargetDitaReportPath() {
        return targetDitaReportPath;
    }

    public void setTargetDitaReportPath(String targetDitaReportPath) {
        this.targetDitaReportPath = targetDitaReportPath;
    }

    public void setDependencies(List<DependencyData> dependencies) {
        this.dependencies = dependencies;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getInternalArtifactPrefix() {
        return internalArtifactPrefix;
    }

    public void setInternalArtifactPrefix(String internalArtifactPrefix) {
        this.internalArtifactPrefix = internalArtifactPrefix;
    }

    public class DependencySummary {

        Map<String, DependencyData> dependencies = new HashMap<String, DependencyData>();

        public List<DependencyData> getDependencies() {
            List<DependencyData> result = new ArrayList<DependencyData>(dependencies.values());
            Collections.sort(result);
            return result;
        }

        public void addDependency(DependencyData d) {
            String artifactId = d.getArtifactId();
            if (dependencies.containsKey(artifactId)) {
                if (PROVIDED_SCOPE.equals(dependencies.get(artifactId).getScope())) {
                    if (!PROVIDED_SCOPE.equals(d.getScope())) {
                        dependencies.get(artifactId).setScope(d.getScope());
                    }
                }
            } else {
                this.dependencies.put(artifactId, d);
            }
        }

    }

}
