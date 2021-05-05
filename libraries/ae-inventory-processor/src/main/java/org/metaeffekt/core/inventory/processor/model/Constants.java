/**
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.model;

public final class Constants {

    public static final String ASTERISK = "*";
    public static final String VERSION_PLACHOLDER_PREFIX = "${";
    public static final String VERSION_PLACHOLDER_SUFFIX = "}";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String DOT_SLASH = DOT + SLASH;

    public static final String STRING_EMPTY = "";
    public static final String STRING_TRUE = Boolean.TRUE.toString();
    public static final String STRING_FALSE = Boolean.FALSE.toString();

    public static final char DELIMITER_COLON = ':';
    public static final char DELIMITER_DASH = '-';
    public static final char DELIMITER_PIPE = '|';
    public static final char DELIMITER_COMMA = ',';
    public static final String DELIMITER_NEWLINE = String.format("%n");

    /**
     * Support to mark artifacts as matched by wildcard. This is usually transient information. The wildcard information
     * is lost, when resolving the version. Therefore the fact that an artifact was matched using wildcards is held
     * using this key.
     */
    public static final String KEY_WILDCARD_MATCH = "WILDCARD-MATCH";

    public static final String KEY_DERIVED_LICENSE_PACKAGE = "Specified Package License";
    public static final String KEY_DOCUMENTATION_PATH_PACKAGE = "Package Documentation Path";
    public static final String KEY_LICENSE_PATH_PACKAGE = "Package License Path";
    public static final String KEY_GROUP_PACKAGE = "Package Group";

    public static final String KEY_SUMMARY = "Summary";
    public static final String KEY_DESCRIPTION = "Description";
    public static final String KEY_ARCHITECTURE = "Architecture";
    public static final String KEY_TYPE = "Type";
    public static final String KEY_SOURCE_PROJECT = "Source Project";

    public static final String KEY_CONTAINER = "Container";
    public static final String KEY_ISSUE = "Issue";

    public static final String ARTIFACT_TYPE_PACKAGE = "package";
    public static final String ARTIFACT_TYPE_FILE = "file";
    public static final String ARTIFACT_TYPE_NODEJS_MODULE = "nodejs-module";

    protected Constants() {};

}
