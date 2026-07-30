package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.pdm.PdmLockV4_5Parser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.poetry.PoetryLockParser;

import java.util.List;

public class LockFileParserFactory {
    private static final List<AbstractLockFileParser> parsers = List.of(new PoetryLockParser(), new PdmLockV4_5Parser());

    public LockFileParser getParser(JsonNode lockRoot) {
        return parsers.stream()
                .filter(parser ->
                        parser.supports(lockRoot)
                )
                .findFirst()
                .orElse(null);
    }
}
