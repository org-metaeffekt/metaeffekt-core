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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.PdmParser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.PoetryParser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.PyProjectData;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.PyProjectParser;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;
import static org.metaeffekt.core.util.FileUtils.asRelativePath;

@Slf4j
public class PyProjectComponentPatternContributor extends ComponentPatternContributor {

    private static final List<PyProjectParser> PY_PROJECT_PARSERS = List.of(new PoetryParser(), new PdmParser());

    private static final List<String> suffixes = Collections.unmodifiableList(new ArrayList<>() {{
        add("pyproject.toml");
    }});

    @Override
    public List<String> getSuffixes() {
        return suffixes;
    }

    @Override
    public int getExecutionPhase() {
        return 1;
    }

    @Override
    public boolean applies(String pathInContext) {
        return suffixes.stream().anyMatch(pathInContext::endsWith);
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        final File anchorFile = new File(baseDir, relativeAnchorPath);
        final File contextBaseDir = anchorFile.getParentFile();
        final String anchorRelPath = asRelativePath(contextBaseDir, anchorFile);

        try {

            // the main anchor
            if (relativeAnchorPath.endsWith(".toml")) {
                final List<ComponentPatternData> list = new ArrayList<>();

                final File pyProjectToml = new File(baseDir, relativeAnchorPath);
                final ObjectMapper objectMapper = new TomlMapper();
                final JsonNode pyProjectRootNode = objectMapper.readTree(pyProjectToml);

                final PyProjectParser parser = findParser(pyProjectRootNode);
                if (parser == null) {
                    log.info("Unsupported pyproject.toml format: {}", pyProjectToml.getAbsolutePath());
                    return null;
                }

                final PyProjectData pyProjectData = parser.parse(pyProjectToml, pyProjectRootNode);
                final ResolvedModule projectModule = pyProjectData.getProjectModule();
                final List<UnresolvedModule> directDevelopmentDependencies = pyProjectData.getDirectDevelopmentDependencies();
                final List<UnresolvedModule> directRuntimeDependencies = pyProjectData.getDirectRuntimeDependencies();
                final List<ResolvedModule> resolvedModules = pyProjectData.getResolvedModulesFromLockFile();

                // here we have the
                // - a resolve project level-module
                // - unresolved direct dependencies
                // - all known resolved dependencies with their unresolved transitives

                // we want to
                // - create an inventory with artifacts representing the resolved modules
                // - direct development dependencies are marked with 'd' for project-level module
                // - indirect development dependencies are marked with '(d)' for project-level module
                // - direct runtime dependencies are marked with 'r' for project-level module
                // - indirect runtime dependencies are marked with '(r)' for project-level module

                Map<String, ResolvedModule> nameToResolvedModuleMap = new HashMap<>();
                for (ResolvedModule resolvedModule : resolvedModules) {
                    nameToResolvedModuleMap.put(resolvedModule.getName(), resolvedModule);
                }

                final Map<String, Artifact> nameToArtifactMap = new HashMap<>();
                final String projectAssetId = "AID-" + projectModule.deriveQualifier();

                // NOTE: here transitive development dependencies are starting from a direct dev dependencies and then uses transitive runtime deps
                final List<UnresolvedModule> indirectDevelopmentDependencies = extractIndirectDependencies(directDevelopmentDependencies, nameToResolvedModuleMap, ResolvedModule::getRuntimeDependencies);
                contributeDependencies(indirectDevelopmentDependencies, "(d)", projectAssetId, nameToArtifactMap, nameToResolvedModuleMap, relativeAnchorPath);
                contributeDependencies(directDevelopmentDependencies, "d", projectAssetId, nameToArtifactMap, nameToResolvedModuleMap, relativeAnchorPath);

                final List<UnresolvedModule> indirectRuntimeDependencies = extractIndirectDependencies(directRuntimeDependencies, nameToResolvedModuleMap, ResolvedModule::getRuntimeDependencies);
                contributeDependencies(indirectRuntimeDependencies, "(r)", projectAssetId, nameToArtifactMap, nameToResolvedModuleMap, relativeAnchorPath);
                contributeDependencies(directRuntimeDependencies, "r", projectAssetId, nameToArtifactMap, nameToResolvedModuleMap, relativeAnchorPath);

                Inventory inventory = new Inventory();
                inventory.getArtifacts().addAll(nameToArtifactMap.values());

//                resolvedModules.forEach(System.out::println);

                ComponentPatternData cpd = new ComponentPatternData();

                cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR, anchorRelPath);
                cpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                cpd.set(ComponentPatternData.Attribute.COMPONENT_NAME, projectModule.getName());
                cpd.set(ComponentPatternData.Attribute.COMPONENT_VERSION, projectModule.getVersion());
                cpd.set(ComponentPatternData.Attribute.COMPONENT_PART, projectModule.deriveQualifier());
                cpd.set(ComponentPatternData.Attribute.COMPONENT_PART_PATH, anchorRelPath);

                cpd.set(ComponentPatternData.Attribute.TYPE, ARTIFACT_TYPE_APPLICATION);
                cpd.set(ComponentPatternData.Attribute.COMPONENT_SOURCE_TYPE, "python-application");

                cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, parser.getIncludePattern());

                cpd.setExpansionInventorySupplier(() -> inventory);

                list.add(cpd);

                return list;
            }
        } catch (IOException e) {
            log.warn("Failure processing composer.lock file: [{}]", e.getMessage());
        }

        return Collections.emptyList();

    }

    private PyProjectParser findParser(JsonNode root) {
        return PY_PROJECT_PARSERS.stream().filter(parser -> parser.supports(root)).findFirst().orElse(null);
    }

    private List<UnresolvedModule> extractIndirectDependencies(List<UnresolvedModule> seedDependencies, Map<String, ResolvedModule> resolvedModules, Function<ResolvedModule, Map<String, UnresolvedModule>> supplier) {
        final Stack<UnresolvedModule> stack = new Stack<>();
        seedDependencies.forEach(stack::push);

        final List<UnresolvedModule> indirectDependencies = new ArrayList<>();
        while (!stack.isEmpty()) {
            UnresolvedModule unresolvedModule = stack.pop();

            if (!indirectDependencies.contains(unresolvedModule)) {
                ResolvedModule resolvedModule = resolvedModules.get(unresolvedModule.getName());
                if (resolvedModule == null) {
                    log.warn("Unable to resolve module [{}].", unresolvedModule.getName());
                    continue;
                }

                Map<String, UnresolvedModule> map = supplier.apply(resolvedModule);
                if (map != null) {
                    for (Map.Entry<String, UnresolvedModule> dependency : map.entrySet()) {
                        UnresolvedModule module = dependency.getValue();
                        indirectDependencies.add(module);
                        stack.push(module);
                    }
                }
            }
        }

        return indirectDependencies;
    }

    private void contributeDependencies(List<UnresolvedModule> dependencies, String dependencyType,
            String projectAssetId, Map<String, Artifact> nameToArtifactMap, Map<String, ResolvedModule> nameToResolvedModuleMap,
            String relativePath) {

        for (UnresolvedModule module : dependencies) {
            final String name = module.getName();
            final ResolvedModule resolvedModule = nameToResolvedModuleMap.get(name);

            if (resolvedModule == null) {
                log.warn("Cannot find resolved module for module name [{}]. Skipping.", name);
                continue;
            }

            final String version = resolvedModule.getVersion();
            final PyProjectPackageSource pyProjectPackageSource = resolvedModule.getPyProjectPackageSource();

            Artifact artifact = nameToArtifactMap.get(resolvedModule.getName());
            if (artifact == null) {
                artifact = new Artifact();
                artifact.setId(name + "-" + version);
                artifact.setVersion(version);
                artifact.setComponent(name);

                if (pyProjectPackageSource != null) {
                    artifact.set(KEY_PACKAGE_SOURCE, pyProjectPackageSource.reference());
                    artifact.set(KEY_PACKAGE_SOURCE_URL, pyProjectPackageSource.url());
                }
                artifact.set(Constants.KEY_PATH_IN_ASSET, relativePath + "[" + name + "]");

                // we cannot add a root path; there is no physical file that is part of the module
                // artifact.set(KEY_ROOT_PATHS, relativePath);

                artifact.set(KEY_TYPE, ARTIFACT_TYPE_MODULE);
                artifact.set(KEY_COMPONENT_SOURCE_TYPE, "python-module");

                artifact.set(KEY_PURL, buildPurl(name, version));

                nameToArtifactMap.put(name, artifact);
            }
            artifact.set(projectAssetId, dependencyType);
        }
    }

    private String buildPurl(String name, String version) {
        // first we have to handle that the name should not be case sensitive and underscore should be replaced by dash
        // see: https://github.com/package-url/purl-spec/blob/master/PURL-TYPES.rst#pypi
        name = name.toLowerCase(Locale.ENGLISH).replace("_", "-");
        return "pkg:pypi/" + name + "@" + version;
    }

}
