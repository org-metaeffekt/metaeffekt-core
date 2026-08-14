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
package org.metaeffekt.core.inventory.processor.adapter.pyproject.toml;

import com.fasterxml.jackson.databind.JsonNode;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.pdm.PdmTomlParser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.poetry.PoetryLegacyV1Parser;
import org.metaeffekt.core.inventory.processor.adapter.pyproject.toml.poetry.PoetryPep621Parser;

import java.util.List;

/**
 * Factory class used to determine a parser for a specific toml file version.
 */
public class TomlParserFactory {
    private static final List<AbstractTomlParser> PARSERS = List.of(new PoetryLegacyV1Parser(), new PoetryPep621Parser(), new PdmTomlParser());

    /**
     * Determines the lock file parser for the specific toml file.
     * @param root the toml file root node
     * @return the corresponding lock file parser
     */
    public AbstractTomlParser getParser(JsonNode root) {
        return PARSERS.stream()
                .filter(parser -> parser.supports(root))
                .findFirst()
                .orElse(null);
    }
}
