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
package org.metaeffekt.core.inventory.processor.patterns.contributors.web;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class collect different information items on a web module.
 */
@Data
public class WebModule implements Comparable<WebModule> {

    /**
     * The path here on WebModule level specifies the anchor file from which the WebModule was derived. Please note,
     * that is contrast to paths in NpmModule this path does not represent the path under which the module can
     * be resolved.
     */
    private String path;

    // the anchor may be any file that belongs to the module
    private File anchor;
    private String anchorChecksum;

    // there may be one or more lock files for a component; these support to determine the transitive dependencies and
    // version
    private List<File> lockFiles = new ArrayList<>();

    // in case name or version are not defined; we consider the web module as abstract
    private String name;
    private String version;

    private String license;

    private String url;

    @Override
    public int compareTo(WebModule o) {
        return path.compareToIgnoreCase(o.path);
    }

    /**
     * Direct dependencies detected for the web module. An empty list means not specified.
     */
    private List<WebModuleDependency> directDependencies = new ArrayList<>();

}
