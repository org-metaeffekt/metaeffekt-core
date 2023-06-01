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
package org.metaeffekt.core.inventory.processor.patterns;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.*;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class ComponentPatternProducer {

    public static final String[] INCLUDE_PATTERNS_ORDERED_BY_PRIORITY = new String[]{
            // web modules
            // prioritize bower components; in general, better metadata first
            "bower_components/**/.bower.json",
            "bower_components/**/bower.json",
            "node_modules/**/package-lock.json",
            "node_modules/**/package.json",
            "package.json",
            ".bower.json",
            "bower.json",
            "composer.json",

            // container marker
            "json",

            // eclipse bundles
            "about.html",
            "about.ini",
            "about.properties",
            "about.mappings",

            // python modules
            "*.dist-info/METADATA",
            "*.dist-info/RECORD",
            "*.dist-info/WHEEL",
            "__init__.py",
            "__about__.py",
    };
    private static final Logger LOG = LoggerFactory.getLogger(ComponentPatternProducer.class);

    public void extractComponentPatterns(File baseDir, Inventory targetInventory) {

        final Set<String> uniqueFolder = new HashSet<>();
        final Map<String, String> uniqueFile = new HashMap<>();

        // collect all folders
        final String[] folders = FileUtils.scanDirectoryForFolders(baseDir, "**/*");

        final Set<String> fileSet = new HashSet<>(Arrays.asList(folders));
        final List<String> foldersByLength = new ArrayList<>(fileSet);
        Collections.sort(foldersByLength);
        Collections.sort(foldersByLength, Comparator.comparingInt(String::length));

        // configure contributors
        final List<ComponentPatternContributor> componentPatternContributors = new ArrayList<>();
        componentPatternContributors.add(new ContainerComponentPatternContributor());
        componentPatternContributors.add(new WebModuleComponentPatternContributor());
        componentPatternContributors.add(new UnwrappedEclipseBundleContributor());
        componentPatternContributors.add(new PythonModuleComponentPatternContributor());
        componentPatternContributors.add(new DefaultComponentPatternContributor());

        Set<String> qualifierSet = new HashSet<>();

        // for each include pattern (by priority) try to identify a component pattern
        for (String includePattern : INCLUDE_PATTERNS_ORDERED_BY_PRIORITY) {

            // process folders (ordered by path length)
            for (String folder : foldersByLength) {

                File contextBaseDir = new File(baseDir, folder);

                // FIXME: shouldn't this be an exact match rather that a subtree match; is this an optimization?
                // skip subtrees already covered
                boolean skip = false;
                for (String subtree : uniqueFolder) {
                    if (folder.startsWith(subtree + "/")) skip = true;
                }
                if (skip) {
                    continue;
                }

                // scan inside folder using the current include pattern
                String[] files = FileUtils.scanForFiles(contextBaseDir, includePattern, "--none--");

                for (String file : files) {
                    final File anchorFile = new File(contextBaseDir, file);
                    final File anchorParentDir = anchorFile.getParentFile();

                    // skip if there is already a component pattern available
                    if (uniqueFolder.contains(anchorParentDir.getAbsolutePath())) {
                        continue;
                    }

                    final String checksum = FileUtils.computeChecksum(anchorFile);
                    uniqueFile.put(checksum, new File(file).getAbsolutePath());
                    uniqueFolder.add(anchorParentDir.getAbsolutePath());

                    final Artifact artifact = new Artifact();
                    artifact.setId(anchorParentDir.getName());
                    artifact.setChecksum(checksum);

                    // construct component pattern
                    final ComponentPatternData componentPatternData = new ComponentPatternData();
                    componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorFile.getParentFile().getName() + "/" + anchorFile.getName());
                    componentPatternData.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, checksum);

                    // apply contributors
                    for (ComponentPatternContributor cpc : componentPatternContributors) {
                        if (cpc.applies(contextBaseDir, file, artifact)) {
                            cpc.contribute(contextBaseDir, file, artifact, componentPatternData);

                            // the first contributor wins
                            break;
                        }
                    }

                    LOG.info("Identified component pattern: " + componentPatternData.createCompareStringRepresentation());

                    // FIXME: defer to 2nd pass
                    final String version = componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_VERSION);
                    if (version != null && "unspecific".equalsIgnoreCase(version)) {
                        continue;
                    }

                    final String qualifier = componentPatternData.deriveQualifier();
                    if (!qualifierSet.contains(qualifier)) {
                        targetInventory.getComponentPatternData().add(componentPatternData);
                        qualifierSet.add(qualifier);
                    }
                }
            }
        }

        for (Map.Entry<String, String> entry : uniqueFile.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }


}
