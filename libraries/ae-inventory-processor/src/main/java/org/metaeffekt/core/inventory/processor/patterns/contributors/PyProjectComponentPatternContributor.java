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
import org.metaeffekt.core.inventory.processor.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static org.metaeffekt.core.inventory.processor.model.Constants.*;
import static org.metaeffekt.core.util.FileUtils.asRelativePath;

@Slf4j
public class PyProjectComponentPatternContributor extends ComponentPatternContributor {

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

                ObjectMapper objectMapper = new TomlMapper();
                JsonNode pyProjectRootNode = objectMapper.readTree(pyProjectToml);

                JsonNode projectNode = pyProjectRootNode.at("/tool/poetry");
                ResolvedModule projectModule = new ResolvedModule(projectNode.get("name").asText(), null);
                projectModule.setVersion(projectNode.get("version").asText());

                final List<UnresolvedModule> directRuntimeDependencies = extractDirectDependencies(projectNode, "/dependencies");
                final List<UnresolvedModule> directDevelopmentDependencies = extractDirectDependencies(projectNode, "/group/dev/dependencies");

                final File poetryLockFile = new File(pyProjectToml.getParentFile(), "poetry.lock");
                final ObjectMapper lockObjectMapper = new TomlMapper();
                final JsonNode lockNode = lockObjectMapper.readTree(poetryLockFile);

                // read the packages
                final List<ResolvedModule> resolvedModules = new ArrayList<>();
                lockNode.path("package").valueStream().forEach(packageNode -> {
                    final ResolvedModule resolvedModule = new ResolvedModule(packageNode.get("name").textValue(), null);
                    resolvedModule.setVersion(packageNode.get("version").textValue());

                    final PyProjectPackageSource source = parseSource(packageNode);
                    resolvedModule.setPyProjectPackageSource(source);

                    final JsonNode packageDependenciesNode = packageNode.path("dependencies");
                    final Map<String, UnresolvedModule> unresolvedModuleMap = new HashMap<>();
                    if (!packageDependenciesNode.isMissingNode()) {
                        packageDependenciesNode.propertyStream().forEach(dependency -> {
                            final UnresolvedModule unresolvedModule = new UnresolvedModule(dependency.getKey(), null, dependency.getValue().toString());
                            unresolvedModuleMap.put(dependency.getKey(), unresolvedModule);
                        });
                    }
                    resolvedModule.setRuntimeDependencies(unresolvedModuleMap);

                    resolvedModules.add(resolvedModule);
                });

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

                cpd.set(ComponentPatternData.Attribute.INCLUDE_PATTERN, "pyproject.toml, poetry.lock");

                cpd.setExpansionInventorySupplier(() -> inventory);

                list.add(cpd);

                return list;
            }
        } catch (IOException e) {
            log.warn("Failure processing composer.lock file: [{}]", e.getMessage());
        }

        return Collections.emptyList();

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

    private List<UnresolvedModule> extractDirectDependencies(JsonNode pyProjectRootNode, String fullQualifiedPath) {
        final List<UnresolvedModule> modules = new ArrayList<>();
        JsonNode dependencyNode = pyProjectRootNode.at(fullQualifiedPath);
        if (!dependencyNode.isMissingNode()) {
            dependencyNode.propertyStream().forEach(entry -> {
                String versionRange = deriveVersionRange(entry.getValue());
                UnresolvedModule unresolvedModule = new UnresolvedModule(entry.getKey(), null, versionRange);
                modules.add(unresolvedModule);
            });
        }
        return modules;
    }

    private String deriveVersionRange(JsonNode value) {
        return value.isTextual() ? value.textValue() : value.get("version").textValue();
    }

    private PyProjectPackageSource parseSource(JsonNode packageNode) {
        final JsonNode source = packageNode.path("source");
        if (source.isMissingNode()) {
            return new PyProjectPackageSource(null, null, "PyPI");
        }
        final String type = source.path("type").asText(null);
        final String url = source.path("url").asText(null);
        final String reference = source.path("reference").asText(null);
        String finalUrlCollection = buildPackageSourceUrls(packageNode, url);

        return new PyProjectPackageSource(type, finalUrlCollection, reference);
    }

    private String buildPackageSourceUrls(JsonNode packageNode, String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        final JsonNode files = packageNode.path("files");
        if (files.isMissingNode() || !files.isArray()) {
            return url;
        }

        final List<String> urls = new ArrayList<>();
        for (JsonNode file : files) {
            if (file.isObject()) {
                String finalUrl = url;
                finalUrl += "/" + file.get("file").asText();
                String hash = file.get("hash").asText(null);
                List<String> hashComponents = hash != null ? Arrays.stream(hash.split(":")).toList() : List.of();
                if (!hashComponents.isEmpty()) {
                    finalUrl += "#" + hashComponents.get(0) + "=" + hashComponents.get(1);
                }
                List<String> splitUrlOnPoint = Arrays.stream(finalUrl.split("\\.", 2)).toList();
                finalUrl = splitUrlOnPoint.get(0) + "-r2." + splitUrlOnPoint.get(1);
                urls.add(finalUrl);
            }
        }
        return urls.isEmpty() ? url : String.join(", ", urls);
    }

    private String buildPurl(String name, String version) {
        // first we have to handle that the name should not be case sensitive and underscore should be replaced by dash
        // see: https://github.com/package-url/purl-spec/blob/master/PURL-TYPES.rst#pypi
        name = name.toLowerCase(Locale.ENGLISH).replace("_", "-");
        return "pkg:pypi/" + name + "@" + version;
    }

}
