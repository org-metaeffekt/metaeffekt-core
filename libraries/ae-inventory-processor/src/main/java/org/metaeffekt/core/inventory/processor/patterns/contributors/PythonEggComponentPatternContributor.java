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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PythonEggComponentPatternContributor extends ComponentPatternContributor {
    private static final Logger LOG = LoggerFactory.getLogger(PythonEggComponentPatternContributor.class);

    // TODO: unify suffixes and other checks like in "applies"
    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<String>(){{
        add("egg-info/pkg-info");
    }});

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("EGG-INFO/PKG-INFO");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum, EvaluationContext evaluationContext) {

        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File anchorParentDir = anchorFile.getParentFile().getParentFile();

        // compute semaphore to see whether this component has been already processed
        final Object semaphore = getClass().getCanonicalName() + "/" + anchorParentDir.getAbsolutePath();

        // skip if component was already detected with a different anchor
        if (evaluationContext.isProcessed(semaphore)) {
            return Collections.emptyList();
        }

        final File pkgInfoFile = anchorFile;

        // this is the root of the component
        final File contextBaseDir = anchorParentDir.getParentFile();
        final String anchorPathInContext = FileUtils.asRelativePath(contextBaseDir, anchorFile);

        Properties properties = new Properties();
        try {
            List<String> strings = FileUtils.readLines(pkgInfoFile, StandardCharsets.UTF_8);
            for (String s : strings) {
                int index = s.indexOf(":");
                if (index != -1) {
                    String key = s.substring(0, index).trim();
                    String value = s.substring(index + 1).trim();

                    // do not allow overwrites; first key/value pair wins
                    if (!properties.containsKey(key)) {
                        properties.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }

        String componentName = properties.getProperty("Name");
        String componentVersion = properties.getProperty("Version");

        if (componentName == null || componentVersion == null) {
            LOG.warn("Could not determine component name or version of python egg. Skipping.");
            return Collections.emptyList();
        }

        String componentPart = componentName + "-" + componentVersion;

        // not yet propagated details
        String homepage = properties.getProperty("Home-page");
        String summary = properties.getProperty("Summary");
        String licenseExpression = properties.getProperty("License");

        // construct component pattern
        final ComponentPatternData componentPatternData = new ComponentPatternData();
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorPathInContext);
        componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, componentName);
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, componentVersion);
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, componentPart);
        componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART_PATH, anchorParentDir.getName());

        componentPatternData.set(ComponentPatternData.Attribute.EXCLUDE_PATTERN,
                "**/node_modules/**/*" + ", **/bower_components/**/*");
        componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN,
                anchorParentDir.getName() + "/**/*");

        componentPatternData.set(ComponentPatternData.Attribute.SHARED_INCLUDE_PATTERN, "**/*.exe");

        componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_MODULE);
        componentPatternData.set(Constants.KEY_COMPONENT_SOURCE_TYPE, "python-library");

        String purl = buildPurl(componentName, componentVersion);
        componentPatternData.set(Artifact.Attribute.PURL.getKey(), purl);

        evaluationContext.registerProcessed(semaphore);

        return Collections.singletonList(componentPatternData);
    }

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    private String buildPurl(String name, String version) {
        // first we have to handle that the name should not be case sensitive and underscore should be replaced by dash
        // see: https://github.com/package-url/purl-spec/blob/master/PURL-TYPES.rst#pypi
        name = name.toLowerCase(Locale.ENGLISH).replace("_", "-");
        return "pkg:pypi/" + name + "@" + version;
    }

}
