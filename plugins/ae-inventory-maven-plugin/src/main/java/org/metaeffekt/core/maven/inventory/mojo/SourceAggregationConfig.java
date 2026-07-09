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
package org.metaeffekt.core.maven.inventory.mojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class SourceAggregationConfig {

    private Map<String, String> properties = new HashMap<>();

    private List<String> sourceUrls = new ArrayList<>();

    private ImplicitConfig exclude = new ImplicitConfig();
    private ImplicitConfig include = new ImplicitConfig();

    private boolean defaultImplicitInclusion = false;
    private boolean defaultNoLicenseExclusion = true;

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<String> getSourceUrls() {
        return sourceUrls;
    }

    public void setSourceUrls(List<String> sourceUrls) {
        this.sourceUrls = sourceUrls;
    }

    public ImplicitConfig getExclude() {
        return exclude;
    }

    public void setExclude(ImplicitConfig exclude) {
        this.exclude = exclude;
    }

    public ImplicitConfig getInclude() {
        return include;
    }

    public void setInclude(ImplicitConfig include) {
        this.include = include;
    }

    public boolean isDefaultImplicitInclusion() {
        return defaultImplicitInclusion;
    }

    public void setDefaultImplicitInclusion(boolean defaultImplicitInclusion) {
        this.defaultImplicitInclusion = defaultImplicitInclusion;
    }

    public boolean isDefaultNoLicenseExclusion() {
        return defaultNoLicenseExclusion;
    }

    public void setDefaultNoLicenseExclusion(boolean defaultNoLicenseExclusion) {
        this.defaultNoLicenseExclusion = defaultNoLicenseExclusion;
    }

    public static class ImplicitConfig {
        private List<String> licenses = new ArrayList<>();
        private List<String> patterns = new ArrayList<>();

        public List<String> getLicenses() {
            return licenses;
        }

        public void setLicenses(List<String> licenses) {
            this.licenses = licenses;
        }

        public List<String> getPatterns() {
            return patterns;
        }

        public void setPatterns(List<String> patterns) {
            this.patterns = patterns;
        }
    }

    public static SourceAggregationConfig load(java.io.File sourceAggregationConfig) throws java.io.IOException {
        SourceAggregationConfig config = new SourceAggregationConfig();
        if (sourceAggregationConfig != null && sourceAggregationConfig.exists()) {
            org.yaml.snakeyaml.LoaderOptions loaderOptions = new org.yaml.snakeyaml.LoaderOptions();
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.Constructor(SourceAggregationConfig.class, loaderOptions));
            try (java.io.InputStream in = new java.io.FileInputStream(sourceAggregationConfig)) {
                config = yaml.load(in);
                if (config == null) {
                    config = new SourceAggregationConfig();
                }
            }
        }
        return config;
    }
}
