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
package org.metaeffekt.core.inventory.processor.filescan;

public class FileSystemScanConstants {

    // TEMP processing attributes use capital letters and underscores

    // FIXME: rename; asset path chain
    public static final String ATTRIBUTE_KEY_ASSET_ID_CHAIN = "ASSET_ID_CHAIN";

    public static final String ATTRIBUTE_KEY_SCAN_DIRECTIVE = "SCAN_DIRECTIVE";

    public static final String ATTRIBUTE_KEY_UNWRAP = "UNWRAP";

    public static final String ATTRIBUTE_KEY_INSPECTED = "INSPECTED";

    public static final String ATTRIBUTE_KEY_ARTIFACT_PATH = "ARTIFACT PATH";

    public static final String ATTRIBUTE_KEY_INSPECTION_SOURCE = "Inspection Source";

    public static final String HINT_SCAN = "scan";

    public static final String HINT_ATOMIC = "atomic";

    public static final String HINT_COMPLEX = "complex";

    public static final String HINT_IGNORE = "ignore";

    public static final String HINT_INCLUDE = "include";

    public static final String HINT_EXCLUDE = "exclude";

    public static final String SCAN_DIRECTIVE_DELETE = "delete";

    public static final String ATTRIBUTE_KEY_COMPONENT_PATTERN_MARKER = "COMPONENT PATTERN MARKER";

    protected FileSystemScanConstants() {}

}
