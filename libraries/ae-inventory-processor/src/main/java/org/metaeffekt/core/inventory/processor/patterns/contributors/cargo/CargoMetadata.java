/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.patterns.contributors.cargo;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Basic binding for CargoMetadata files format.
 */
public class CargoMetadata extends AbstractMapAccess<String, Object> {

    public static class Package extends AbstractMapAccess<String, Object> {
        public Package(Map<String, Object> map) {
            super(map);
        }
    }

    public CargoMetadata(final File cargoTomlFile) throws IOException {
        super(new TomlMapper().readerFor(Map.class).readValue(cargoTomlFile));
    }

    public Package getPackage() {
        return new Package(mapOf("package"));
    }

}
