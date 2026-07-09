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

package org.metaeffekt.core.inventory.resolver;

import lombok.Setter;
import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PatternResolver {

    private final List<Handler> handlers = new ArrayList<>();
    @Setter
    private Handler fallbackHandler;

    public void addHandler(Handler handler) {
        this.handlers.add(handler);
    }

    public String resolve(String pattern) {
        if (pattern == null) return null;

        for (Handler handler : handlers) {
            if (handler.canHandle(pattern)) {
                return handler.resolve(pattern);
            }
        }
        if (fallbackHandler != null) {
            return fallbackHandler.resolve(pattern);
        }
        return pattern;
    }

    public abstract static class Handler {
        public abstract boolean canHandle(String pattern);
        public abstract String resolve(String pattern);
    }

    public static class PropertyPlaceholderHandler extends Handler {
        private final Properties properties;

        public PropertyPlaceholderHandler(Properties properties) {
            this.properties = properties;
        }

        @Override
        public boolean canHandle(String pattern) {
            return properties != null && properties.containsKey(pattern);
        }

        @Override
        public String resolve(String pattern) {
            return properties.getProperty(pattern);
        }
    }

    public static class ArtifactAttributeHandler extends Handler {
        private final Artifact artifact;

        public ArtifactAttributeHandler(Artifact artifact) {
            this.artifact = artifact;
        }

        @Override
        public boolean canHandle(String pattern) {
            return pattern != null && pattern.toLowerCase().startsWith("artifact.");
        }

        @Override
        public String resolve(String pattern) {
            String field = pattern.substring("artifact.".length());

            if (field.equalsIgnoreCase("namespace")) {
                String namespace = artifact.getGroupId();
                return namespace != null ? namespace : (artifact.get("PURL") != null ? artifact.get("PURL") : "");
            } else if (field.equalsIgnoreCase("name")) {
                String name = artifact.deriveArtifactId();
                return name != null ? name : "";
            } else if (field.equalsIgnoreCase("version")) {
                String version = artifact.getVersion();
                return version != null ? version : "";
            } else if (field.equalsIgnoreCase("groupId")) {
                String groupId = artifact.getGroupId();
                return groupId != null ? groupId : "";
            } else if (field.equalsIgnoreCase("id")) {
                String id = artifact.getId();
                return id != null ? id : "";
            } else if (field.equalsIgnoreCase("extension") || field.equalsIgnoreCase("type")) {
                String type = artifact.getType();
                return type != null ? type : "";
            }

            // fallback for any other artifact attributes requested
            String value = artifact.get(field);
            return value != null ? value : "";
        }
    }
}