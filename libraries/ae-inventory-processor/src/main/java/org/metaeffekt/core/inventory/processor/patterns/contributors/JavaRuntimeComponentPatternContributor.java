/*
 * Copyright 2009-2022 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.util.FileUtils;
import org.metaeffekt.core.util.PropertiesUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class JavaRuntimeComponentPatternContributor extends ComponentPatternContributor {

    @Override
    public boolean applies(String pathInContext) {
        return pathInContext.endsWith("/release");
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        try {
            final File anchorFile = new File(baseDir, relativeAnchorPath);

            final ReleasedPackageData rpd = parsePackageData(anchorFile);

            if (StringUtils.isNotEmpty(rpd.componentPart)) {

                // construct component pattern
                final ComponentPatternData componentPatternData = new ComponentPatternData();
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorFile.getName());
                componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_NAME, rpd.componentName);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_VERSION, rpd.version);
                componentPatternData.set(ComponentPatternData.Attribute.COMPONENT_PART, rpd.componentPart);
                componentPatternData.set("Release", rpd.release);

                componentPatternData.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "**/*");

                componentPatternData.set(Constants.KEY_TYPE, Constants.ARTIFACT_TYPE_PACKAGE);

                return Collections.singletonList(componentPatternData);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    public static class ReleasedPackageData {
        String name;
        String os;
        String arch;
        String type;
        String runtimeVersion;
        String implementorVersion;
        String url;
        String implementor;
        String version;
        String release;

        String componentPart;
        String componentName;

    }

    private ReleasedPackageData parsePackageData(File anchorFile) throws IOException {
        final ReleasedPackageData releasedPackageData = new ReleasedPackageData();

        parseReleaseFile(anchorFile, releasedPackageData);

        // check whether properties exist
        final File file = FileUtils.findSingleFile(anchorFile.getParentFile(), "*.properties");
        if (file != null) {
            parseAddonProperties(releasedPackageData, file);
        }

        modulatePackageData(releasedPackageData);

        return releasedPackageData;
    }

    private void parseAddonProperties(ReleasedPackageData releasedPackageData, File file) {
        final Properties properties = PropertiesUtils.loadPropertiesFile(file);

        if (releasedPackageData.implementorVersion == null) {
            releasedPackageData.implementorVersion = properties.getProperty("java.vendor.version");
        }
        if (releasedPackageData.implementor == null) {
            releasedPackageData.implementor = properties.getProperty("java.vm.vendor");
        }
    }

    private ReleasedPackageData parseReleaseFile(File anchorFile, ReleasedPackageData data) {
        final Properties p = PropertiesUtils.loadPropertiesFile(anchorFile);

        data.name = anchorFile.getParentFile().getName();

        data.implementor = parseProperty(p, "IMPLEMENTOR", "Java");
        data.implementorVersion = parseProperty(p, "IMPLEMENTOR_VERSION", null);
        data.version = parseProperty(p, "JAVA_VERSION", null);
        data.runtimeVersion = parseProperty(p, "JAVA_RUNTIME_VERSION", null);
        data.url = parseProperty(p, "SOURCE_REPO", null);
        data.type = parseProperty(p, "IMAGE_TYPE", "jdk");
        data.arch = parseProperty(p, "OS_ARCH", null);
        data.os = parseProperty(p, "OS_NAME", null);

        return data;
    }

    private void modulatePackageData(ReleasedPackageData data) {
        // map known constructs
        if ("Oracle Corporation".equals(data.implementor)) {
            data.implementor = "Oracle";
        }

        boolean evidenceForOpenJdk = false;
        evidenceForOpenJdk |= data.name.toLowerCase().contains("openjdk");
        evidenceForOpenJdk |= data.implementor.toLowerCase().contains("openjdk");

        if (StringUtils.isNotBlank(data.implementorVersion)) {
            evidenceForOpenJdk |= data.implementor.toLowerCase().contains("openjdk");
        }

        // derive consolidated information
        String name = data.implementor.toLowerCase();
        String prefix = (evidenceForOpenJdk ? "Open" : "") + data.type.toUpperCase();

        String extendedVersion = data.implementorVersion;
        if (StringUtils.isBlank(extendedVersion)) {
            extendedVersion = data.runtimeVersion;
        }

        if (StringUtils.isNotBlank(extendedVersion)) {
            int versionIndex = extendedVersion.indexOf(data.version);

            if (versionIndex != -1) {
                data.release = extendedVersion.substring(versionIndex + data.version.length() + 1);
            }

            if (versionIndex > 0) {
                prefix = extendedVersion.substring(0, versionIndex -1);
                if (prefix.length() > 1) {
                    name = prefix;
                }
            }
        }

        data.componentPart = name.toLowerCase().replace(" ", "-");

        data.componentPart += "-" + (evidenceForOpenJdk ? "open" : "") + data.type.toLowerCase();
        data.componentPart += "-" + data.version;

        data.componentName = data.implementor + " " + prefix;
    }

    private String parseProperty(Properties p, String key, String defaultValue) {
        final String v = p.getProperty(key);
        if (v == null) return defaultValue;
        if (v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

}
