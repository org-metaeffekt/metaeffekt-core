package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class AbstractLockFileParser implements LockFileParser {

    /**
     * Extracts all packages from poetry.lock.
     */
    protected List<ResolvedModule> extractPackages(JsonNode lockRoot) {
        List<ResolvedModule> resolvedModules = new ArrayList<>();
        JsonNode packages = lockRoot.path("package");
        if (!packages.isArray()) {
            return resolvedModules;
        }
        packages.forEach(packageNode -> {
                    ResolvedModule module = parsePackage(packageNode);
                    resolvedModules.add(module);
                }
        );
        return resolvedModules;
    }

    /**
     * Creates resolved module from poetry package.
     */
    protected ResolvedModule parsePackage(JsonNode packageNode) {
        ResolvedModule module = new ResolvedModule(packageNode.path("name").asText(), null);
        module.setVersion(packageNode.path("version").asText(null));
        Map<String, UnresolvedModule> unresolvedModuleMap = parseDependencies(packageNode.path("dependencies"));
        module.setRuntimeDependencies(unresolvedModuleMap);

        module.setPyProjectPackageSource(parseSource(packageNode));
        module.setPyProjectPackageFiles(collectFiles(packageNode));

        return module;
    }

    /**
     * Collects the file elements from the files attribute from the package in the lock file.
     *
     * @param packageNode the package node
     * @return JSON array containing the files and the hashes
     */
    protected JSONArray collectFiles(JsonNode packageNode) {
        JsonNode files = packageNode.path("files");
        if (!files.isArray()) {
            return null;
        }
        JSONArray fileData = new JSONArray();
        files.forEach(file -> {
                    JSONObject object = new JSONObject();
                    object.put("file", file.path("file").asText(null));
                    object.put("hash", file.path("hash").asText(null));
                    fileData.put(object);
                }
        );

        return fileData;
    }

    /**
     * Parses package.dependencies.
     */
    protected abstract Map<String, UnresolvedModule> parseDependencies(JsonNode dependencies);


    /**
     * Parses information of the package source attribute of the lock file.
     *
     * @param packageNode the package node
     * @return the extracted source data
     */
    protected abstract PyProjectPackageSource parseSource(JsonNode packageNode);

}
