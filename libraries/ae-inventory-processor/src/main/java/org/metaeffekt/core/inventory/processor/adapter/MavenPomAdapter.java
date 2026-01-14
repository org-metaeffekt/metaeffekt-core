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
package org.metaeffekt.core.inventory.processor.adapter;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class MavenPomAdapter {

    private final Model model;
    private final Properties properties;

    public MavenPomAdapter(File pomFile) throws IOException, XmlPullParserException {
        try (FileInputStream fis = new FileInputStream(pomFile)) {
            final MavenXpp3Reader reader = new MavenXpp3Reader();
            model = reader.read(fis);
            properties = model.getProperties();

            final Build build = model.getBuild();
            if (build != null) {
                build.setExtensions(null);
                if (build.getPlugins() != null) {
                    for (Plugin plugin : build.getPlugins()) {
                        plugin.setExtensions(false);
                    }
                }
            }
        }
    }

    public String resolveArtifactId() {
        return model.getArtifactId();
    }

    public String resolveGroupId() {
        String groupId = resolveProperty(model.getGroupId(), properties, model);
        if (groupId == null) {
            final Parent parent = model.getParent();
            if (parent != null) {
                groupId = resolveProperty(parent.getGroupId(), properties, model);
            }
        }
        return groupId;
    }

    public String resolveVersion() {
        String version = resolveProperty(model.getVersion(), properties, model);
        if (version == null) {
            final Parent parent = model.getParent();
            if (parent != null) {
                version = resolveProperty(parent.getVersion(), properties, model);
            }
        }
        return version;
    }

    protected String resolveProperty(String value, Properties properties, Model model) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("${") && value.endsWith("}")) {
            String key = value.substring(2, value.length() - 1);
            return properties.getOrDefault(key, model.getProperties().getProperty(key)).toString();
        }
        return value;
    }

    public static String buildPurl(String groupId, String artifactId, String version) {
        if (groupId != null && artifactId != null && version != null) {
            return String.format("pkg:maven/%s/%s@%s", groupId, artifactId, version);
        }
        return null;
    }

}
