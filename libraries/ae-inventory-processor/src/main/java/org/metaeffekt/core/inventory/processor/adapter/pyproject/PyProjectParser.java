package org.metaeffekt.core.inventory.processor.adapter.pyproject;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.UnresolvedModule;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface PyProjectParser {
    /**
     * Checks whether this specific parser supports toml files.
     */
    boolean supports(JsonNode root);

    /**
     * Parses the toml and lock files extracting the dependencies.
     *
     * @param pyProjectToml the toml file
     * @param root          the root JSON node from the toml file
     * @return a {@link PyProjectData} object containing the parsed data
     * @throws IOException if an I/O error occurs
     */
    PyProjectData parse(File pyProjectToml, JsonNode root) throws IOException;

    /**
     * Extracts the direct dependencies in a toml file
     *
     * @param projectNode       the root project node
     * @param fullQualifiedPath the path to a node in the root project node
     * @return list of unresolved dependencies (modules)
     */
    List<UnresolvedModule> extractDirectDependencies(JsonNode projectNode, String fullQualifiedPath);

    String getIncludePattern();
}
