package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.poetry;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;

import java.util.List;

/**
 * Parser for Poetry lock format 1.x and 2.x (same format convention).
 */
public class PoetryLockParser extends AbstractPoetryLockParser {
    @Override
    public boolean supports(JsonNode root) {
        String version = root.path("metadata").path("lock-version").asText();
        return version.startsWith("1.") || version.startsWith("2.");
    }

    @Override
    public List<ResolvedModule> parse(JsonNode root) {
        return extractPackages(root);
    }
}
