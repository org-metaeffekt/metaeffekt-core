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

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.inventory.resolver.ServerCredential;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class SourceAggregationConfig {

    private Map<String, String> properties = new HashMap<>();

    private List<String> sourceUrls = new ArrayList<>();

    private List<ServerCredential> credentials = new ArrayList<>();

    private List<TargetFolderMapping> targetFolderMappings = new ArrayList<>();

    private ImplicitConfig exclude = new ImplicitConfig();
    private ImplicitConfig include = new ImplicitConfig();

    private boolean defaultImplicitInclusion = false;
    private boolean defaultNoLicenseExclusion = true;

    @Setter
    @Getter
    public static class ImplicitConfig {
        private List<String> licenses = new ArrayList<>();
        private List<String> patterns = new ArrayList<>();

    }

    @Setter
    @Getter
    public static class TargetFolderMapping {
        private String urlPattern;
        private String targetFolder;

    }

    public static SourceAggregationConfig load(File sourceAggregationConfig) throws java.io.IOException {
        SourceAggregationConfig config = new SourceAggregationConfig();
        if (sourceAggregationConfig != null && sourceAggregationConfig.exists()) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(SourceAggregationConfig.class, loaderOptions));
            try (InputStream in = new FileInputStream(sourceAggregationConfig)) {
                config = yaml.load(in);
                if (config == null) {
                    config = new SourceAggregationConfig();
                }
            }
        }
        return config;
    }
}
