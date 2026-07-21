package org.metaeffekt.core.inventory.processor.adapter.pyproject;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class for parsing pdm toml and pdm.lock files.
 */
public class PdmParser implements PyProjectParser {

    @Override
    public boolean supports(JsonNode root) {
        return root.at("/project").isObject();
    }

    @Override
    public PyProjectData parse(File pyProjectToml, JsonNode root) throws IOException {
        return null;
    }

    @Override
    public List<UnresolvedModule> extractDirectDependencies(JsonNode projectNode, String fullQualifiedPath) {
        return List.of();
    }

    @Override
    public String getIncludePattern() {
        return "pyproject.toml, pdm.lock";
    }
}
