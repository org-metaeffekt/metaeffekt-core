package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.poetry;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.AbstractLockFileParser;
import org.metaeffekt.core.inventory.processor.model.PyProjectPackageSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.SharedMethodProvider.addIfNotNull;
import static org.metaeffekt.core.inventory.processor.adapter.pyproject.shared.SharedMethodProvider.extractVersion;

/**
 * Abstract class for parsing poetry lock file versions 1.x and 2.x.
 */
public abstract class AbstractPoetryLockParser extends AbstractLockFileParser {

    @Override
    protected Map<String, UnresolvedModule> parseDependencies(JsonNode dependencies) {
        Map<String, UnresolvedModule> unresolvedModuleMap = new HashMap<>();
        if (!dependencies.isObject()) {
            return unresolvedModuleMap;
        }
        dependencies.propertyStream().forEach(dependency -> unresolvedModuleMap.put(dependency.getKey(),
                new UnresolvedModule(dependency.getKey(), null, extractVersion(dependency.getValue())))
        );

        return unresolvedModuleMap;
    }


    @Override
    protected PyProjectPackageSource parseSource(JsonNode packageNode) {
        JsonNode source = packageNode.path("source");
        if (source.isMissingNode()) {
            return null;
        }
        final String type = source.path("type").asText(null);
        final List<String> urls = new ArrayList<>();
        addIfNotNull(urls, source.path("url").asText(null));
        final String reference = source.path("reference").asText(null);

        return new PyProjectPackageSource(type, urls, reference);
    }

}
