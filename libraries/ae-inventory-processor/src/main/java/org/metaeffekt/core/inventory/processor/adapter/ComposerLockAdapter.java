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
package org.metaeffekt.core.inventory.processor.adapter;

import org.metaeffekt.core.inventory.processor.adapter.npm.ComposerLockParser;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.patterns.contributors.web.WebModule;

import java.io.File;
import java.io.IOException;

/**
 * Extracts an inventory for PHP Composer modules based on a composer.lock file.
 */
public class ComposerLockAdapter extends AbstractLockAdapter {

    public static final String TYPE_PHP_MODULE = "php-module";

    /**
     * Extracts an inventory from a composer.lock file.
     *
     * @param composerLock The composer.lock file to parse
     * @param webModule The webModule for which to extract data.
     *
     * @return An inventory populated with the modules defined in the composer.lock file
     *
     * @throws IOException May be thrown on file access.
     */
    public Inventory extractInventory(File composerLock, WebModule webModule) throws IOException {
        ComposerLockParser composerLockAdapter = new ComposerLockParser(composerLock);

        composerLockAdapter.parseModules(webModule);

        // populate data into inventory
        return createInventory(webModule, composerLockAdapter);
    }

    @Override
    protected String buildPurl(ResolvedModule module) {
        final String name = module.getName();
        final String version = module.getVersion();
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (version == null || version.isEmpty()) {
            return String.format("pkg:composer/%s", name);
        }
        return String.format("pkg:composer/%s@%s", name, version);
    }

    protected String getComponentSourceType() {
        return TYPE_PHP_MODULE;
    }

}