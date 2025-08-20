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
package org.metaeffekt.core.inventory.processor.adapter;

import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.adapter.npm.NpmLockParserFactory;
import org.metaeffekt.core.inventory.processor.adapter.npm.PackageLockParser;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;

import java.io.File;
import java.io.IOException;

/**
 * Extracts an inventory for production npm modules based on a package-lock.json file.
 */
@Slf4j
public class NpmPackageLockAdapter extends AbstractLockAdapter {

    public static final String TYPE_NPM_MODULE = "npm-module";

    protected String getComponentSourceType() {
        return TYPE_NPM_MODULE;
    }

    protected String buildPurl(ResolvedModule module) {
        return String.format("pkg:npm/%s@%s", module.getName(), module.getVersion());
    }

    /**
     * @param packageLockJsonFile The package-lock.json file to parse.
     * @param webModule The webModule for which to extract data.
     *
     * @return An inventory populated with the runtime modules defined in the package json file.
     *
     * @throws IOException May throw {@link IOException} when accessing and parsing the packageLockJsonFile.
     */
    public Inventory createInventoryFromPackageLock(File packageLockJsonFile, WebModule webModule) throws IOException {

        final PackageLockParser packageLockParser = NpmLockParserFactory.createPackageLockParser(packageLockJsonFile);

        // parse dependency tree from lock; may use the webModule name for filtering (project in workspace)
        packageLockParser.parseModules(webModule);

        // populate data into inventory
        return createInventory(webModule, packageLockParser);
    }

}
