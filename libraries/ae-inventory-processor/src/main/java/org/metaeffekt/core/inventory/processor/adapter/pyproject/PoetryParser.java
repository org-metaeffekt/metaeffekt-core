package org.metaeffekt.core.inventory.processor.adapter.pyproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for parsing poetry toml and poetry.lock files.
 */
public class PoetryParser implements PyProjectParser {
    @Override
    public boolean supports(JsonNode root) {
        return root.at("/tool/poetry").isObject();
    }

    @Override
    public PyProjectData parse(File pyProjectToml, JsonNode root) throws IOException {
        PyProjectData pyProjectData = new PyProjectData();

        // parse /tool/poetry
        JsonNode projectNode = root.at("/tool/poetry");
        ResolvedModule projectModule = new ResolvedModule(projectNode.get("name").asText(), null);
        projectModule.setVersion(projectNode.get("version").asText());

        pyProjectData.setProjectModule(projectModule);
        pyProjectData.setDirectRuntimeDependencies(extractDirectDependencies(projectNode, "/dependencies"));
        pyProjectData.setDirectDevelopmentDependencies(extractDirectDependencies(projectNode, "/group/dev/dependencies"));


        // parse poetry.lock
        final File poetryLockFile = new File(pyProjectToml.getParentFile(), "poetry.lock");
        final ObjectMapper lockObjectMapper = new TomlMapper();
        final JsonNode lockNode = lockObjectMapper.readTree(poetryLockFile);

        pyProjectData.setResolvedModulesFromLockFile(getResolvedModulesFromLockFile(lockNode));
        return pyProjectData;
    }

    @Override
    public List<UnresolvedModule> extractDirectDependencies(JsonNode projectNode, String fullQualifiedPath) {
        final List<UnresolvedModule> modules = new ArrayList<>();
        JsonNode dependencyNode = projectNode.at(fullQualifiedPath);
        if (!dependencyNode.isMissingNode()) {
            dependencyNode.propertyStream().forEach(entry -> {
                String versionRange = deriveVersionRange(entry.getValue());
                UnresolvedModule unresolvedModule = new UnresolvedModule(entry.getKey(), null, versionRange);
                modules.add(unresolvedModule);
            });
        }
        return modules;
    }

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, poetry.lock";
    }

    private List<ResolvedModule> getResolvedModulesFromLockFile(JsonNode lockNode) {
        final List<ResolvedModule> resolvedModules = new ArrayList<>();
        lockNode.path("package").valueStream().forEach(packageNode -> {
            final ResolvedModule resolvedModule = new ResolvedModule(packageNode.get("name").textValue(), null);
            resolvedModule.setVersion(packageNode.get("version").textValue());

            final PyProjectPackageSource source = getSourceIfExists(packageNode.path("source"));
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
        return resolvedModules;
    }

    private String deriveVersionRange(JsonNode value) {
        return value.isTextual() ? value.textValue() : value.get("version").textValue();
    }

    private PyProjectPackageSource getSourceIfExists(JsonNode source) {
        if (!source.isMissingNode()) {
            JsonNode typeNode = source.path("type");
            JsonNode urlNode = source.path("url");
            JsonNode referenceNode = source.path("reference");

            String type = !typeNode.isMissingNode() ? typeNode.asText() : null;
            String url = !urlNode.isMissingNode() ? urlNode.asText() : null;
            String reference = !referenceNode.isMissingNode() ? referenceNode.asText() : null;

            return new PyProjectPackageSource(type, url, reference);
        }
        return new PyProjectPackageSource(null, null, "PyPI");
    }
}
