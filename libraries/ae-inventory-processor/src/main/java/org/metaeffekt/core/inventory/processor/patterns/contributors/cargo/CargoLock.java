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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Basic binding of a CargoLock file format.
 */
public class CargoLock extends AbstractMapAccess<String, Object> {
    public static class Package extends AbstractMapAccess<String, Object> {
        // compare https://docs.rs/cargo-lock/latest/cargo_lock/package/struct.Package.html
        public Package(Map<String, Object> map) {
            super(map);
        }

        public String getName() {
            return stringOf("name");
        }

        public String getVersion() {
            return stringOf("version");
        }

        public String getChecksum() {
            return stringOf("checksum");
        }

        public String getSource() {
            return stringOf("source");
        }

        public List<String> getDependencies() {
            return listOf("dependencies");
        }
    }

    // compare https://docs.rs/cargo-lock/latest/cargo_lock/struct.Lockfile.html
    public CargoLock(final File cargoLockFile) throws IOException {
        super(new TomlMapper().readerFor(Map.class).readValue(cargoLockFile));
    }

    public String getVersion() {
        return stringOf("version");
    }

    public List<Package> getPackages() {
        final List<Map<String, Object>> aPackage = listOf("package");
        return aPackage.stream().map(Package::new).collect(Collectors.toList());
    }

}
