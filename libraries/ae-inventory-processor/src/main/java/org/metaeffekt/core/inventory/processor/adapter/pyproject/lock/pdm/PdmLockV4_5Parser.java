package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.pdm;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;

import java.util.List;

/**
 * Parser for Pdm lock format 4.x.
 */
public class PdmLockV4_5Parser extends AbstractPdmLockParser {

    @Override
    public boolean supports(JsonNode root) {
        return root.path("metadata").path("lock_version").asText().startsWith("4.");
    }

    @Override
    public List<ResolvedModule> parse(JsonNode root) {
        return extractPackages(root);
    }
}
