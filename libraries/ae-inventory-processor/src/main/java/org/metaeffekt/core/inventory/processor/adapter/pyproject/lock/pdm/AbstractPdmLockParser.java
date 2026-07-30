package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.pdm;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.AbstractLockFileParser;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.SharedMethodProvider.addIfNotNull;
import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.SharedMethodProvider.parseRequirement;


public abstract class AbstractPdmLockParser extends AbstractLockFileParser {
    @Override
    protected Map<String, UnresolvedModule> parseDependencies(JsonNode dependencies) {
        Map<String, UnresolvedModule> unresolvedModuleMap = new HashMap<>();
        if (!dependencies.isArray()) {
            return unresolvedModuleMap;
        }
        dependencies.valueStream().forEach(dependency -> {
                    UnresolvedModule module = parseRequirement(dependency.asText());
                    unresolvedModuleMap.put(module.getName(), module);
                }
        );

        return unresolvedModuleMap;
    }

    @Override
    protected PyProjectPackageSource parseSource(JsonNode packageNode) {
        List<String> urls = new ArrayList<>();
        packageNode.path("files").forEach(file -> {
                    String url = file.path("url").asText(null);
                    addIfNotNull(urls, url);
                }
        );

        return urls.isEmpty() ? null : new PyProjectPackageSource(null, urls, null);
    }
}
