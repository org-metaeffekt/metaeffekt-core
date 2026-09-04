/*
 * Copyright 2009-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.inventory.processor.adapter.pyproject.lock;

import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.pdm.PdmLockV4_5Parser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.lock.poetry.PoetryLockParser;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Factory class used to determine a parser for a specific lock file version.
 */
public class LockFileParserFactory {
    private static final List<AbstractLockFileParser> PARSERS = List.of(new PoetryLockParser(), new PdmLockV4_5Parser());

    /**
     * Determines the lock file parser for the specific lock file.
     *
     * @param lockRoot the lock file root node
     * @return the corresponding lock file parser
     */
    public AbstractLockFileParser getParser(JsonNode lockRoot) {
        return PARSERS.stream()
                .filter(parser -> parser.supports(lockRoot))
                .findFirst()
                .orElse(null);
    }
}
