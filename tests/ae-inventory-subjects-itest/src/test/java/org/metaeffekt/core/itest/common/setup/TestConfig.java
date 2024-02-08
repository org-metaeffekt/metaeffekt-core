/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.itest.common.setup;

public class TestConfig {

    private static final String DOWNLOAD_FOLDER = ".test/downloads/";
    private static final String SCAN_FOLDER = "target/.test/scan/";
    private static final String INVENTORY_FOLDER = "target/.test/inventory/";

    public static String getDownloadFolder() {
        return DOWNLOAD_FOLDER;}

    public static String getScanFolder() {
        return SCAN_FOLDER;
    }

    public static String getInventoryFolder() {
        return INVENTORY_FOLDER;
    }
}
