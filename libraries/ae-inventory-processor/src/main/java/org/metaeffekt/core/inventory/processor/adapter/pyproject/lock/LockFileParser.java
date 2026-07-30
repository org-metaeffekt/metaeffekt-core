package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.ResolvedModule;

import java.util.List;

/**
 * Interface defining required methods for parsing py project lock files.
 */
public interface LockFileParser {
    /**
     * Checks whether this parser supports the lock file format.
     */
    boolean supports(JsonNode lockRoot);

    /**
     * Parses resolved modules.
     */
    List<ResolvedModule> parse(JsonNode lockRoot);
}
